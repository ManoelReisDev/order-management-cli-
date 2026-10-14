package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

final class JsonFileStore {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .build();

    <T> List<T> read(Path path, TypeReference<List<T>> type) {
        if (Files.notExists(path)) {
            return List.of();
        }
        try {
            if (!Files.isRegularFile(path)) {
                throw new IOException("path is not a regular file");
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new IOException("JSON document is empty");
            }
            List<T> values = mapper.readValue(content, type);
            if (values == null) {
                throw new IOException("JSON root must be an array");
            }
            return values;
        } catch (JsonPersistenceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonPersistenceException("ler", path, exception);
        }
    }

    void writeAtomic(Path path, Object value) {
        Path parent = path.toAbsolutePath().getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("atomic move is not supported", exception);
            }
            temporary = null;
        } catch (JsonPersistenceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JsonPersistenceException("gravar", path, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Preserve the original persistence failure.
                }
            }
        }
    }
}
