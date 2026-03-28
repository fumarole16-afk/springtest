package com.stockanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalyzer.dto.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class FinnhubClient {
    private static final Logger log = LoggerFactory.getLogger(FinnhubClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${finnhub.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<NewsItem> fetchCompanyNews(String ticker, String fromDate, String toDate) {
        String url = "https://finnhub.io/api/v1/company-news?symbol=" + ticker +
                "&from=" + fromDate + "&to=" + toDate + "&token=" + apiKey;
        try {
            String json = restTemplate.getForObject(url, String.class);
            return parseNewsArray(json);
        } catch (Exception e) {
            log.error("Failed to fetch Finnhub news for {}: {}", ticker, e.getMessage());
            return new ArrayList<>();
        }
    }

    static List<NewsItem> parseNewsArray(String json) {
        List<NewsItem> items = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            if (!root.isArray()) return items;
            for (JsonNode node : root) {
                NewsItem item = new NewsItem();
                item.setTitle(node.path("headline").asText(null));
                item.setSource(node.path("source").asText(null));
                item.setUrl(node.path("url").asText(null));
                long datetime = node.path("datetime").asLong(0);
                if (datetime > 0) {
                    item.setPublishedAt(java.time.Instant.ofEpochSecond(datetime)
                            .toString());
                }
                item.setSummary(node.path("summary").asText(null));
                item.setImageUrl(node.path("image").asText(null));
                items.add(item);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Finnhub news response", e);
        }
        return items;
    }
}
