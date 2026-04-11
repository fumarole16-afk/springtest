package com.stockanalyzer.client;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

public class SecEdgarClientTest {

    @Test
    public void resolveCik_returnsCorrectCik() {
        String mockJson = "{\"0\":{\"cik_str\":320193,\"ticker\":\"AAPL\",\"title\":\"Apple Inc.\"}," +
                           "\"1\":{\"cik_str\":789019,\"ticker\":\"MSFT\",\"title\":\"MICROSOFT CORP\"}}";
        SecEdgarClient client = new SecEdgarClient();
        client.loadTickerMap(mockJson);
        assertEquals("0000320193", client.resolveCik("AAPL"));
        assertEquals("0000789019", client.resolveCik("MSFT"));
        assertNull(client.resolveCik("UNKNOWN"));
    }

    @Test
    public void parseFinancialFacts_extractsAnnualData() {
        String json = "{ \"cik\": 320193, \"entityName\": \"Apple Inc.\", \"facts\": { \"us-gaap\": {" +
            "\"RevenueFromContractWithCustomerExcludingAssessedTax\": { \"units\": { \"USD\": [" +
            "  {\"val\": 394328000000, \"fy\": 2024, \"form\": \"10-K\", \"end\": \"2024-09-28\"}," +
            "  {\"val\": 416161000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"NetIncomeLoss\": { \"units\": { \"USD\": [" +
            "  {\"val\": 93736000000, \"fy\": 2024, \"form\": \"10-K\", \"end\": \"2024-09-28\"}," +
            "  {\"val\": 112010000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"OperatingIncomeLoss\": { \"units\": { \"USD\": [" +
            "  {\"val\": 133050000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"Assets\": { \"units\": { \"USD\": [" +
            "  {\"val\": 359241000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"Liabilities\": { \"units\": { \"USD\": [" +
            "  {\"val\": 285508000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"StockholdersEquity\": { \"units\": { \"USD\": [" +
            "  {\"val\": 73733000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}," +
            "\"NetCashProvidedByUsedInOperatingActivities\": { \"units\": { \"USD\": [" +
            "  {\"val\": 111482000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
            "]}}" +
            "}}}";

        List<SecEdgarClient.EdgarFinancial> results = SecEdgarClient.parseFinancialFacts(json);
        assertFalse(results.isEmpty());

        SecEdgarClient.EdgarFinancial latest = results.get(0);
        assertEquals(2025, latest.fiscalYear);
        assertEquals(new BigDecimal("416161000000"), latest.revenue);
        assertEquals(new BigDecimal("112010000000"), latest.netIncome);
        assertEquals(new BigDecimal("133050000000"), latest.operatingIncome);
        assertEquals(new BigDecimal("359241000000"), latest.totalAssets);
        assertEquals(new BigDecimal("285508000000"), latest.totalLiabilities);
        assertEquals(new BigDecimal("73733000000"), latest.totalEquity);
        assertEquals(new BigDecimal("111482000000"), latest.operatingCashFlow);
    }
}
