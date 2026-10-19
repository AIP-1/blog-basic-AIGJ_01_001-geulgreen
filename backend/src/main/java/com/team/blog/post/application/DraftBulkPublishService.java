package com.team.blog.post.application;

import com.team.blog.post.domain.PostStatus;
import com.team.blog.post.infra.PostEditStore;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.event.PostsPublishedTogether;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 113 후속 강성찬 개인 확장: 내 글 &gt; 임시글에서 고른 글을 한 번에 발행. 글마다 저장된 발행 설정(공개 범위·태그)으로
 * 하나씩 발행하고, 발행에 필요한 것이 빠진 글(제목 없음 등)은 건너뛰어 이름과 이유를 돌려준다. 팔로워 알림은 한 번만.
 */
@Service
public class DraftBulkPublishService {

    public static final int MAX = 100;

    public record Skipped(long id, String title, String reason) {
    }

    public record Result(List<Long> published, List<Skipped> skipped) {
    }

    private final AccountGuard accountGuard;
    private final PostEditStore store;
    private final PostDraftService drafts;
    private final PostPublishService publishService;
    private final ApplicationEventPublisher events;

    public DraftBulkPublishService(AccountGuard accountGuard, PostEditStore store, PostDraftService drafts,
                                   PostPublishService publishService, ApplicationEventPublisher events) {
        this.accountGuard = accountGuard;
        this.store = store;
        this.drafts = drafts;
        this.publishService = publishService;
        this.events = events;
    }

    public Result publish(Optional<CurrentUser> currentUser, List<Long> postIds) {
        CurrentUser user = accountGuard.requireWritable(currentUser);
        List<Long> ids = postIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(postIds));
        if (ids.isEmpty() || ids.size() > MAX) {
            throw new com.team.blog.shared.error.PostContentException("INVALID_REQUEST");
        }
        List<Long> published = new ArrayList<>();
        List<Long> publicIds = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();
        PostsPublishedTogether.begin();
        try {
            for (Long id : ids) {
                Optional<PostEditRow> row = store.findActive(id)
                        .filter(r -> r.authorId() == user.memberId() && r.status() == PostStatus.DRAFT);
                if (row.isEmpty()) {
                    skipped.add(new Skipped(id, null, "임시글이 아니에요"));
                    continue;
                }
                EditingContent content = drafts.current(row.get());
                PostDraftService.PublishDefaults settings = drafts.publishDefaults(Optional.of(user), id);
                String title = content.title() == null || content.title().isBlank() ? "(제목 없음)" : content.title();
                try {
                    publishService.publish(Optional.of(user), id, "bulk-" + id + "-" + content.version(),
                            new PublishCommand(content.title(), content.contentMd(), settings.tags(), settings.visibility(), content.version()));
                    published.add(id);
                    if ("PUBLIC".equals(settings.visibility())) {
                        publicIds.add(id);
                    }
                } catch (NotFoundException e) {
                    skipped.add(new Skipped(id, title, "글을 찾지 못했어요"));
                } catch (RuntimeException e) {
                    skipped.add(new Skipped(id, title, reason(e)));
                }
            }
        } finally {
            PostsPublishedTogether.end();
        }
        if (!publicIds.isEmpty()) {
            events.publishEvent(new PostsPublishedTogether(user.memberId(), List.copyOf(publicIds)));
        }
        return new Result(List.copyOf(published), List.copyOf(skipped));
    }

    /** 발행 규칙에 걸린 이유를 사람이 읽는 말로(PublishValidator의 칸 오류 코드). */
    static String reason(RuntimeException e) {
        if (e instanceof com.team.blog.shared.error.ProfileValidationException v && !v.getErrors().isEmpty()) {
            String code = v.getErrors().get(0).code();
            return switch (code) {
                case "TITLE_REQUIRED" -> "제목이 없어요";
                case "CONTENT_REQUIRED" -> "본문이 없어요";
                case "PENDING_IMAGES" -> "올리는 중인 사진이 있어요";
                case "TOO_MANY_TAGS" -> "태그가 너무 많아요";
                case "INVALID_IMAGE" -> "쓸 수 없는 사진이 있어요";
                default -> code.contains("TOO_LONG") ? "글이 너무 길어요" : code.startsWith("TAG") ? "쓸 수 없는 태그가 있어요" : "발행 조건이 맞지 않아요";
            };
        }
        return "발행하지 못했어요";
    }
}
