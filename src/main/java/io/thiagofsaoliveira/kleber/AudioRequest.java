package io.thiagofsaoliveira.kleber;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class AudioRequest {

    private final long requesterId;
    private final long textChannelId;
    private final AudioTrack track;

    private String interactionToken;

    public AudioRequest(
            long requesterId,
            long textChannelId,
            AudioTrack track) {
        this.requesterId = requesterId;
        this.textChannelId = textChannelId;
        this.track = track;
    }

    public String getInteractionToken() {
        return interactionToken;
    }

    public void setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
    }

    public long getRequesterId() {
        return requesterId;
    }

    public long getTextChannelId() {
        return textChannelId;
    }

    public AudioTrack getTrack() {
        return track;
    }
}
