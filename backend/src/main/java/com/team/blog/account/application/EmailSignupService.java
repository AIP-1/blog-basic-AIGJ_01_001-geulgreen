package com.team.blog.account.application;

import com.team.blog.account.application.MemberUniqueViolationTranslator.SignupContext;
import com.team.blog.account.domain.AuthIdentity;
import com.team.blog.account.domain.Handle;
import com.team.blog.account.domain.Member;
import com.team.blog.account.domain.Nickname;
import com.team.blog.account.domain.PasswordPolicy;
import com.team.blog.account.domain.PasswordPolicyViolation;
import com.team.blog.account.domain.Provider;
import com.team.blog.account.infra.AuthIdentityRepository;
import com.team.blog.account.infra.MemberRepository;
import com.team.blog.account.infra.RedisTokenStore;
import com.team.blog.account.infra.TokenKind;
import com.team.blog.shared.error.HandleTakenException;
import com.team.blog.shared.error.HandleViolationException;
import com.team.blog.shared.event.EmailVerified;
import com.team.blog.shared.event.MemberSignedUp;
import com.team.blog.shared.event.VerificationMailRequested;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이메일 가입(US1, contracts/account-service.md §2).
 *
 * <p>바깥 {@link #signUp}은 트랜잭션이 없고 안쪽 단계를 {@link TransactionTemplate}으로 실행한다. 동시 가입으로 DB UNIQUE 제약에
 * 진 쪽은 트랜잭션 경계 밖에서 번역한다(002 contract §6): {@code uq_auth_identity} → {@link DuplicateEmailException},
 * 주소·닉네임 → {@link MemberUniqueViolationTranslator}.
 */
@Service
public class EmailSignupService {

    private final MemberRepository memberRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AgreementRecorder agreementRecorder;
    private final HandleService handleService;
    private final NicknamePolicy nicknamePolicy;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final RedisTokenStore tokenStore;
    private final MemberUniqueViolationTranslator translator;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final GuardianConsentService guardianConsent;
    private final SignupProperties signupProperties;

    public EmailSignupService(MemberRepository memberRepository, AuthIdentityRepository authIdentityRepository,
                              AgreementRecorder agreementRecorder, HandleService handleService,
                              NicknamePolicy nicknamePolicy, PasswordPolicy passwordPolicy,
                              PasswordEncoder passwordEncoder, RedisTokenStore tokenStore,
                              MemberUniqueViolationTranslator translator, ApplicationEventPublisher events,
                              TransactionTemplate transactionTemplate, Clock clock, GuardianConsentService guardianConsent,
                              SignupProperties signupProperties) {
        this.signupProperties = signupProperties;
        this.guardianConsent = guardianConsent;
        this.memberRepository = memberRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.agreementRecorder = agreementRecorder;
        this.handleService = handleService;
        this.nicknamePolicy = nicknamePolicy;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.translator = translator;
        this.events = events;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * @return 새 회원 번호(미인증). 커밋 후 인증 메일이 나간다.
     * @throws AgreementRequiredException, InvalidEmailException, InvalidPasswordException, PasswordMismatchException,
     *         DuplicateEmailException, WithdrawnAccountExistsException, 002 주소·닉네임 예외
     */
    public long signUp(EmailSignupCommand command) {
        return signUp(command, false);
    }

    /** 074 강성찬 개인 확장: {@code emailVerified}면 메일 코드로 이미 확인한 이메일이라 인증된 채로 만들고 링크 메일을 보내지 않는다. */
    public long signUp(EmailSignupCommand command, boolean emailVerified) {
        Objects.requireNonNull(command);
        // 10-25 chan: 주소를 우리가 만든 경우(칸 없음)엔 같은 순간 다른 가입과 겹쳐도 사용자에게 묻지 않고 대안 주소로 다시 시도한다
        boolean derived = signupProperties.simple() && (command.handle() == null || command.handle().isBlank());
        EmailSignupCommand attempt = command;
        for (int tries = 0; ; tries++) {
            try {
                return signUpOnce(attempt, emailVerified);
            } catch (HandleTakenException e) {
                if (!derived || tries >= 2 || e.getSuggestion() == null) {
                    throw e;
                }
                attempt = attempt.withHandle(e.getSuggestion());
            } catch (HandleViolationException e) {
                if (!derived || tries >= 2 || e.getSuggestion() == null
                        || e.getCode() != com.team.blog.account.domain.HandleViolation.HANDLE_DUPLICATE) {
                    throw e;
                }
                attempt = attempt.withHandle(e.getSuggestion());
            }
        }
    }

    private long signUpOnce(EmailSignupCommand command, boolean emailVerified) {
        Handle[] submittedHandle = new Handle[1];
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> doSignUp(command, submittedHandle, emailVerified)));
        } catch (DataIntegrityViolationException e) {
            String constraint = MemberUniqueViolationTranslator.constraintName(e);
            if (MemberUniqueViolationTranslator.UQ_AUTH_IDENTITY.equals(constraint)) {
                throw new DuplicateEmailException();
            }
            translator.translate(e, SignupContext.email(submittedHandle[0]));
            throw e; // translate는 이메일 가입 문맥에서 항상 던진다
        }
    }

    private long doSignUp(EmailSignupCommand command, Handle[] submittedHandle, boolean emailVerified) {
        // 화면 순서대로 검사한다: 이메일 → 닉네임 → 주소 → 비밀번호 → 약관(약관 안내가 앞 단계에서 먼저 뜨지 않게)
        String email = EmailRules.normalizeAndValidate(command.email());
        Optional<AuthIdentity> existing = authIdentityRepository.findLocalByEmail(email);
        if (existing.isPresent()) {
            boolean withdrawing = memberRepository.findById(existing.get().getMemberId())
                    .map(Member::isWithdrawalPending).orElse(false);
            throw withdrawing ? new WithdrawnAccountExistsException() : new DuplicateEmailException();
        }
        Nickname nickname = nicknamePolicy.validate(command.nickname(), null);
        // 10-25 chan: 가입 화면에 주소 칸이 없다 — 비어 오면 이메일 앞부분으로 만든다(설정에서 바꿀 수 있음). 보낸 값이 있으면 그대로 검사
        String rawHandle = signupProperties.simple() && (command.handle() == null || command.handle().isBlank())
                ? handleService.prefill(email, Provider.LOCAL) : command.handle();
        Handle handle = handleService.validateForSignup(rawHandle, Provider.LOCAL);
        submittedHandle[0] = handle;
        List<PasswordPolicyViolation> violations = passwordPolicy.validate(command.password(), email);
        if (!violations.isEmpty()) {
            throw new InvalidPasswordException(violations);
        }
        // 비밀번호 확인 칸도 화면에서 뺐다(simple). 보내 온 경우(예전 폼·API)나 간소화를 껐을 때만 일치를 본다
        if ((!signupProperties.simple() || command.passwordConfirm() != null)
                && !Objects.equals(command.password(), command.passwordConfirm())) {
            throw new PasswordMismatchException();
        }
        agreementRecorder.requireAll(command.agreeTerms(), command.agreePrivacy(), command.ageGroup());
        if (command.under14()) {
            GuardianConsentService.problem(command.guardianName(), command.guardianEmail(), email).ifPresent(m -> {
                throw new GuardianInfoException(m);
            });
        }

        Instant now = clock.instant();
        Member member = memberRepository.saveAndFlush(new Member(handle, nickname, now));
        long memberId = member.getId();
        AuthIdentity identity = AuthIdentity.local(memberId, email, passwordEncoder.encode(command.password()), now);
        if (emailVerified) {
            identity.markVerified(now);
        }
        authIdentityRepository.saveAndFlush(identity);
        agreementRecorder.recordSignupAgreements(memberId, now, !command.under14());
        if (command.under14()) {
            // 14세 미만: 보호자가 동의해야 계정이 열린다(로그인 막힘)
            guardianConsent.request(memberId, command.guardianName(), command.guardianEmail(), nickname.value());
        }
        if (emailVerified) {
            events.publishEvent(new MemberSignedUp(memberId, Provider.LOCAL.name()));
            events.publishEvent(new EmailVerified(memberId));
            return memberId;
        }
        String rawToken = tokenStore.issue(TokenKind.VERIFY, memberId);
        events.publishEvent(new MemberSignedUp(memberId, Provider.LOCAL.name()));
        events.publishEvent(new VerificationMailRequested(memberId, email, rawToken));
        return memberId;
    }
}
