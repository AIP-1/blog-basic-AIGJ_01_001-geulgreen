package com.team.blog.discovery.web;

import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.post.application.TagListingQuery;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.tag.domain.TagNormalizer;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

/**
 * 태그 화면·API(013, 22 §5~§7). 태그 주소는 이름을 경로 조각으로 인코딩한다({@code #} → {@code %23}). 정리되지 않은 이름은
 * 정리된 주소로 301, 형식에 맞지 않으면 404. 공개 글이 없는 태그도 정상 페이지(아무도 안 씀과 비공개 글에만 쓰임을 구별하지 않음).
 */
@Controller
public class TagPageController {

    private final TagListingQuery tagQuery;
    private final PostListQuery listQuery;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final com.team.blog.discovery.application.TagBrowseQuery browseQuery;
    private final com.team.blog.tag.application.icon.TagIconService tagIcons;

    public TagPageController(TagListingQuery tagQuery, PostListQuery listQuery, CurrentUserProvider currentUserProvider,
                             Clock clock, com.team.blog.discovery.application.TagBrowseQuery browseQuery,
                             com.team.blog.tag.application.icon.TagIconService tagIcons) {
        this.browseQuery = browseQuery;
        this.tagIcons = tagIcons;
        this.tagQuery = tagQuery;
        this.listQuery = listQuery;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public static String tagPath(String name) {
        return "/tags/" + UriUtils.encodePathSegment(name, java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/tags/{name}")
    public Object tag(@PathVariable("name") String raw, @RequestParam(value = "cursor", required = false) String cursor,
                      Model model) {
        String name = TagNormalizer.lookupName(raw).orElseThrow(NotFoundException::new);
        if (!name.equals(raw)) {
            RedirectView redirect = new RedirectView(tagPath(name));
            redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirect;
        }
        Optional<Long> tagId = tagQuery.tagId(name);
        CardPage page;
        try {
            page = tagId.map(id -> listQuery.byTag(id, cursor)).orElse(new CardPage(List.of(), null));
        } catch (PostContentException e) {
            page = tagId.map(id -> listQuery.byTag(id, null)).orElse(new CardPage(List.of(), null));
        }
        CardModel.fill(model, page, clock.instant());
        model.addAttribute("tagName", name);
        model.addAttribute("publicCount", tagId.isPresent() ? tagQuery.publicCount(name) : 0);
        model.addAttribute("continued", cursor != null && !cursor.isEmpty());
        model.addAttribute("listApi", "/api" + tagPath(name) + "/posts");
        model.addAttribute("tagPath", tagPath(name));
        return "tag/tag";
    }

    /** 한 번에 그리는 태그 묶음 수와 묶음마다 카드 수. */
    static final int SECTIONS = 10;
    static final int CARDS = 10;

    /**
     * 태그 모아 보기. 강성찬 개인 확장(10-15, devlog 태그 화면 참고): 제목·전체 태그 수, 태그 검색, 인기순·최근순(이름순은 10-15에 뺌),
     * 태그마다 가로로 넘기는 글 카드 줄. 처음 {@value #SECTIONS}개 묶음, 나머지는 끝에 가까워지면 {@code /tags/sections}로 이어 붙인다.
     */
    @GetMapping("/tags")
    public String tags(@RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "q", required = false) String q, Model model) {
        fillSections(model, sort, q, 0);
        model.addAttribute("tagTotal", browseQuery.count(q));
        return "tag/tags";
    }

    /** 이어 붙일 태그 묶음(HTML 조각). */
    @GetMapping("/tags/sections")
    public String moreSections(@RequestParam(value = "sort", required = false) String sort,
                               @RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "offset", defaultValue = "0") int offset, Model model) {
        fillSections(model, sort, q, Math.max(0, offset));
        return "tag/tags :: sections";
    }

    private void fillSections(Model model, String sort, String q, int offset) {
        String s = "recent".equals(sort) ? sort : "popular"; // 이름순은 뺐다(10-15 chan), 예전 ?sort=name은 인기순
        String query = q == null ? "" : q.strip();
        if (query.length() > 30) {
            query = query.substring(0, 30);
        }
        List<com.team.blog.discovery.application.TagBrowseQuery.Section> sections = browseQuery.sections(s, query, offset, SECTIONS + 1);
        boolean more = sections.size() > SECTIONS;
        sections = more ? sections.subList(0, SECTIONS) : sections;
        java.time.Instant now = clock.instant();
        java.util.Map<Long, List<com.team.blog.post.application.PostCard>> cards = new java.util.HashMap<>();
        java.util.Map<Long, String> dates = new java.util.HashMap<>();
        java.util.Map<String, String> latest = new java.util.HashMap<>();
        for (var sec : sections) {
            List<com.team.blog.post.application.PostCard> row = listQuery.byTag(sec.id(), null).items().stream().limit(CARDS).toList();
            cards.put(sec.id(), row);
            row.forEach(c -> dates.put(c.id(), com.team.blog.post.application.CardDates.label(c.firstPublicAt(), now)));
            latest.put(sec.name(), com.team.blog.post.application.CardDates.label(sec.latest(), now));
        }
        model.addAttribute("sections", sections);
        model.addAttribute("sectionCards", cards);
        model.addAttribute("cardDates", dates);
        model.addAttribute("latestLabels", latest);
        model.addAttribute("tagStyles", tagIcons.styles(sections.stream().map(com.team.blog.discovery.application.TagBrowseQuery.Section::name).toList()));
        model.addAttribute("sort", s);
        model.addAttribute("q", query);
        model.addAttribute("moreUrl", more ? org.springframework.web.util.UriComponentsBuilder.fromPath("/tags/sections")
                .queryParam("sort", s).queryParamIfPresent("q", Optional.of(query).filter(x -> !x.isEmpty()))
                .queryParam("offset", offset + SECTIONS).encode().build().toUriString() : null);
    }

    @GetMapping("/api/tags/{name}/posts")
    @ResponseBody
    public CardPage tagPosts(@PathVariable("name") String raw, @RequestParam(value = "cursor", required = false) String cursor) {
        String name = TagNormalizer.lookupName(raw).orElseThrow(NotFoundException::new);
        return tagQuery.tagId(name).map(id -> listQuery.byTag(id, cursor)).orElse(new CardPage(List.of(), null));
    }

    @GetMapping("/api/tags")
    @ResponseBody
    public List<TagListingQuery.TagCount> topTags(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return tagQuery.top(Math.clamp(limit, 1, 100));
    }

    @GetMapping("/api/tags/suggest")
    @ResponseBody
    public List<TagListingQuery.Suggestion> suggest(@RequestParam(value = "q", required = false) String q) {
        return tagQuery.suggest(currentUserProvider.current(), q);
    }
}
