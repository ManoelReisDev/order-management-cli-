package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

public final class JsonPersistenceException extends RuntimeException {

    public JsonPersistenceException(String operation, java.nio.file.Path path, Throwable cause) {
        super("Falha ao " + operation + " o arquivo JSON " + path, cause);
    }

    public JsonPersistenceException(String operation, java.nio.file.Path path) {
        super("Falha ao " + operation + " o arquivo JSON " + path);
    }
}
