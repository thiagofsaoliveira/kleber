package io.thiagofsaoliveira.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AudioPlayerManager {

    private final com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager lavaplayerManager;
    private final ConcurrentMap<Long, AudioPlayer> audioPlayers =
            new ConcurrentHashMap<>();
    private final Collection<AudioEventListener> listeners = new ArrayList<>();

    public AudioPlayerManager(
            com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager lavaplayerManager) {
        this.lavaplayerManager = lavaplayerManager;
    }

    public static AudioPlayerManager newAudioPlayerManager() {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager(
                true,
                new MusicWithThumbnail(),
                new WebWithThumbnail(),
                new AndroidWithThumbnail()));
        return new AudioPlayerManager(manager);
    }

    public boolean containsAudioPlayer(long id) {
        return audioPlayers.containsKey(id);
    }

    public AudioPlayer getAudioPlayer(long id) {
        return audioPlayers.computeIfAbsent(id, key -> {
            com.sedmelluq.discord.lavaplayer.player.AudioPlayer lavaplayer =
                    lavaplayerManager.createPlayer();
            var adapter = new AudioConnectionAdapter(lavaplayer);
            var audioPlayer = new AudioPlayer(id, lavaplayer, adapter);
            lavaplayer.addListener(audioPlayer);
            audioPlayer.addListeners(listeners);
            return audioPlayer;
        });
    }

    public void addEventListeners(
            Collection<AudioEventListener> audioListeners) {
        listeners.addAll(audioListeners);
    }

    public void loadItem(
            Object orderingKey,
            String query,
            AudioLoadResultHandler handler) {
        String identifier = parseIdentifier(query);
        lavaplayerManager.loadItemOrdered(orderingKey, identifier, handler);
    }

    private String parseIdentifier(String identifier) {
        try {
            URI.create(identifier).toURL();
            return identifier;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return "ytsearch:" + identifier;
        }
    }
}
