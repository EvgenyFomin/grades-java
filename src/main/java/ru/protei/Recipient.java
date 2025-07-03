package ru.protei;

public class Recipient {
    private final String token;
    public Recipient(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void send(String message) {
        System.out.println("The message '" + message + "' has been sent. Token: " + token);
    }
}
