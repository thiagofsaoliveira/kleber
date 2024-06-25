package io.thiagofsaoliveira.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent;
import io.thiagofsaoliveira.AudioRequest;
import io.thiagofsaoliveira.AudioRequests;
import io.thiagofsaoliveira.AudioRequestsManager;
import io.thiagofsaoliveira.Messages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AudioPausedListener implements AudioEventListener {

    private final Logger log =
            LoggerFactory.getLogger(AudioPausedListener.class);

    private final JDA jda;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public AudioPausedListener(
            JDA jda,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.jda = jda;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void handle(AudioEvent event, long id) {
        if (event instanceof PlayerPauseEvent) {
            log.debug("AudioPausedEvent received for guild: {}", id);

            AudioRequests requests = requestsManager.getAudioRequests(id);
            Optional<AudioRequest> request = requests.getCurrentRequest();
            request.map(AudioRequest::getInteractionToken)
                    .ifPresent(token -> {
                        String msg = messages.getMessage("PAUSED_MSG");
                        InteractionHook hook = InteractionHook.from(jda, token);
                        hook.sendMessage(msg).queue();
                    });
        }
    }
}
