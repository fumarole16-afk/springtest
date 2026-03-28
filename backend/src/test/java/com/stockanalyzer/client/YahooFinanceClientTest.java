package com.stockanalyzer.client;

import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

public class YahooFinanceClientTest {

    @Test
    public void parseQuoteResponse_extractsStockInfo() {
        String json = "{\"quoteResponse\":{\"result\":[{" +
                "\"symbol\":\"AAPL\"," +
                "\"shortName\":\"Apple Inc.\"," +
                "\"fullExchangeName\":\"NASDAQ\"," +
                "\"marketCap\":3000000000000," +
                "\"trailingPE\":28.5," +
                "\"regularMarketPrice\":173.50" +
                "}]}}";

        StockDetail detail = YahooFinanceClient.parseQuoteResponse(json);
        assertEquals("AAPL", detail.getTicker());
        assertEquals("Apple Inc.", detail.getCompanyName());
        assertEquals(new BigDecimal("173.50"), detail.getCurrentPrice());
    }

    @Test
    public void parseChartResponse_extractsPriceHistory() {
        String json = "{\"chart\":{\"result\":[{" +
                "\"timestamp\":[1711929600,1712016000]," +
                "\"indicators\":{\"quote\":[{" +
                "\"open\":[170.0,171.5]," +
                "\"high\":[175.0,176.0]," +
                "\"low\":[168.0,169.5]," +
                "\"close\":[173.0,174.5]," +
                "\"volume\":[50000000,48000000]" +
                "}],\"adjclose\":[{\"adjclose\":[173.0,174.5]}]}}]}}";

        List<PriceData> prices = YahooFinanceClient.parseChartResponse(json);
        assertEquals(2, prices.size());
        assertEquals(new BigDecimal("170.0"), prices.get(0).getOpen());
        assertEquals(new BigDecimal("174.5"), prices.get(1).getClose());
        assertEquals(50000000L, prices.get(0).getVolume());
    }
}
