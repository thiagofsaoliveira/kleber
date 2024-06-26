package io.thiagofsaoliveira.kleber.discord;

import io.thiagofsaoliveira.kleber.AudioRequest;
import io.thiagofsaoliveira.kleber.AudioRequests;
import io.thiagofsaoliveira.kleber.AudioRequestsManager;
import io.thiagofsaoliveira.kleber.Messages;
import io.thiagofsaoliveira.kleber.audio.AudioPlayer;
import io.thiagofsaoliveira.kleber.audio.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class TogglePauseCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(TogglePauseCommandListener.class);

    private final AudioPlayerManager audioManager;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public TogglePauseCommandListener(
            AudioPlayerManager audioManager,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.audioManager = audioManager;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof IReplyCallback e && isPauseCommandEvent(e)) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug(
                    "TogglePauseCommandEvent received for guild: {}",
                    guildId);

            Member member = Objects.requireNonNull(e.getMember());
            GuildVoiceState voiceState =
                    Objects.requireNonNull(member.getVoiceState());

            if (!voiceState.inAudioChannel()) {
                String msg = messages.getMessage("NOT_CONNECTED_MSG");
                e.reply(msg).queue();
                return;
            }

            AudioPlayer audioPlayer = audioManager.getAudioPlayer(guildId);

            if (!audioPlayer.isPlaying()) {
                String msg = messages.getMessage("NO_AUDIO_PLAYING_MSG");
                e.reply(msg).queue();
                return;
            }

            e.deferReply().queue();

            AudioRequests requests = requestsManager.getAudioRequests(guildId);
            Optional<AudioRequest> request = requests.getCurrentRequest();
            request.ifPresent(req -> {
                req.setInteractionToken(e.getToken());
                audioPlayer.pause();
            });
        }
    }

    private boolean isPauseCommandEvent(IReplyCallback event) {
        return (event instanceof SlashCommandInteractionEvent slashInteraction
                && slashInteraction.getName().equals("togglepause"))
                || (event instanceof ButtonInteractionEvent buttonInteraction
                && buttonInteraction.getComponentId().equals("togglepause"));
    }
}
