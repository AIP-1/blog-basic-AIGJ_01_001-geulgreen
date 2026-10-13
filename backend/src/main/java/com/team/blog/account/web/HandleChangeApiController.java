package com.team.blog.account.web;

import com.team.blog.account.application.HandleChangeService;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.KoreanDateFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 074 강성찬 개인 확장: 설정에서 블로그 주소(아이디) 바꾸기. 규칙 위반은 공통 처리기가 400 + 대안 주소로 답한다. */
@RestController
public class HandleChangeApiController {

    private final HandleChangeService handleChangeService;
    private final CurrentUserProvider currentUserProvider;
    private final AccountGuard accountGuard;

    public HandleChangeApiController(HandleChangeService handleChangeService, CurrentUserProvider currentUserProvider,
                                     AccountGuard accountGuard) {
        this.handleChangeService = handleChangeService;
        this.currentUserProvider = currentUserProvider;
        this.accountGuard = accountGuard;
    }

    public record HandleRequest(String handle) {
    }

    public record HandleResponse(String handle, String message) {
    }

    @PutMapping("/api/me/handle")
    public ResponseEntity<HandleResponse> change(@RequestBody(required = false) HandleRequest body) {
        long memberId = accountGuard.requireWritable(currentUserProvider.current()).memberId();
        try {
            String handle = handleChangeService.change(memberId, body == null ? "" : body.handle());
            return ResponseEntity.ok(new HandleResponse(handle, "블로그 주소를 바꿨어요"));
        } catch (HandleChangeService.TooSoonException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new HandleResponse(null,
                    "주소는 14일에 두 번까지 바꿀 수 있어요. 다음 변경 가능일: " + KoreanDateFormatter.monthDay(e.nextAllowedAt())));
        }
    }
}
