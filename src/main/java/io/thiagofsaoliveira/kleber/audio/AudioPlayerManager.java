package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import io.thiagofsaoliveira.kleber.spotify.AudioTrackData;
import io.thiagofsaoliveira.kleber.spotify.SpotifyException;
import io.thiagofsaoliveira.kleber.spotify.SpotifyManager;

import java.io.UncheckedIOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AudioPlayerManager {

    private static final Pattern SPOTIFY_URL_PATTERN = Pattern.compile(
            "https://open\\.spotify\\.com/(intl-[a-zA-Z]{2}/)?track/(.*)");
    private static final String SUBNET = "::/48";

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
        var youtubeSource = new YoutubeAudioSourceManager(
                true,
                new MusicWithThumbnail(),
                new WebWithThumbnail(),
                new AndroidWithThumbnail());
        String cidr = getIpv6Address() + SUBNET;
        var routePlanner = new RotatingIpRoutePlanner(
                Collections.singletonList(new Ipv6Block(cidr)));
        HttpInterfaceManager configurable =
                youtubeSource.getHttpInterfaceManager();
        new YoutubeIpRotatorSetup(routePlanner)
                .forConfiguration(configurable, false)
                .withMainDelegateFilter(null)
                .setup();

        var manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(youtubeSource);
        return new AudioPlayerManager(manager, spotifyManager);
    }

    private static String getIpv6Address() {
        try {
            Optional<String> address = NetworkInterface.networkInterfaces()
                    .flatMap(NetworkInterface::inetAddresses)
                    .filter(Inet6Address.class::isInstance)
                    .map(InetAddress::getHostAddress)
                    .findFirst();

            return address.orElseThrow(
                    () -> new IllegalStateException("No ipv6 block available"));
        } catch (SocketException e) {
            throw new UncheckedIOException(e);
        }
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
