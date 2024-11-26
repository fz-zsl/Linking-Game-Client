package org.example.client;

public class GameEndException extends RuntimeException {
    public GameEndException(String message) {
        super(message);
    }
}
