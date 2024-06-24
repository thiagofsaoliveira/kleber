package io.thiagofsaoliveira.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import io.thiagofsaoliveira.AudioRequests;
import io.thiagofsaoliveira.AudioRequestsManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AudioStoppedListener implements AudioEventListener {

    private final Logger log =
            LoggerFactory.getLogger(AudioStoppedListener.class);

    private final JDA jda;
    private final AudioRequestsManager requestsManager;

    public AudioStoppedListener(
            JDA jda,
            AudioRequestsManager requestsManager) {
        this.jda = jda;
        this.requestsManager = requestsManager;
    }

    @Override
    public void handle(AudioEvent event, long id) {
        if (event instanceof TrackEndEvent) {
            log.debug("AudioStoppedEvent received for guild: {}", id);

            AudioRequests requests = requestsManager.getAudioRequests(id);
            Guild guild = Objects.requireNonNull(jda.getGuildById(id));
            requests.clear();
            AudioManager guildAudioManager = guild.getAudioManager();
            guildAudioManager.closeAudioConnection();
        }
    }
}
