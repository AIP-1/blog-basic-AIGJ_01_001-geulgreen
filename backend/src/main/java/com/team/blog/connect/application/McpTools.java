package com.team.blog.connect.application;

import com.team.blog.category.application.CategoryService;
import com.team.blog.post.application.EditingContent;
import com.team.blog.post.application.ManagePage;
import com.team.blog.post.application.ManageTab;
import com.team.blog.post.application.PostDraftService;
import com.team.blog.post.application.PostManageQuery;
import com.team.blog.post.application.PostProperties;
import com.team.blog.post.application.SaveCommand;
import com.team.blog.post.domain.PostStatus;
import com.team.blog.post.markdown.MarkdownProperties;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.tag.application.PostTagService;
import com.team.blog.tag.domain.TagNormalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * 097 AI 연결의 MCP 도구 네 개. 모두 키 주인의 글만 다루고, 임시글만 만들고 고친다. 발행·삭제·남의 글은 없다.
 * 권한·입력 규칙은 글쓰기 화면과 같은 Service를 거친다.
 */
@Service
public class McpTools {

    /** 도구가 돌려주는 실패. {@code code}는 화면 API와 같은 이유 코드. */
    public static class ToolException extends RuntimeException {
        public ToolException(String code) {
            super(code);
        }
    }

    public record Tool(String name, String description, Map<String, Object> inputSchema) {
    }

    private static final Map<String, Object> TITLE = Map.of("type", "string", "description", "제목(100자까지)");
    private static final Map<String, Object> CONTENT = Map.of("type", "string", "description", "본문(Markdown)");
    private static final Map<String, Object> TAGS = Map.of("type", "array", "items", Map.of("type", "string"),
            "description", "태그(10개까지, # 없이). 주면 글의 태그를 이 목록으로 바꿔요");
    private static final Map<String, Object> CATEGORY = Map.of("type", "integer",
            "description", "카테고리 번호(list_tags_and_categories로 확인)");
    private static final Map<String, Object> POST_ID = Map.of("type", "integer", "description", "임시글 번호");

    public static final List<Tool> TOOLS = List.of(
            new Tool("create_draft", "글그린 블로그에 새 임시글을 만들어요. 공개되지 않고, 글쓴이가 글쓰기 화면에서 읽고 직접 발행해요."
                    + " 나눈 이야기나 한 작업을 블로그 글로 정리할 때 써요. 비밀번호·토큰·개인 정보는 넣지 마세요.",
                    schema(Map.of("title", TITLE, "content_md", CONTENT, "tags", TAGS, "category_id", CATEGORY),
                            List.of("title", "content_md"))),
            new Tool("update_draft", "내 임시글을 고쳐요. 준 칸만 바뀌어요. 발행한 글은 고칠 수 없어요.",
                    schema(Map.of("post_id", POST_ID, "title", TITLE, "content_md", CONTENT, "tags", TAGS), List.of("post_id"))),
            new Tool("list_drafts", "내 임시글 목록(최근에 고친 순)을 봐요. 이어 쓸 글을 찾을 때 써요.",
                    schema(Map.of("cursor", Map.of("type", "string", "description", "다음 쪽(앞 응답의 next_cursor)")), List.of())),
            new Tool("list_tags_and_categories", "내가 쓴 태그(많이 쓴 순)와 내 카테고리 목록을 봐요. 태그를 고를 때 있는 이름을 먼저 써요.",
                    schema(Map.of(), List.of())));

    private final PostDraftService drafts;
    private final PostManageQuery manageQuery;
    private final PostTagService tagService;
    private final TagNormalizer tagNormalizer;
    private final CategoryService categories;
    private final PostProperties postProperties;
    private final MarkdownProperties markdownProperties;
    private final JdbcTemplate jdbc;

    public McpTools(PostDraftService drafts, PostManageQuery manageQuery, PostTagService tagService, TagNormalizer tagNormalizer,
                    CategoryService categories, PostProperties postProperties, MarkdownProperties markdownProperties,
                    JdbcTemplate jdbc) {
        this.drafts = drafts;
        this.manageQuery = manageQuery;
        this.tagService = tagService;
        this.tagNormalizer = tagNormalizer;
        this.categories = categories;
        this.postProperties = postProperties;
        this.markdownProperties = markdownProperties;
        this.jdbc = jdbc;
    }

    /** 도구 실행. 없는 도구는 {@code UNKNOWN_TOOL}. 돌려준 값은 JSON으로 그대로 직렬화된다. */
    public Map<String, Object> call(Optional<CurrentUser> user, String name, JsonNode args) {
        return switch (name == null ? "" : name) {
            case "create_draft" -> createDraft(user, args);
            case "update_draft" -> updateDraft(user, args);
            case "list_drafts" -> listDrafts(user, args);
            case "list_tags_and_categories" -> listTagsAndCategories(user);
            default -> throw new ToolException("UNKNOWN_TOOL");
        };
    }

