package io.thiagofsaoliveira;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AudioRequestsManager {

    private final ConcurrentMap<Long, AudioRequests> audioRequests =
            new ConcurrentHashMap<>();

    private AudioRequestsManager() {

    }

    public static AudioRequestsManager newAudioRequestsManager() {
        return new AudioRequestsManager();
    }

    public AudioRequests getAudioRequests(long id) {
        return audioRequests.computeIfAbsent(id, key -> new AudioRequests());
    }
}
