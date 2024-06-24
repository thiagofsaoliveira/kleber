package io.thiagofsaoliveira;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;

public class AudioRequests {

    private final Deque<AudioRequest> requests = new ArrayDeque<>();

    public boolean hasNextRequest() {
        return requests.size() > 1;
    }

    public Optional<AudioRequest> getCurrentRequest() {
        return Optional.ofNullable(requests.peekFirst());
    }

    public Collection<AudioRequest> getNextRequests() {
        return requests.stream().skip(1).toList();
    }

    public int nextRequestsLeft() {
        return Math.max(requests.size() - 1, 0);
    }

    public void skipCurrentRequest() {
        if (!requests.isEmpty()) {
            requests.removeFirst();
        }
    }

    public void addRequest(AudioRequest request) {
        requests.addLast(request);
    }

    public void clear() {
        requests.clear();
    }
}
