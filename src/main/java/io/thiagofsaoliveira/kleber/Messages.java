package io.thiagofsaoliveira.kleber;

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

    public String getMessageFormatted(String key, Object... args) {
        return messages().getString(key).formatted(args);
    }

    private ResourceBundle messages() {
        return ResourceBundle.getBundle("messages");
    }
}
