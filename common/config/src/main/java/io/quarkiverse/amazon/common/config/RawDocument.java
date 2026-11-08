package io.quarkiverse.amazon.common.config;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Bytes are preserved by the provider; text decoding belongs to publication. */
public record RawDocument(ResourceId id, byte[] content, String contentType, String version) {
    public RawDocument {
        Objects.requireNonNull(id);
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public static RawDocument text(ResourceId id, String content, String contentType, String version) {
        try {
            var encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(content));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return new RawDocument(id, bytes, contentType, version);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Configuration text cannot be encoded as UTF-8.");
        }
    }
}
