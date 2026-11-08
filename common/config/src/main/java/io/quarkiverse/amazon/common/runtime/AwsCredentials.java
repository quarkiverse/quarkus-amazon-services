package io.quarkiverse.amazon.common.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.json.JsonObject;

import io.quarkiverse.amazon.common.config.FetchFailure;

/** Shared selection, text decoding and the small credential map understood by Quarkus consumers. */
public final class AwsCredentials {
    private AwsCredentials() {
    }

    /** Resolve a fresh, complete selection; the service reader owns protocol validation and its CDI client. */
    public static Map<String, String> resolve(Optional<String> documentId, Map<String, String> rawSelection,
            ConfigSourceFormat format, String usernameKey, String passwordKey,
            Function<List<String>, Map<String, String>> reader) {
        boolean document = documentId.isPresent();
        if (document && !rawSelection.isEmpty() || !document && rawSelection.isEmpty()
                || format == ConfigSourceFormat.AUTO || passwordKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Select a credential document or individual raw values with an explicit format.");
        }
        var selection = document ? Map.of("password", documentId.orElseThrow()) : rawSelection;
        validateSelection(selection);
        if (document) {
            format.checkAvailable();
        }
        var ids = selection.values().stream().distinct().toList();
        var values = reader.apply(ids);
        if (values == null || !values.keySet().equals(Set.copyOf(ids))) {
            throw new ReadFailure(FetchFailure.PARTIAL_RESPONSE);
        }
        if (document) {
            return decode(values.get(ids.getFirst()), format, usernameKey, passwordKey);
        }
        Map<String, String> credentials = new LinkedHashMap<>();
        selection.forEach((key, id) -> credentials.put(key, values.get(id)));
        return validate(credentials);
    }

    public static String text(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("Credentials content is not valid UTF-8.");
        }
    }

    public static void validateSelection(Map<String, String> selection) {
        if (!selection.containsKey("password") || !Set.of("user", "password").containsAll(selection.keySet())
                || selection.values().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Select a password and optionally a user.");
        }
    }

    public static Map<String, String> decode(String content, ConfigSourceFormat format, String usernameKey,
            String passwordKey) {
        try {
            if (format == ConfigSourceFormat.AUTO || passwordKey.isBlank()) {
                throw new IllegalArgumentException("Credentials require an explicit format and a password key.");
            }
            Map<String, String> credentials = new LinkedHashMap<>();
            if (format == ConfigSourceFormat.RAW) {
                credentials.put("password", content);
            } else if (format == ConfigSourceFormat.JSON) {
                JsonObject object = JsonConfigFlattener.parseJson(content).asJsonObject();
                credentials.put("password", object.getString(passwordKey));
                if (usernameKey != null && object.containsKey(usernameKey)) {
                    credentials.put("user", object.getString(usernameKey));
                }
            } else {
                var properties = format.decode("", content);
                credentials.put("password", properties.get(passwordKey));
                if (usernameKey != null && properties.containsKey(usernameKey)) {
                    credentials.put("user", properties.get(usernameKey));
                }
            }
            return validate(credentials);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid credentials content.");
        }
    }

    public static Map<String, String> validate(Map<String, String> credentials) {
        if (!credentials.containsKey("password") || !Set.of("user", "password").containsAll(credentials.keySet())
                || credentials.values().stream().anyMatch(value -> value == null || value.isEmpty()
                        || !StandardCharsets.UTF_8.newEncoder().canEncode(value))) {
            throw new IllegalArgumentException("Invalid credentials content.");
        }
        return Map.copyOf(credentials);
    }

    /** Carries only a safe failure category from a service reader, never SDK diagnostics or secret content. */
    public static final class ReadFailure extends RuntimeException {
        private final FetchFailure failure;

        public ReadFailure(FetchFailure failure) {
            this.failure = failure;
        }

        public FetchFailure failure() {
            return failure;
        }
    }
}
