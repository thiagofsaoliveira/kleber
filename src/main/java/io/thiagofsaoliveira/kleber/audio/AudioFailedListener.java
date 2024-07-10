package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioFailedListener implements AudioEventListener {

    private final Logger log =
            LoggerFactory.getLogger(AudioFailedListener.class);

    @Override
    public void handle(AudioEvent event, long id) {
        if (event instanceof TrackExceptionEvent e) {
            log.warn("Audio failed: ", e.exception);
        }
    }
}
