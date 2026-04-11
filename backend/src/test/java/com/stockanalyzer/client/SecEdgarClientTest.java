package com.stockanalyzer.client;

import org.junit.Test;

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
}
