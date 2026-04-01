package com.example.work_helper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);
    private static final Pattern LATIN_OR_NUMBER = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9+#._-]*");
    private static final Pattern CJK_CHAR = Pattern.compile("[\\p{IsHan}]");
    private static final int MAX_RESULTS = 5;

    @Bean
    public ContentRetriever contentRetriever() {
        try {
            List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
            DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 200);
            List<IndexedSegment> indexedSegments = new ArrayList<>();

            for (Document document : documents) {
                for (TextSegment segment : splitter.split(document)) {
                    TextSegment decoratedSegment = TextSegment.from(
                            segment.metadata().getString("file_name") + "\n" + segment.text(),
                            segment.metadata());
                    indexedSegments.add(new IndexedSegment(decoratedSegment, extractTokens(decoratedSegment.text())));
                }
            }

            log.info("Local RAG initialized with {} text segments", indexedSegments.size());

            return query -> retrieveRelevantContent(indexedSegments, query.text());
        } catch (Exception exception) {
            log.warn("RAG initialization failed, content retrieval disabled: {}", exception.getMessage());
            return query -> List.of();
        }
    }

    private List<Content> retrieveRelevantContent(List<IndexedSegment> indexedSegments, String queryText) {
        Set<String> queryTokens = extractTokens(queryText);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        return indexedSegments.stream()
                .map(segment -> new ScoredSegment(segment, score(queryTokens, segment)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredSegment::score).reversed())
                .limit(MAX_RESULTS)
                .map(scored -> (Content) new DefaultContent(scored.segment().textSegment()))
                .toList();
    }

    private double score(Set<String> queryTokens, IndexedSegment segment) {
        double score = 0;
        String text = segment.textSegment().text().toLowerCase(Locale.ROOT);

        for (String token : queryTokens) {
            if (segment.tokens().contains(token)) {
                score += token.length() > 1 ? 3 : 1;
            }
            if (text.contains(token)) {
                score += 0.5;
            }
        }

        return score;
    }

    private Set<String> extractTokens(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();

        Matcher latinMatcher = LATIN_OR_NUMBER.matcher(normalized);
        while (latinMatcher.find()) {
            String token = latinMatcher.group().trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }

        Matcher cjkMatcher = CJK_CHAR.matcher(normalized);
        while (cjkMatcher.find()) {
            tokens.add(cjkMatcher.group());
        }

        return tokens;
    }

    private record IndexedSegment(TextSegment textSegment, Set<String> tokens) {
    }

    private record ScoredSegment(IndexedSegment segment, double score) {
    }
}
