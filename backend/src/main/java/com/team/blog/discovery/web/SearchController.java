package com.team.blog.discovery.web;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.discovery.application.FuzzyText;
import com.team.blog.discovery.application.SearchProperties;
import com.team.blog.discovery.application.SearchService;
import com.team.blog.discovery.application.SearchSuggestService;
import com.team.blog.discovery.application.SearchTerms;
import com.team.blog.discovery.application.VisitorKeys;
import com.team.blog.post.application.TagListingQuery;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.ClientIpResolver;
import com.team.blog.shared.web.VisitorCookie;
import com.team.blog.tag.domain.TagNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

/**
 * 검색(020, 33 §5). 화면 {@code /search}(글·사람 탭, 관련도·최신), [더 보기] API, 블로그 안 검색({@code /@주소?q=}은
 * {@link BlogPageController}가 여기로 넘긴다). 같은 방문자 1분 30번, 결과 화면은 색인 금지.
 */
@Controller
public class SearchController {

    private final SearchService searchService;
    private final BlogOwnerResolver ownerResolver;
    private final TagListingQuery tagListingQuery;
    private final VisitorKeys visitorKeys;
    private final CurrentUserProvider currentUserProvider;
    private final ClientIpResolver clientIpResolver;
    private final java.time.Clock clock;
    private final SearchSuggestService suggestService;
    private final SearchProperties properties;

    public SearchController(SearchService searchService, BlogOwnerResolver ownerResolver, TagListingQuery tagListingQuery,
                            VisitorKeys visitorKeys, CurrentUserProvider currentUserProvider, ClientIpResolver clientIpResolver,
                            java.time.Clock clock, SearchSuggestService suggestService, SearchProperties properties) {
        this.clock = clock;
        this.suggestService = suggestService;
        this.properties = properties;
        this.searchService = searchService;
        this.ownerResolver = ownerResolver;
        this.tagListingQuery = tagListingQuery;
        this.visitorKeys = visitorKeys;
        this.currentUserProvider = currentUserProvider;
        this.clientIpResolver = clientIpResolver;
    }

    private void limit(HttpServletRequest request) {
        searchService.limit(visitorKeys.key(currentUserProvider.current(), VisitorCookie.read(request),
                clientIpResolver.resolve(request), request.getHeader("User-Agent")));
    }

