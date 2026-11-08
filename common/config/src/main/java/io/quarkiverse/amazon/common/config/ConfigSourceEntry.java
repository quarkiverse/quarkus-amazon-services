package io.quarkiverse.amazon.common.config;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;

/** A remote resource can be decoded independently by several configured entries. */
public record ConfigSourceEntry(String name, ResourceId resource, String prefix, ConfigSourceFormat format,
        boolean document) {
    public ConfigSourceEntry {
        Objects.requireNonNull(name);
        Objects.requireNonNull(resource);
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(format);
    }

    public Map<String, String> decode(RawDocument raw) {
        if (!resource.equals(raw.id())) {
            throw new IllegalArgumentException("The entry and document refer to different resources.");
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(raw.content())).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Configuration content is not valid UTF-8.");
        }
        return document && format == ConfigSourceFormat.AUTO ? decodeDocument(prefix, text, raw.contentType())
                : format.decode(prefix, text);
    }

    public static Map<String, String> decodeDocument(String prefix, String content, String contentType) {
        String trimmed = content.trim();
        String mime = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean yaml = mime.equals("application/yaml") || mime.equals("application/x-yaml")
                || mime.equals("text/yaml") || mime.equals("text/x-yaml") || mime.endsWith("+yaml");
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return ConfigSourceFormat.JSON.decode(prefix, trimmed);
            } catch (IllegalArgumentException e) {
                if (!yaml) {
                    throw e;
                }
            }
        }
        return (yaml ? ConfigSourceFormat.YAML : ConfigSourceFormat.PROPERTIES).decode(prefix, content);
    }
}
