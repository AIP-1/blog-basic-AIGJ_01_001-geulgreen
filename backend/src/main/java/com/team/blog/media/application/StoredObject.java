package com.team.blog.media.application;

import java.util.Arrays;
import java.util.Objects;

/**
 * 저장소에 올라간 파일의 확인 결과.
 *
 * @param size        실제 크기(바이트)
 * @param contentType 저장소가 기록한 Content-Type
 * @param head        앞부분 바이트(최대 {@link #HEAD_BYTES})
 */
public record StoredObject(long size, String contentType, byte[] head) {

    public static final int HEAD_BYTES = 64 * 1024;

    @Override
    public boolean equals(Object o) {
        return o instanceof StoredObject s && size == s.size && Objects.equals(contentType, s.contentType)
                && Arrays.equals(head, s.head);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, contentType, Arrays.hashCode(head));
    }

    @Override
    public String toString() {
        return "StoredObject[size=" + size + ", contentType=" + contentType + ", head=" + head.length + " bytes]";
    }
}
