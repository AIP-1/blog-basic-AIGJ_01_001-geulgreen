package com.team.blog.mission.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 10-29 방장 API 키 암호화: AES-256-GCM, 키는 서버 비밀값의 SHA-256. 저장 값은 base64(IV 12바이트 + 암호문).
 * DB만 새어 나가서는 키를 읽을 수 없다. 비밀값이 바뀌면 예전 키는 못 풀고(빈 값) 방장에게 다시 연결해 달라고 한다.
 */
@Component
public class ClubAiKeys {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV = 12;
    private final ClubAiProperties properties;

    public ClubAiKeys(ClubAiProperties properties) {
        this.properties = properties;
    }

    private SecretKeySpec key() throws GeneralSecurityException {
        byte[] k = MessageDigest.getInstance("SHA-256").digest(properties.secret().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] body = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV + body.length];
            System.arraycopy(iv, 0, out, 0, IV);
            System.arraycopy(body, 0, out, IV, body.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("키를 암호화하지 못했어요", e);
        }
    }

    public Optional<String> decrypt(String stored) {
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, IV));
            return Optional.of(new String(c.doFinal(all, IV, all.length - IV), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