    @GetMapping("/search")
    public Object page(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "tab", required = false) String tab,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "cursor", required = false) String cursor,
                       HttpServletRequest request, Model model) {
        String raw = q == null ? "" : q.strip();
        // FR-020: "#태그"는 그 태그가 있으면 태그 글 목록으로, 없으면 # 뺀 나머지로 검색
        if (raw.startsWith("#")) {
            Optional<String> tag = TagNormalizer.lookupName(raw.substring(1)).filter(t -> tagListingQuery.tagId(t).isPresent());
            if (tag.isPresent()) {
                RedirectView view = new RedirectView("/tags/" + UriUtils.encodePathSegment(tag.get(), StandardCharsets.UTF_8));
                view.setStatusCode(HttpStatus.SEE_OTHER);
                return view;
            }
            raw = raw.substring(1);
        }
        limit(request);
        fill(model, raw, tab, sort, cursor, null);
        return "search/results";
    }

    /** {@code /@주소?q=}: 그 블로그 주인의 공개 글로만(FR-017). */
    public String blog(BlogOwner owner, String q, String sort, String cursor, HttpServletRequest request, Model model) {
        limit(request);
        fill(model, q, "posts", sort, cursor, owner);
        return "search/results";
    }

    private void fill(Model model, String raw, String tab, String sort, String cursor, BlogOwner owner) {
        SearchTerms terms = SearchTerms.parse(raw);
        boolean people = "people".equals(tab) && owner == null;
        boolean latest = "latest".equals(sort);
        model.addAttribute("pageNoindex", true);
        model.addAttribute("q", terms.text());
        model.addAttribute("tab", people ? "people" : "posts");
        model.addAttribute("sort", latest ? "latest" : "relevance");
        model.addAttribute("owner", owner);
        model.addAttribute("emptyQuery", terms.empty());
        model.addAttribute("twoCharNotice", terms.hasTwoCharWord());
        String base = owner == null ? "/search" : com.team.blog.shared.web.BlogPaths.of(owner.handle());
        model.addAttribute("searchBase", base);
        String enc = UriUtils.encodeQueryParam(terms.text(), StandardCharsets.UTF_8);
        model.addAttribute("encodedQ", enc);
        Long ownerId = owner == null ? null : owner.memberId();
        if (people) {
            List<SearchService.Person> found = searchService.people(terms);
            model.addAttribute("people", found);
            Optional<String> fix = suggestion(terms, found.size(), null, model);
            model.addAttribute("fixedPeople", fix.isPresent() && found.isEmpty() ? searchService.people(SearchTerms.parse(fix.get())) : List.of());
            return;
        }
        SearchService.SearchPage page;
        boolean continued = cursor != null && !cursor.isEmpty();
        try {
            page = searchService.posts(terms, latest, owner == null ? null : owner.memberId(), cursor);
        } catch (PostContentException e) {
            page = searchService.posts(terms, latest, owner == null ? null : owner.memberId(), null);
            continued = false;
        }
        List<SearchService.SearchCard> fixedResults = List.of();
        List<SearchService.SearchCard> similar = List.of();
        Optional<String> fix = continued ? Optional.empty() : suggestion(terms, page.items().size(), ownerId, model);
        if (!continued && properties.suggestEnabled() && page.items().size() < properties.suggestBelow()) {
            java.util.Set<Long> shown = new java.util.HashSet<>();
            page.items().forEach(c -> shown.add(c.id()));
            java.util.List<String> words = new java.util.ArrayList<>(terms.words());
            if (fix.isPresent()) {
                SearchTerms fixedTerms = SearchTerms.parse(fix.get());
                fixedResults = searchService.posts(fixedTerms, latest, ownerId, null).items().stream()
                        .filter(c -> shown.add(c.id())).toList();
                words.addAll(fixedTerms.words());
            }
            similar = searchService.cards(suggestService.similar(words, ownerId, shown, properties.similarLimit()), terms);
        }
        model.addAttribute("fixedResults", fixedResults);
        model.addAttribute("similarResults", similar);
        java.util.Map<Long, String> dates = new java.util.HashMap<>();
        java.time.Instant now = java.time.Instant.now(clock);
        java.util.stream.Stream.of(page.items(), fixedResults, similar).flatMap(List::stream)
                .forEach(c -> dates.put(c.id(), com.team.blog.post.application.CardDates.label(c.firstPublicAt(), now)));
        model.addAttribute("dates", dates);
        model.addAttribute("results", page.items());
        model.addAttribute("nextCursor", page.nextCursor());
        model.addAttribute("continued", continued);
        model.addAttribute("listApi", "/api/search/posts?q=" + enc + "&sort=" + (latest ? "latest" : "relevance")
                + (owner == null ? "" : "&blog=" + owner.handle()));
        model.addAttribute("moreLinkBase", "?q=" + enc + (owner == null ? "&tab=posts" : "") + "&sort="
                + (latest ? "latest" : "relevance") + "&cursor=");
    }

    /** 094: 결과가 적으면 고친 검색어를 화면에 넘긴다("혹시 ○○을 찾으셨나요?"). 스위치를 끄면 늘 비어 있다. */
    private Optional<String> suggestion(SearchTerms terms, int found, Long ownerId, Model model) {
        Optional<String> fix = properties.suggestEnabled() && found < properties.suggestBelow()
                ? suggestService.correct(terms, ownerId) : Optional.empty();
        model.addAttribute("suggestion", fix.orElse(null));
        model.addAttribute("suggestionParticle", fix.map(FuzzyText::objectParticle).orElse(null));
        model.addAttribute("encodedSuggestion", fix.map(f -> UriUtils.encodeQueryParam(f, StandardCharsets.UTF_8)).orElse(null));
        return fix;
    }

    @GetMapping("/api/search/posts")
    @ResponseBody
    public SearchService.SearchPage postsApi(@RequestParam(value = "q", required = false) String q,
                                             @RequestParam(value = "sort", required = false) String sort,
                                             @RequestParam(value = "cursor", required = false) String cursor,
                                             @RequestParam(value = "blog", required = false) String blog,
                                             HttpServletRequest request) {
        limit(request);
        Long ownerId = blog == null ? null : ownerResolver.resolve(blog).orElseThrow(NotFoundException::new).memberId();
        return searchService.posts(SearchTerms.parse(q), "latest".equals(sort), ownerId, cursor);
    }

    @GetMapping("/api/search/people")
    @ResponseBody
    public List<SearchService.Person> peopleApi(@RequestParam(value = "q", required = false) String q,
                                                HttpServletRequest request) {
        limit(request);
        return searchService.people(SearchTerms.parse(q));
    }
}
