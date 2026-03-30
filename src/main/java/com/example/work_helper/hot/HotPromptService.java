package com.example.work_helper.hot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HotPromptService {

    private static final Logger log = LoggerFactory.getLogger(HotPromptService.class);

    private static final String JUEJIN_FEED_URL = "https://api.juejin.cn/recommend_api/v1/article/recommend_all_feed";
    private static final String V2EX_HOT_URL = "https://www.v2ex.com/api/topics/hot.json";
    private static final Duration HOT_SOURCE_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration HOT_SOURCE_READ_TIMEOUT = Duration.ofSeconds(4);
    private static final int MAX_PROMPTS = 5;

    private static final List<KeywordWeight> THEME_KEYWORDS = List.of(
            new KeywordWeight("面试", 140),
            new KeywordWeight("求职", 130),
            new KeywordWeight("简历", 120),
            new KeywordWeight("offer", 120),
            new KeywordWeight("校招", 115),
            new KeywordWeight("社招", 110),
            new KeywordWeight("实习", 110),
            new KeywordWeight("八股", 105),
            new KeywordWeight("算法", 100),
            new KeywordWeight("项目", 95),
            new KeywordWeight("后端", 95),
            new KeywordWeight("前端", 90),
            new KeywordWeight("全栈", 85),
            new KeywordWeight("程序员", 82),
            new KeywordWeight("编程", 80),
            new KeywordWeight("开发", 76),
            new KeywordWeight("学习路线", 74),
            new KeywordWeight("学习", 60),
            new KeywordWeight("java", 95),
            new KeywordWeight("spring", 88),
            new KeywordWeight("spring boot", 88),
            new KeywordWeight("mysql", 80),
            new KeywordWeight("redis", 78),
            new KeywordWeight("jvm", 76),
            new KeywordWeight("并发", 74),
            new KeywordWeight("微服务", 72),
            new KeywordWeight("docker", 60),
            new KeywordWeight("kubernetes", 58),
            new KeywordWeight("python", 70),
            new KeywordWeight("golang", 70),
            new KeywordWeight("go", 46),
            new KeywordWeight("vue", 62),
            new KeywordWeight("react", 62),
            new KeywordWeight("数据库", 58),
            new KeywordWeight("系统设计", 76)
    );

    private static final Set<String> REQUIRED_THEME_TOKENS = Set.of(
            "面试", "求职", "简历", "offer", "校招", "社招", "实习", "八股", "算法", "项目",
            "后端", "前端", "全栈", "程序员", "编程", "开发", "学习", "java", "spring",
            "mysql", "redis", "jvm", "并发", "微服务", "python", "golang", "go", "vue", "react"
    );

    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "抽奖", "广告", "活动", "吐槽", "闲聊", "相亲", "八卦", "优惠", "返利", "促销",
            "摄影", "旅游", "电影", "装修", "租房", "情感", "股票", "基金", "车险"
    );

    private static final Set<String> V2EX_ALLOWED_NODES = Set.of(
            "career", "programmer", "java", "python", "go", "mysql", "server", "devops", "frontend"
    );

    private static final List<HotPromptItem> DEFAULT_PROMPTS = List.of(
            new HotPromptItem(
                    "default-java-roadmap",
                    "零基础转 Java 后端，3 个月应该怎么规划学习路线？",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：零基础转 Java 后端，3 个月应该怎么规划学习路线？",
                    "学习路线",
                    "",
                    "热榜兜底"
            ),
            new HotPromptItem(
                    "default-interview-java",
                    "Java 后端面试中，JVM、并发、Spring、MySQL 应该怎么系统准备？",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：Java 后端面试中，JVM、并发、Spring、MySQL 应该怎么系统准备？",
                    "面试准备",
                    "",
                    "热榜兜底"
            ),
            new HotPromptItem(
                    "default-project-resume",
                    "后端求职项目经历怎么包装，才能让简历更有竞争力？",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：后端求职项目经历怎么包装，才能让简历更有竞争力？",
                    "项目简历",
                    "",
                    "热榜兜底"
            ),
            new HotPromptItem(
                    "default-offer-plan",
                    "如果目标是拿到后端 offer，刷题、八股、项目和投递应该怎么分配时间？",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：如果目标是拿到后端 offer，刷题、八股、项目和投递应该怎么分配时间？",
                    "求职规划",
                    "",
                    "热榜兜底"
            ),
            new HotPromptItem(
                    "default-springboot",
                    "用通俗的话解释 Spring Boot 自动装配，并说清常见面试追问。",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：用通俗的话解释 Spring Boot 自动装配，并说清常见面试追问。",
                    "框架原理",
                    "",
                    "热榜兜底"
            ),
            new HotPromptItem(
                    "default-rag-ai",
                    "做一个 AI 编程助手项目，RAG、SSE 流式输出和 MCP 工具调用分别适合解决什么问题？",
                    "请围绕这个编程学习或求职问题，给我一份结构化中文回答，包含背景分析、具体建议、常见误区、面试延伸问题和下一步行动方案：做一个 AI 编程助手项目，RAG、SSE 流式输出和 MCP 工具调用分别适合解决什么问题？",
                    "项目实战",
                    "",
                    "热榜兜底"
            )
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private volatile CachedHotPrompts cache;

    public HotPromptService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HOT_SOURCE_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(HOT_SOURCE_READ_TIMEOUT);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "AI-Code-Helper/1.0")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .build();
        this.objectMapper = objectMapper;
    }

    public List<HotPromptItem> getDailyHotPrompts(boolean refresh) {
        String today = LocalDate.now().toString();
        CachedHotPrompts currentCache = cache;
        if (!refresh && currentCache != null && currentCache.date().equals(today) && !currentCache.items().isEmpty()) {
            return currentCache.items();
        }

        List<HotPromptCandidate> mergedCandidates = new ArrayList<>();
        mergedCandidates.addAll(safeFetch("juejin", this::fetchJuejinHot));
        mergedCandidates.addAll(safeFetch("v2ex", this::fetchV2exHot));

        List<HotPromptItem> items = toPromptItems(mergedCandidates);
        cache = new CachedHotPrompts(today, items);
        return items;
    }

    private List<HotPromptCandidate> safeFetch(String source, CheckedSupplier<List<HotPromptCandidate>> fetcher) {
        try {
            return fetcher.get();
        } catch (Exception exception) {
            log.warn("Failed to fetch hot prompts from {}: {}", source, exception.getMessage());
            return List.of();
        }
    }

    private List<HotPromptCandidate> fetchJuejinHot() throws Exception {
        String responseBody = restClient.post()
                .uri(JUEJIN_FEED_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, "https://juejin.cn")
                .header(HttpHeaders.REFERER, "https://juejin.cn/")
                .body(Map.of(
                        "id_type", 2,
                        "sort_type", 200,
                        "cursor", "0",
                        "limit", 30
                ))
                .retrieve()
                .body(String.class);

        JsonNode data = objectMapper.readTree(responseBody).path("data");
        List<HotPromptCandidate> candidates = new ArrayList<>();

        for (JsonNode entry : data) {
            JsonNode itemInfo = entry.path("item_info");
            JsonNode article = itemInfo.path("article_info");
            String title = article.path("title").asText("");
            List<String> tags = new ArrayList<>();
            for (JsonNode tag : itemInfo.path("tags")) {
                String tagName = tag.path("tag_name").asText("");
                if (!tagName.isBlank()) {
                    tags.add(tagName);
                }
            }

            HotPromptCandidate candidate = buildCandidate(
                    "juejin-" + article.path("article_id").asText(""),
                    title,
                    "https://juejin.cn/post/" + article.path("article_id").asText(""),
                    tags.isEmpty() ? "掘金热榜" : tags.getFirst(),
                    "掘金",
                    tags,
                    "",
                    article.path("hot_index").asDouble(0)
                            + article.path("digg_count").asDouble(0) * 4
                            + article.path("comment_count").asDouble(0) * 6
                            + Math.log10(article.path("view_count").asDouble(0) + 1) * 55
            );

            if (isRelevant(candidate)) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private List<HotPromptCandidate> fetchV2exHot() throws Exception {
        String responseBody = restClient.get()
                .uri(V2EX_HOT_URL)
                .header(HttpHeaders.REFERER, "https://www.v2ex.com/")
                .retrieve()
                .body(String.class);

        JsonNode data = objectMapper.readTree(responseBody);
        List<HotPromptCandidate> candidates = new ArrayList<>();

        for (JsonNode item : data) {
            String nodeName = item.path("node").path("name").asText("");
            if (!V2EX_ALLOWED_NODES.contains(nodeName)) {
                continue;
            }

            String title = item.path("title").asText("");
            String nodeTitle = item.path("node").path("title").asText("");
            HotPromptCandidate candidate = buildCandidate(
                    "v2ex-" + item.path("id").asText(""),
                    title,
                    item.path("url").asText("https://www.v2ex.com/t/" + item.path("id").asText("")),
                    nodeTitle.isBlank() ? "V2EX 热门" : nodeTitle,
                    "V2EX",
                    List.of(nodeTitle),
                    nodeName,
                    item.path("replies").asDouble(0) * 18 + item.path("member").path("id").asDouble(0) / 1000
            );

            if (isRelevant(candidate)) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private HotPromptCandidate buildCandidate(
            String id,
            String title,
            String link,
            String tagLabel,
            String sourceLabel,
            List<String> tags,
            String node,
            double baseHotness
    ) {
        int themeHits = countThemeHits(title, tags, node);
        double themeScore = calculateThemeScore(title, tags, node);
        return new HotPromptCandidate(
                id,
                title,
                link,
                tagLabel,
                sourceLabel,
                themeHits,
                themeScore,
                baseHotness + themeScore
        );
    }

    private List<HotPromptItem> toPromptItems(List<HotPromptCandidate> candidates) {
        Map<String, HotPromptCandidate> uniqueByTitle = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator.comparingDouble(HotPromptCandidate::hotnessScore).reversed())
                .forEach(candidate -> uniqueByTitle.putIfAbsent(normalizeTitle(candidate.title()), candidate));

        List<HotPromptItem> items = uniqueByTitle.values().stream()
                .sorted(Comparator.comparingDouble(HotPromptCandidate::themeScore)
                        .thenComparingDouble(HotPromptCandidate::hotnessScore)
                        .reversed())
                .limit(MAX_PROMPTS)
                .map(candidate -> new HotPromptItem(
                        candidate.id(),
                        candidate.title(),
                        buildPrompt(candidate),
                        candidate.tagLabel(),
                        candidate.link(),
                        candidate.sourceLabel()
                ))
                .toList();

        if (items.size() >= MAX_PROMPTS) {
            return items;
        }

        LinkedHashMap<String, HotPromptItem> merged = new LinkedHashMap<>();
        items.forEach(item -> merged.putIfAbsent(normalizeTitle(item.title()), item));
        DEFAULT_PROMPTS.forEach(item -> merged.putIfAbsent(normalizeTitle(item.title()), item));
        return merged.values().stream().limit(MAX_PROMPTS).toList();
    }

    private String buildPrompt(HotPromptCandidate candidate) {
        return "请围绕这个编程学习、求职准备或技术面试问题，给我一份结构化中文回答，包含背景分析、核心知识点、可落地建议、面试延伸问题和下一步行动方案："
                + candidate.title()
                + "\n来源：" + candidate.sourceLabel();
    }

    private int countThemeHits(String title, List<String> tags, String node) {
        String merged = mergeText(title, tags, node);
        int hits = 0;
        for (String keyword : REQUIRED_THEME_TOKENS) {
            if (merged.contains(keyword)) {
                hits++;
            }
        }
        return hits;
    }

    private double calculateThemeScore(String title, List<String> tags, String node) {
        String merged = mergeText(title, tags, node);
        double score = 0;

        for (KeywordWeight keywordWeight : THEME_KEYWORDS) {
            if (merged.contains(keywordWeight.keyword())) {
                score += keywordWeight.weight();
            }
        }

        for (String keyword : NEGATIVE_KEYWORDS) {
            if (merged.contains(keyword)) {
                score -= 220;
            }
        }

        return score;
    }

    private String mergeText(String title, List<String> tags, String node) {
        return (title + " " + String.join(" ", tags) + " " + node).toLowerCase(Locale.ROOT);
    }

    private boolean isRelevant(HotPromptCandidate candidate) {
        if (candidate.title().isBlank()) {
            return false;
        }
        return candidate.themeHits() >= 1 && candidate.themeScore() >= 90;
    }

    private String normalizeTitle(String title) {
        return title.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private record KeywordWeight(String keyword, int weight) {
    }

    private record HotPromptCandidate(
            String id,
            String title,
            String link,
            String tagLabel,
            String sourceLabel,
            int themeHits,
            double themeScore,
            double hotnessScore
    ) {
    }

    private record CachedHotPrompts(String date, List<HotPromptItem> items) {
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
