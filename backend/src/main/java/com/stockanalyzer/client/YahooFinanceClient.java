package com.stockanalyzer.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockanalyzer.dto.IndexQuote;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${yahoo.finance.base-url:https://query1.finance.yahoo.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String fetchWithHeaders(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "*/*");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
    }

    public StockDetail fetchQuote(String ticker) {
        // Use v8 chart API which doesn't require authentication
        String url = baseUrl + "/v8/finance/chart/" + ticker + "?range=1d&interval=1d";
        String json = fetchWithHeaders(url);
        return parseQuoteFromChart(json);
    }

    public List<StockDetail> searchTickers(String query) {
        String url = "https://query1.finance.yahoo.com/v1/finance/search?q=" + query + "&quotesCount=10&newsCount=0";
        try {
            String json = fetchWithHeaders(url);
            JsonNode root = readTreeExact(json);
            JsonNode quotes = root.path("quotes");
            List<StockDetail> results = new ArrayList<>();
            for (JsonNode q : quotes) {
                if (!"EQUITY".equals(q.path("quoteType").asText())) continue;
                StockDetail d = new StockDetail();
                d.setTicker(q.path("symbol").asText());
                d.setCompanyName(q.has("longname") ? q.path("longname").asText() : q.path("shortname").asText());
                d.setExchange(q.path("exchDisp").asText());
                results.add(d);
            }
            return results;
        } catch (Exception e) {
            log.warn("Yahoo search failed for '{}': {}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    public JsonNode fetchFinancials(String ticker) {
        String url = baseUrl + "/v10/finance/quoteSummary/" + ticker +
                "?modules=incomeStatementHistory,balanceSheetHistory,cashflowStatementHistory";
        String json = fetchWithHeaders(url);
        try {
            return readTreeExact(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch financials for " + ticker, e);
        }
    }

    public List<PriceData> fetchPriceHistory(String ticker, String range, String interval) {
        String url = baseUrl + "/v8/finance/chart/" + ticker +
                "?range=" + range + "&interval=" + interval;
        String json = fetchWithHeaders(url);
        return parseChartResponse(json);
    }

    static StockDetail parseQuoteFromChart(String json) {
        try {
            JsonNode root = readTreeExact(json);
            JsonNode meta = root.path("chart").path("result").get(0).path("meta");
            StockDetail detail = new StockDetail();
            detail.setTicker(meta.path("symbol").asText());
            detail.setCompanyName(meta.has("longName") ? meta.path("longName").asText() :
                    meta.has("shortName") ? meta.path("shortName").asText() : meta.path("symbol").asText());
            detail.setExchange(meta.path("fullExchangeName").asText());
            detail.setCurrentPrice(nodeToDecimal(meta.path("regularMarketPrice")));
            detail.setMarketCap(BigDecimal.ZERO);
            return detail;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse chart response for quote", e);
        }
    }

    public IndexQuote fetchIndexQuote(String symbol, String displayName) {
        try {
            String url = baseUrl + "/v8/finance/chart/" + symbol + "?range=2d&interval=1d";
            String json = fetchWithHeaders(url);
            return parseIndexQuoteFromChart(json, symbol, displayName);
        } catch (Exception e) {
            log.warn("Failed to fetch index quote for {}: {}", symbol, e.getMessage());
            return new IndexQuote(symbol, displayName, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    static IndexQuote parseIndexQuoteFromChart(String json, String symbol, String displayName) {
        try {
            JsonNode root = readTreeExact(json);
            JsonNode meta = root.path("chart").path("result").get(0).path("meta");
            BigDecimal price = nodeToDecimal(meta.path("regularMarketPrice"));
            BigDecimal prevClose = nodeToDecimal(meta.path("chartPreviousClose"));
            BigDecimal change = price.subtract(prevClose);
            BigDecimal changePct = prevClose.compareTo(BigDecimal.ZERO) != 0
                    ? change.multiply(new BigDecimal("100")).divide(prevClose, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
            return new IndexQuote(symbol, displayName, price, change, changePct);
        } catch (Exception e) {
            log.warn("Failed to parse index chart for {}: {}", symbol, e.getMessage());
            return new IndexQuote(symbol, displayName, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    static StockDetail parseQuoteResponse(String json) {
        try {
            JsonNode root = readTreeExact(json);
            JsonNode result = root.path("quoteResponse").path("result").get(0);
            StockDetail detail = new StockDetail();
            detail.setTicker(result.path("symbol").asText());
            detail.setCompanyName(result.path("shortName").asText());
            detail.setExchange(result.path("fullExchangeName").asText());
            detail.setMarketCap(nodeToDecimal(result.path("marketCap")));
            detail.setCurrentPrice(nodeToDecimal(result.path("regularMarketPrice")));
            if (result.has("trailingPE") && !result.path("trailingPE").isNull()) {
                detail.setTrailingPE(nodeToDecimal(result.path("trailingPE")));
            }
            return detail;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Yahoo Finance quote response", e);
        }
    }

    static List<PriceData> parseChartResponse(String json) {
        try {
            JsonNode root = readTreeExact(json);
            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);
            JsonNode adjClose = result.path("indicators").path("adjclose").get(0).path("adjclose");

            List<PriceData> prices = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                long ts = timestamps.get(i).asLong();
                LocalDate date = Instant.ofEpochSecond(ts)
                        .atZone(ZoneId.of("America/New_York")).toLocalDate();
                PriceData pd = new PriceData();
                pd.setDate(date);
                pd.setOpen(nodeToDecimal(quote.path("open").get(i)));
                pd.setHigh(nodeToDecimal(quote.path("high").get(i)));
                pd.setLow(nodeToDecimal(quote.path("low").get(i)));
                pd.setClose(nodeToDecimal(quote.path("close").get(i)));
                pd.setAdjustedClose(nodeToDecimal(adjClose.get(i)));
                pd.setVolume(quote.path("volume").get(i).asLong());
                prices.add(pd);
            }
            return prices;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Yahoo Finance chart response", e);
        }
    }

    /**
     * Parses JSON preserving exact decimal representation (e.g. 173.50 stays as scale-2 BigDecimal).
     * Standard ObjectMapper normalizes floats, losing trailing zeros and using scientific notation.
     */
    private static JsonNode readTreeExact(String json) throws Exception {
        JsonParser p = mapper.createParser(json);
        p.nextToken();
        return buildNode(p);
    }

    private static JsonNode buildNode(JsonParser p) throws Exception {
        JsonNodeFactory f = JsonNodeFactory.instance;
        JsonToken token = p.currentToken();
        switch (token) {
            case START_OBJECT: {
                ObjectNode obj = f.objectNode();
                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String name = p.getCurrentName();
                    p.nextToken();
                    obj.set(name, buildNode(p));
                }
                return obj;
            }
            case START_ARRAY: {
                ArrayNode arr = f.arrayNode();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    arr.add(buildNode(p));
                }
                return arr;
            }
            case VALUE_NUMBER_FLOAT:
                return new DecimalNode(new BigDecimal(p.getText()));
            case VALUE_NUMBER_INT:
                return f.numberNode(p.getLongValue());
            case VALUE_STRING:
                return f.textNode(p.getText());
            case VALUE_TRUE:
                return f.booleanNode(true);
            case VALUE_FALSE:
                return f.booleanNode(false);
            default:
                return f.nullNode();
        }
    }

    private static BigDecimal nodeToDecimal(JsonNode node) {
        if (node == null || node.isNull()) return BigDecimal.ZERO;
        if (node.isIntegralNumber()) return new BigDecimal(node.bigIntegerValue());
        return node.decimalValue();
    }
}
