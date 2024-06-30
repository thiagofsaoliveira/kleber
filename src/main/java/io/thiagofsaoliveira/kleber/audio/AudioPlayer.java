package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.Collection;

public class AudioPlayer implements
        com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener {

    private final long id;
    private final com.sedmelluq.discord.lavaplayer.player.AudioPlayer lavaplayer;
    private final AudioConnectionAdapter audioConnectionAdapter;
    private final Collection<AudioEventListener> listeners = new ArrayList<>();

    public AudioPlayer(
            long id,
            com.sedmelluq.discord.lavaplayer.player.AudioPlayer lavaplayer,
            AudioConnectionAdapter audioConnectionAdapter) {
        this.id = id;
        this.lavaplayer = lavaplayer;
        this.audioConnectionAdapter = audioConnectionAdapter;
    }

    public boolean isPlaying() {
        return lavaplayer.getPlayingTrack() != null;
    }

    public AudioConnectionAdapter getAudioConnectionAdapter() {
        return audioConnectionAdapter;
    }

    public void playTrack(AudioTrack audioTrack) {
        lavaplayer.startTrack(audioTrack, false);
    }

    public void pause() {
        if (isPlaying()) {
            lavaplayer.setPaused(!lavaplayer.isPaused());
        }
    }

    public void stop() {
        lavaplayer.stopTrack();
    }

    public void addListeners(Collection<AudioEventListener> listeners) {
        this.listeners.addAll(listeners);
    }

    @Override
    public void onEvent(AudioEvent event) {
        listeners.forEach(listener -> listener.handle(event, id));
    }
}
