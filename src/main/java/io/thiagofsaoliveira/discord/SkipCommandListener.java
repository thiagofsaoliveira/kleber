package io.thiagofsaoliveira.discord;

import io.thiagofsaoliveira.AudioRequest;
import io.thiagofsaoliveira.AudioRequests;
import io.thiagofsaoliveira.AudioRequestsManager;
import io.thiagofsaoliveira.Messages;
import io.thiagofsaoliveira.audio.AudioPlayer;
import io.thiagofsaoliveira.audio.AudioPlayerManager;
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

public class SkipCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(SkipCommandListener.class);

    private final AudioPlayerManager audioManager;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public SkipCommandListener(
            AudioPlayerManager audioManager,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.audioManager = audioManager;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof IReplyCallback e && isSkipCommandEvent(e)) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug("SkipCommandEvent received for guild: {}", guildId);

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

            AudioRequests requests = requestsManager.getAudioRequests(guildId);
            Optional<AudioRequest> request = requests.getCurrentRequest();
            request.ifPresent(req -> {
                String msg = messages.getMessage("SKIPPED_MSG");
                e.reply(msg).queue();
                audioPlayer.stop();
            });
        }
    }

    private boolean isSkipCommandEvent(IReplyCallback event) {
        return (event instanceof SlashCommandInteractionEvent slashInteraction
                && slashInteraction.getName().equals("skip"))
                || (event instanceof ButtonInteractionEvent buttonInteraction
                && buttonInteraction.getComponentId().equals("skip"));
    }
}
