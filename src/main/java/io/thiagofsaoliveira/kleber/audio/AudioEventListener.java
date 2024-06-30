package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;

public interface AudioEventListener {

    void handle(AudioEvent event, long id);
}
