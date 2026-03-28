package com.stockanalyzer.client;

import com.stockanalyzer.dto.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class SecEdgarClient {
    private static final Logger log = LoggerFactory.getLogger(SecEdgarClient.class);
    private static final String EDGAR_URL =
            "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&type=&dateb=&owner=include&count={count}&search_text=&CIK={ticker}&output=atom";

    private final RestTemplate restTemplate = new RestTemplate();

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
}
