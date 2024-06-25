package io.thiagofsaoliveira.spotify;

public class SpotifyException extends RuntimeException {

    public SpotifyException(Throwable e) {
        super(e);
    }

    public SpotifyException(String msg) {
        super(msg);
    }
}
