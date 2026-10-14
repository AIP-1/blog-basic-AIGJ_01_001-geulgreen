package com.team.blog.connect.web;

import com.team.blog.connect.application.McpTools;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 097 MCP 서버(Streamable HTTP, 상태 없음). 요청 하나에 JSON 응답 하나 — 세션 번호·SSE 스트림을 쓰지 않아서
 * 앱 두 대 어느 쪽이 받아도 같다. 키 확인·요청 제한은 {@link McpKeyFilter}가 먼저 한다.
 * ponytail: 일괄 요청(배열)은 받지 않는다(2025-06-18 규격에서 빠짐). 클라이언트가 보내면 그때 더한다.
 */
@RestController
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    static final List<String> PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2024-11-05");
    private static final String INSTRUCTIONS = "글그린 블로그에 연결돼 있어요. 사용자가 블로그에 글로 남겨 달라고 하면"
            + " create_draft로 나눈 이야기를 임시글로 정리해 주세요. 글은 공개되지 않고 사용자가 직접 읽고 발행해요."
            + " 비밀번호·토큰·개인 정보·회사 내부 주소는 글에 넣지 마세요. 이어 쓰거나 고칠 때는 list_drafts로 찾고 update_draft로 고쳐요.";

    private final McpTools tools;
    private final CurrentUserProvider currentUserProvider;
    private final JsonMapper jsonMapper;

    public McpController(McpTools tools, CurrentUserProvider currentUserProvider, JsonMapper jsonMapper) {
        this.tools = tools;
        this.currentUserProvider = currentUserProvider;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping(value = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handle(@RequestBody(required = false) String body) {
        JsonNode msg;
        try {
            msg = jsonMapper.readTree(body == null ? "" : body);
        } catch (JacksonException e) {
            return ResponseEntity.badRequest().body(error(null, -32700, "Parse error"));
        }
        if (msg == null || !msg.isObject() || !msg.has("method")) {
            // 응답·알 수 없는 모양: 응답(결과·오류)이면 받기만 한다
            if (msg != null && msg.isObject() && (msg.has("result") || msg.has("error"))) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.badRequest().body(error(null, -32600, "Invalid Request"));
        }
        JsonNode id = msg.get("id");
        if (id == null || id.isNull()) {
            return ResponseEntity.accepted().build(); // 알림(notifications/initialized 등)은 응답 없음
        }
        String method = msg.get("method").asString("");
        JsonNode params = msg.get("params");
        Object result = switch (method) {
            case "initialize" -> initialize(params);
            case "ping" -> Map.of();
            case "tools/list" -> Map.of("tools", McpTools.TOOLS);
            case "tools/call" -> call(params);
            default -> null;
        };
        if (result == null) {
            return ResponseEntity.ok(error(id, -32601, "Method not found"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("result", result);
        return ResponseEntity.ok(out);
    }

    /** 이 서버는 긴 SSE 스트림을 열지 않는다(규격: GET은 405로 거절할 수 있음). */
    @GetMapping("/mcp")
    public ResponseEntity<Void> noStream() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header("Allow", "POST").build();
    }

    private Map<String, Object> initialize(JsonNode params) {
        String asked = params == null || params.get("protocolVersion") == null ? "" : params.get("protocolVersion").asString("");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("protocolVersion", PROTOCOL_VERSIONS.contains(asked) ? asked : PROTOCOL_VERSIONS.get(0));
        out.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        out.put("serverInfo", Map.of("name", "geulgreen", "title", "글그린", "version", "1.0.0"));
        out.put("instructions", INSTRUCTIONS);
        return out;
    }

    /** 도구 실패는 JSON-RPC 오류가 아니라 {@code isError} 결과로 돌려준다(모델이 읽고 고칠 수 있게). */
    private Map<String, Object> call(JsonNode params) {
        String name = params == null || params.get("name") == null ? "" : params.get("name").asString("");
        JsonNode args = params == null ? null : params.get("arguments");
        try {
            Map<String, Object> data = tools.call(currentUserProvider.current(), name, args);
            return Map.of("content", List.of(text(jsonMapper.writeValueAsString(data))), "structuredContent", data,
                    "isError", false);
        } catch (RuntimeException e) {
            String code = reason(e);
            if ("INTERNAL_ERROR".equals(code)) {
                log.warn("mcp tool {} failed", name, e);
            }
            return Map.of("content", List.of(text("실패했어요: " + code)), "isError", true);
        }
    }

    /** 우리 예외는 메시지가 이유 코드다(NOT_FOUND, TITLE_TOO_LONG 등). 그 밖은 내부 오류로만 알린다. */
    static String reason(RuntimeException e) {
        String pkg = e.getClass().getPackageName();
        boolean ours = pkg.startsWith("com.team.blog.shared.error") || pkg.startsWith("com.team.blog.tag.domain")
                || e instanceof McpTools.ToolException;
        return ours && e.getMessage() != null ? e.getMessage() : "INTERNAL_ERROR";
    }

    private static Map<String, Object> text(String s) {
        return Map.of("type", "text", "text", s);
    }

    private static Map<String, Object> error(JsonNode id, int code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("error", Map.of("code", code, "message", message));
        return out;
    }
}
