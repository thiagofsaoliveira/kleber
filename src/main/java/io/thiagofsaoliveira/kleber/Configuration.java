package io.thiagofsaoliveira.kleber;

import io.github.cdimascio.dotenv.Dotenv;

public class Configuration {

    private final Dotenv dotenv;

    private Configuration(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    public static Configuration newConfiguration() {
        Dotenv dotenv = Dotenv.load();
        return new Configuration(dotenv);
    }

    public String getToken() {
        return dotenv.get("TOKEN");
    }

    public String getActivity() {
        return dotenv.get("ACTIVITY");
    }

    public String getLogLevel() {
        return dotenv.get("LOG_LEVEL");
    }

    public String getClientId() {
        return dotenv.get("CLIENT_ID");
    }

    public String getClientSecret() {
        return dotenv.get("CLIENT_SECRET");
    }
}