    private Map<String, Object> createDraft(Optional<CurrentUser> user, JsonNode args) {
        List<String> tags = tags(args); // 태그부터 검사해서 잘못된 태그로 빈 임시글이 남지 않게
        Long categoryId = categories.enabled() ? number(args, "category_id") : null;
        if (categoryId != null && user.isPresent() && categories.find(user.get().memberId(), categoryId).isEmpty()) {
            throw new ToolException("CATEGORY_NOT_FOUND");
        }
        long postId = drafts.create(user, required(args, "title"), required(args, "content_md"));
        if (tags != null) {
            tagService.replace(postId, tags);
        }
        if (categoryId != null) {
            categories.assign(user, postId, categoryId);
        }
        return draft(postId, drafts.editing(user, postId));
    }

    private Map<String, Object> updateDraft(Optional<CurrentUser> user, JsonNode args) {
        Long postId = number(args, "post_id");
        if (postId == null) {
            throw new ToolException("INVALID_REQUEST");
        }
        List<String> tags = tags(args);
        EditingContent current = drafts.editing(user, postId);
        if (current.status() != PostStatus.DRAFT) {
            throw new ToolException("NOT_A_DRAFT");
        }
        String title = text(args, "title");
        String content = text(args, "content_md");
        if (title != null || content != null) {
            drafts.save(user, postId, new SaveCommand(title != null ? title : current.title(),
                    content != null ? content : current.contentMd(), current.version()));
        }
        if (tags != null) {
            tagService.replace(postId, tags);
        }
        return draft(postId, drafts.editing(user, postId));
    }

    private Map<String, Object> listDrafts(Optional<CurrentUser> user, JsonNode args) {
        ManagePage page = manageQuery.page(user, ManageTab.DRAFTS, null, text(args, "cursor"));
        List<Map<String, Object>> items = page.items().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("post_id", row.id());
            m.put("title", row.title());
            m.put("updated_at", row.updatedAt().toString());
            m.put("edit_url", editUrl(row.id()));
            return m;
        }).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("drafts", items);
        out.put("next_cursor", page.nextCursor());
        return out;
    }

    private Map<String, Object> listTagsAndCategories(Optional<CurrentUser> user) {
        long me = user.orElseThrow(() -> new ToolException("LOGIN_REQUIRED")).memberId();
        List<Map<String, Object>> tags = jdbc.query("""
                SELECT t.name, count(*) AS n FROM post_tag pt JOIN tag t ON t.id = pt.tag_id JOIN post p ON p.id = pt.post_id
                WHERE p.author_id = ? AND p.deleted_at IS NULL GROUP BY t.name ORDER BY n DESC, t.name LIMIT 100""",
                (rs, i) -> Map.<String, Object>of("name", rs.getString("name"), "posts", rs.getLong("n")), me);
        List<Map<String, Object>> cats = categories.enabled() ? categories.list(me).stream()
                .map(c -> Map.<String, Object>of("category_id", c.id(), "name", c.name())).toList() : List.of();
        return Map.of("tags", tags, "categories", cats);
    }

    private Map<String, Object> draft(long postId, EditingContent c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("post_id", postId);
        out.put("status", "DRAFT");
        out.put("title", c.title());
        out.put("tags", tagService.tagsOf(postId));
        out.put("edit_url", editUrl(postId));
        out.put("note", "임시글이에요. 글쓴이가 이 주소에서 읽고 직접 발행해요.");
        return out;
    }

    private String editUrl(long postId) {
        return markdownProperties.siteOrigin().replaceAll("/+$", "") + "/write/" + postId;
    }

    /** 태그 칸이 없으면 null(바꾸지 않음). 있으면 정규화·중복 제거·개수 확인. */
    private List<String> tags(JsonNode args) {
        JsonNode node = args == null ? null : args.get("tags");
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new ToolException("INVALID_REQUEST");
        }
        Set<String> out = new LinkedHashSet<>();
        for (JsonNode t : node) {
            if (!t.isString()) {
                throw new ToolException("INVALID_TAG");
            }
            out.add(tagNormalizer.normalize(t.stringValue()));
        }
        if (out.size() > postProperties.maxTags()) {
            throw new ToolException("TOO_MANY_TAGS");
        }
        return List.copyOf(out);
    }

    private static String required(JsonNode args, String field) {
        String v = text(args, field);
        if (v == null) {
            throw new ToolException("INVALID_REQUEST");
        }
        return v;
    }

    private static String text(JsonNode args, String field) {
        JsonNode node = args == null ? null : args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isString()) {
            throw new ToolException("INVALID_REQUEST");
        }
        return node.stringValue();
    }

    private static Long number(JsonNode args, String field) {
        JsonNode node = args == null ? null : args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw new ToolException("INVALID_REQUEST");
        }
        return node.longValue();
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        if (!required.isEmpty()) {
            s.put("required", required);
        }
        s.put("additionalProperties", false);
        return s;
    }
}
