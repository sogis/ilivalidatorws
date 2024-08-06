package ch.so.agi.ilivalidator.storage;

public class LocalFileStorageException extends RuntimeException {
    public LocalFileStorageException(String message) {
        super(message);
    }

    public LocalFileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
