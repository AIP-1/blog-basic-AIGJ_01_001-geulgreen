package com.team.blog.admin.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.post.application.PostDraftService;
import com.team.blog.post.application.PostPublishService;
import com.team.blog.post.application.PublishCommand;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 테스트용 긴 글(083, 강성찬 개인 확장): 관리자 화면 버튼 한 번으로 표시가 분명한 테스트 계정 4개와 긴 글을 넣고, 한 번으로 모두 지운다.
 * 글 본문은 {@code classpath:sample-posts/*.md}. 남의 글이어야 피드·탐색·추천에 보이므로 별도 계정으로 쓴다. 다시 눌러도 이미 있는 계정·글은 건너뛴다.
 * 계정은 로그인할 수 없다(아무도 모르는 비밀번호). {@code blog.admin.sample-posts-enabled=false}면 버튼도 기능도 없다.
 */
@Service
public class SamplePostsService {

    public record Account(String handle, String nickname) {
    }

    public static final List<Account> ACCOUNTS = List.of(
            new Account("test_minji", "테스트민지"), new Account("test_junho", "테스트준호"),
            new Account("test_seoyeon", "테스트서연"), new Account("test_doyun", "테스트도윤"));

    static final String BIO = "테스트용 계정이에요. 관리자 화면에서 한 번에 지울 수 있어요.";

    record SamplePost(String author, String title, List<String> tags, String body) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AdminGuard adminGuard;
    private final PostDraftService drafts;
    private final PostPublishService publisher;
    private final List<WithdrawalPurgeStep> purgeSteps;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final boolean enabled;

    public SamplePostsService(JdbcTemplate jdbc, TransactionTemplate tx, AdminGuard adminGuard, PostDraftService drafts,
                              PostPublishService publisher, List<WithdrawalPurgeStep> purgeSteps,
                              PasswordEncoder passwordEncoder, Clock clock,
                              @Value("${blog.admin.sample-posts-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.adminGuard = adminGuard;
        this.drafts = drafts;
        this.publisher = publisher;
        this.purgeSteps = purgeSteps.stream().sorted(Comparator.comparingInt(WithdrawalPurgeStep::order)).toList();
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    /** 지금 들어가 있는 테스트 글 수(화면 표시용). */
    public int count() {
        return enabled ? jdbc.queryForObject("""
                SELECT count(*) FROM post p JOIN member m ON m.id = p.author_id
                WHERE m.handle = ANY (?) AND m.deleted_at IS NULL AND p.deleted_at IS NULL
                """, Integer.class, (Object) handles()) : 0;
    }

    /** 넣은 글 수. 이미 같은 제목의 글이 있으면 건너뛴다. */
    public int seed(Optional<CurrentUser> current) {
        CurrentUser admin = requireEnabledAdmin(current);
        List<SamplePost> posts = load();
        Instant now = clock.instant();
        int added = 0;
        for (int i = 0; i < posts.size(); i++) {
            SamplePost post = posts.get(i);
            long authorId = account(post.author());
            Integer exists = jdbc.queryForObject("SELECT count(*) FROM post WHERE author_id = ? AND title = ? AND deleted_at IS NULL",
                    Integer.class, authorId, post.title());
            if (exists > 0) { // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
                continue;
            }
            Optional<CurrentUser> author = Optional.of(new CurrentUser(authorId, "USER"));
            long postId = drafts.create(author, post.title(), post.body());
            publisher.publish(author, postId, UUID.randomUUID().toString(),
                    new PublishCommand(post.title(), post.body(), post.tags(), "PUBLIC", 0L));
            // 피드 순서가 자연스럽게 보이도록 발행 시각을 몇 시간씩 앞으로 당긴다
            Timestamp at = Timestamp.from(now.minus(Duration.ofHours(5L * (posts.size() - i))));
            jdbc.update("UPDATE post SET published_at = ?, first_public_at = ?, created_at = ? WHERE id = ?", at, at, at, postId);
            added++;
        }
        log(admin, "SAMPLE_POSTS_SEED", "테스트 글 " + added + "편");
        return added;
    }

    /** 지운 계정 수. 탈퇴 정리 단계를 그대로 돌려 글·댓글·좋아요·팔로우·알림까지 함께 지우고, 아이디는 다시 쓸 수 있게 비운다. */
    public int remove(Optional<CurrentUser> current) {
        CurrentUser admin = requireEnabledAdmin(current);
        List<Long> ids = jdbc.queryForList("SELECT id FROM member WHERE handle = ANY (?) AND deleted_at IS NULL", Long.class,
                (Object) handles());
        Timestamp now = Timestamp.from(clock.instant());
        tx.executeWithoutResult(status -> {
            for (long id : ids) {
                jdbc.update("UPDATE member SET status = 'WITHDRAWN', withdrawn_at = ? WHERE id = ?", now, id);
                purgeSteps.forEach(step -> step.purge(id));
                jdbc.update("UPDATE member SET handle = 'gone_' || id WHERE id = ?", id);
            }
        });
        log(admin, "SAMPLE_POSTS_REMOVE", "테스트 계정 " + ids.size() + "개"); // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
        return ids.size();
    }

    private CurrentUser requireEnabledAdmin(Optional<CurrentUser> current) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        if (!enabled) {
            throw new NotFoundException();
        }
        return admin;
    }

    private long account(String handle) {
        Account account = ACCOUNTS.stream().filter(a -> a.handle().equals(handle)).findFirst()
                .orElseThrow(() -> new IllegalStateException("모르는 테스트 계정: " + handle));
        List<Long> found = jdbc.queryForList("SELECT id FROM member WHERE handle = ? AND deleted_at IS NULL", Long.class, handle);
        if (!found.isEmpty()) { // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
            return found.get(0);
        }
        Timestamp now = Timestamp.from(clock.instant());
        long id = jdbc.queryForObject("INSERT INTO member (handle, nickname, bio) VALUES (?, ?, ?) RETURNING id", Long.class, // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
                handle, account.nickname(), BIO);
        String email = handle + "@sample.invalid";
        jdbc.update("""
                INSERT INTO auth_identity (member_id, provider, provider_user_id, email, password_hash, email_verified_at)
                VALUES (?, 'LOCAL', ?, ?, ?, ?)
                """, id, email, email, passwordEncoder.encode(UUID.randomUUID().toString()), now);
        return id;
    }

    private void log(CurrentUser admin, String action, String target) {
        jdbc.update("INSERT INTO admin_log (admin_id, action, target) VALUES (?, ?, ?)", admin.memberId(), action, target);
    }

    private static String[] handles() {
        return ACCOUNTS.stream().map(Account::handle).toArray(String[]::new);
    }

    /** 파일 이름 순서. 머리 부분은 {@code author:}, {@code title:}, {@code tags:} 줄과 {@code ---}. */
    static List<SamplePost> load() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:sample-posts/*.md");
            return Arrays.stream(files).sorted(Comparator.comparing(Resource::getFilename)).map(SamplePostsService::parse).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static SamplePost parse(Resource file) {
        try {
            String text = file.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n");
            int split = text.indexOf("\n---\n");
            String author = null;
            String title = null;
            List<String> tags = List.of();
            for (String line : text.substring(0, split).split("\n")) {
                String value = line.substring(line.indexOf(':') + 1).strip();
                if (line.startsWith("author:")) {
                    author = value;
                } else if (line.startsWith("title:")) {
                    title = value;
                } else if (line.startsWith("tags:")) {
                    tags = Arrays.stream(value.split(",")).map(String::strip).filter(t -> !t.isEmpty()).toList();
                }
            }
            return new SamplePost(author, title, tags, text.substring(split + 5).strip());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
