package com.team.blog.discovery.web;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.discovery.application.RssProperties;
import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.markdown.MarkdownProperties;
import com.team.blog.shared.error.NotFoundException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** 044 RSS 2.0(강성찬 개인 확장): 전체·블로그별 공개 글 최신 N개. 공용 목록 조건(012) 글만, 본문 대신 요약. */
@RestController
@ConditionalOnProperty(name = "blog.rss.enabled", havingValue = "true", matchIfMissing = true)
public class RssController {

    private static final MediaType RSS = MediaType.parseMediaType("application/rss+xml;charset=UTF-8");

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final BlogOwnerResolver owners;
    private final RssProperties properties;
    private final String origin;

    public RssController(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, BlogOwnerResolver owners, RssProperties properties,
                         MarkdownProperties markdownProperties) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.owners = owners;
        this.properties = properties;
        this.origin = markdownProperties.siteOrigin().replaceAll("/+$", "");
    }

    @GetMapping("/rss")
    public ResponseEntity<String> all() {
        return feed("글그린 새 글", "/", null);
    }

    @GetMapping("/@{handle}/rss")
    public ResponseEntity<String> blog(@PathVariable("handle") String handle) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        return feed(owner.handle() + "의 블로그", com.team.blog.shared.web.BlogPaths.of(owner.handle()), owner.memberId());
    }

    private ResponseEntity<String> feed(String title, String link, Long authorId) {
        List<Object> args = new ArrayList<>();
        String byAuthor = "";
        if (authorId != null) {
            byAuthor = " AND p.author_id = ?";
            args.add(authorId);
        }
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\"><channel>\n")
                .append("<title>").append(esc(title)).append("</title>\n")
                .append("<link>").append(esc(origin + link)).append("</link>\n")
                .append("<description>").append(esc(title)).append("</description>\n");
        jdbc.query("SELECT p.id, p.title, p.excerpt, p.first_public_at, m.handle, m.nickname FROM post p JOIN member m"
                        + " ON m.id = p.author_id WHERE " + accessPolicy.publicListingCondition("p", "m") + byAuthor
                        + " ORDER BY p.first_public_at DESC, p.id DESC LIMIT " + properties.size(),
                rs -> {
                    String url = origin + com.team.blog.shared.web.BlogPaths.of(rs.getString("handle")) + "/posts/" + rs.getLong("id");
                    xml.append("<item><title>").append(esc(rs.getString("title"))).append("</title>")
                            .append("<link>").append(esc(url)).append("</link>")
                            .append("<guid isPermaLink=\"true\">").append(esc(url)).append("</guid>")
                            .append("<author>").append(esc(rs.getString("handle"))).append("</author>")
                            .append("<pubDate>").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(
                                    rs.getTimestamp("first_public_at").toInstant().atOffset(ZoneOffset.UTC))).append("</pubDate>")
                            .append("<description>").append(esc(rs.getString("excerpt"))).append("</description></item>\n");
                }, args.toArray());
        xml.append("</channel></rss>\n");
        return ResponseEntity.ok().contentType(RSS).body(xml.toString());
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }
}
