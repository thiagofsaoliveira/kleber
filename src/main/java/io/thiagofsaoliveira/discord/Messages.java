package io.thiagofsaoliveira.discord;

import java.util.ResourceBundle;

public class Messages {

    private Messages() {

    }

    public static Messages newMessages() {
        return new Messages();
    }

    public String getMessage(String key) {
        return messages().getString(key);
    }

    private ResourceBundle messages() {
        return ResourceBundle.getBundle("messages");
    }
}
