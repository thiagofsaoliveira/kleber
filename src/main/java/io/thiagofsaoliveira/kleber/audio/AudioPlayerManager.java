package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import io.thiagofsaoliveira.kleber.spotify.AudioTrackData;
import io.thiagofsaoliveira.kleber.spotify.SpotifyException;
import io.thiagofsaoliveira.kleber.spotify.SpotifyManager;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AudioPlayerManager {

    private static final Pattern SPOTIFY_URL_PATTERN = Pattern.compile(
            "https://open\\.spotify\\.com/(intl-[a-zA-Z]{2}/)?track/(.*)");

    private final com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager lavaplayerManager;
    private final SpotifyManager spotifyManager;
    private final ConcurrentMap<Long, AudioPlayer> audioPlayers =
            new ConcurrentHashMap<>();
    private final Collection<AudioEventListener> listeners = new ArrayList<>();

    public AudioPlayerManager(
            com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager lavaplayerManager,
            SpotifyManager spotifyManager) {
        this.lavaplayerManager = lavaplayerManager;
        this.spotifyManager = spotifyManager;
    }

    public static AudioPlayerManager newAudioPlayerManager(
            SpotifyManager spotifyManager) {
        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new YoutubeAudioSourceManager(
                true,
                new MusicWithThumbnail(),
                new WebWithThumbnail(),
                new AndroidWithThumbnail()));
        return new AudioPlayerManager(manager, spotifyManager);
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
        try {
            String identifier = parseIdentifier(query);
            lavaplayerManager.loadItemOrdered(orderingKey, identifier, handler);
        } catch (SpotifyException e) {
            handler.loadFailed(new FriendlyException(
                    "Error parsing identifier: " + query,
                    FriendlyException.Severity.COMMON,
                    e));
        }
    }

    private String parseIdentifier(String identifier) {
        try {
            URI.create(identifier).toURL();
            Matcher matcher = SPOTIFY_URL_PATTERN.matcher(identifier);

            if (matcher.matches()) {
                String id = matcher.group(2);
                AudioTrackData data = spotifyManager.getDataFor(id);
                return "ytsearch:%s %s lyrics".formatted(
                        data.track(),
                        data.author());
            }

            return identifier;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return "ytsearch:" + identifier;
        }
    }
}
