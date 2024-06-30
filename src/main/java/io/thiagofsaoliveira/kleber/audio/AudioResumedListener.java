package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent;
import io.thiagofsaoliveira.kleber.AudioRequest;
import io.thiagofsaoliveira.kleber.AudioRequests;
import io.thiagofsaoliveira.kleber.AudioRequestsManager;
import io.thiagofsaoliveira.kleber.Messages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AudioResumedListener implements AudioEventListener {

    private final Logger log =
            LoggerFactory.getLogger(AudioResumedListener.class);

    private final JDA jda;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public AudioResumedListener(
            JDA jda,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.jda = jda;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void handle(AudioEvent event, long id) {
        if (event instanceof PlayerResumeEvent) {
            log.debug("AudioResumedEvent received for guild: {}", id);

            AudioRequests requests = requestsManager.getAudioRequests(id);
            Optional<AudioRequest> request = requests.getCurrentRequest();
            request.map(AudioRequest::getInteractionToken)
                    .ifPresent(token -> {
                        String msg = messages.getMessage("RESUMED_MSG");
                        InteractionHook hook = InteractionHook.from(jda, token);
                        hook.sendMessage(msg).queue();
                    });
        }
    }
}
