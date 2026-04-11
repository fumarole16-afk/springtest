package com.stockanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalyzer.dto.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecEdgarClient {
    private static final Logger log = LoggerFactory.getLogger(SecEdgarClient.class);
    private static final String EDGAR_URL =
            "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&type=&dateb=&owner=include&count={count}&search_text=&CIK={ticker}&output=atom";
    private static final String TICKER_MAP_URL = "https://www.sec.gov/files/company_tickers.json";

    @Value("${sec.edgar.user-agent:stockanalyzer/1.0 contact@example.com}")
    private String userAgent;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> tickerToCik = new ConcurrentHashMap<>();

    public List<NewsItem> fetchFilings(String ticker, int count) {
        String url = EDGAR_URL.replace("{ticker}", ticker).replace("{count}", String.valueOf(count));
        try {
            String xml = restTemplate.getForObject(url, String.class);
            return parseAtomFeed(xml, ticker);
        } catch (Exception e) {
            log.error("Failed to fetch SEC EDGAR filings for {}: {}", ticker, e.getMessage());
            return new ArrayList<>();
        }
    }

    static List<NewsItem> parseAtomFeed(String xml, String source) {
        List<NewsItem> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                NewsItem item = new NewsItem();
                item.setTitle(getTagText(entry, "title"));
                item.setSource(source != null ? "SEC EDGAR" : "SEC EDGAR");
                item.setUrl(getLinkHref(entry));
                item.setPublishedAt(getTagText(entry, "updated"));
                item.setSummary(getTagText(entry, "summary"));
                items.add(item);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SEC EDGAR Atom feed", e);
        }
        return items;
    }

    private static String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private static String getLinkHref(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        if (links.getLength() > 0) {
            Element link = (Element) links.item(0);
            String href = link.getAttribute("href");
            if (href != null && !href.isEmpty()) return href;
            return link.getTextContent();
        }
        return null;
    }

    public void loadTickerMap(String json) {
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            for (JsonNode entry : root) {
                String ticker = entry.path("ticker").asText().toUpperCase();
                int cik = entry.path("cik_str").asInt();
                tickerToCik.put(ticker, String.format("%010d", cik));
            }
            log.info("Loaded {} ticker-to-CIK mappings", tickerToCik.size());
        } catch (Exception e) {
            log.error("Failed to parse ticker map: {}", e.getMessage());
        }
    }

    public void initTickerMapIfEmpty() {
        if (!tickerToCik.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String json = restTemplate.exchange(TICKER_MAP_URL, HttpMethod.GET, entity, String.class).getBody();
            loadTickerMap(json);
        } catch (Exception e) {
            log.error("Failed to fetch ticker map from SEC: {}", e.getMessage());
        }
    }

    public String resolveCik(String ticker) {
        initTickerMapIfEmpty();
        return tickerToCik.get(ticker.toUpperCase());
    }
}
