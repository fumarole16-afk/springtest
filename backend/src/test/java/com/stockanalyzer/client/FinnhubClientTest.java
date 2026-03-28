package com.stockanalyzer.client;

import com.stockanalyzer.dto.NewsItem;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FinnhubClientTest {

    @Test
    public void parseNewsArray_extractsFields() {
        String json = "[{" +
                "\"headline\":\"Apple Beats Earnings\"," +
                "\"source\":\"Reuters\"," +
                "\"url\":\"https://reuters.com/apple-earnings\"," +
                "\"datetime\":1711929600," +
                "\"summary\":\"Apple reported strong Q1 results.\"," +
                "\"image\":\"https://example.com/img.jpg\"" +
                "}]";

        List<NewsItem> items = FinnhubClient.parseNewsArray(json);
        assertEquals(1, items.size());
        NewsItem item = items.get(0);
        assertEquals("Apple Beats Earnings", item.getTitle());
        assertEquals("Reuters", item.getSource());
        assertEquals("https://reuters.com/apple-earnings", item.getUrl());
        assertNotNull(item.getPublishedAt());
        assertEquals("Apple reported strong Q1 results.", item.getSummary());
        assertEquals("https://example.com/img.jpg", item.getImageUrl());
    }

    @Test
    public void parseNewsArray_multipleItems_returnsAll() {
        String json = "[" +
                "{\"headline\":\"News 1\",\"source\":\"S1\",\"url\":\"http://u1\",\"datetime\":1711929600}," +
                "{\"headline\":\"News 2\",\"source\":\"S2\",\"url\":\"http://u2\",\"datetime\":1711929700}" +
                "]";
        List<NewsItem> items = FinnhubClient.parseNewsArray(json);
        assertEquals(2, items.size());
        assertEquals("News 1", items.get(0).getTitle());
        assertEquals("News 2", items.get(1).getTitle());
    }

    @Test
    public void parseNewsArray_emptyArray_returnsEmpty() {
        List<NewsItem> items = FinnhubClient.parseNewsArray("[]");
        assertTrue(items.isEmpty());
    }
}
