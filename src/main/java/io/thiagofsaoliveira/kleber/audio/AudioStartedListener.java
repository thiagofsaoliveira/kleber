package io.thiagofsaoliveira.kleber.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.thiagofsaoliveira.kleber.AudioRequest;
import io.thiagofsaoliveira.kleber.AudioRequests;
import io.thiagofsaoliveira.kleber.AudioRequestsManager;
import io.thiagofsaoliveira.kleber.Messages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AudioStartedListener implements AudioEventListener {

    private final Logger log =
            LoggerFactory.getLogger(AudioStartedListener.class);

    private static final int MAX_TEXT_LENGTH = 40;

    private final JDA jda;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public AudioStartedListener(
            JDA jda,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.jda = jda;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void handle(AudioEvent event, long id) {
        if (event instanceof TrackStartEvent) {
            log.debug("AudioStartedEvent received for guild: {}", id);

            AudioRequests requests = requestsManager.getAudioRequests(id);
            Optional<AudioRequest> currentRequest =
                    requests.getCurrentRequest();
            currentRequest.ifPresent(req -> {
                Guild guild = Objects.requireNonNull(jda.getGuildById(id));
                var builder = new EmbedBuilder();
                AudioTrack track = req.getTrack();
                AudioTrackInfo info = track.getInfo();
                appendHeader(builder, guild, info);
                appendFooter(builder, req, guild);
                Collection<ItemComponent> buttons = buildButtons();

                long textChannelId = req.getTextChannelId();
                TextChannel channel = Objects.requireNonNull(
                        guild.getTextChannelById(textChannelId));
                channel.sendMessageEmbeds(builder.build())
                        .addActionRow(buttons)
                        .queue();
            });
        }
    }

    private void appendHeader(
            EmbedBuilder builder,
            Guild guild,
            AudioTrackInfo info) {
        String title = trimTitle(info.title);
        String author = trimAuthor(info.author);
        String duration = formatDuration(info.length);
        Member self = guild.getSelfMember();
        String selfName = self.getEffectiveName();
        String selfAvatar = self.getEffectiveAvatarUrl();
        String authorLabel =
                messages.getMessage("AUTHOR_LABEL");
        String durationLabel =
                messages.getMessage("DURATION_LABEL");

        builder.setAuthor(selfName, null, selfAvatar)
                .setTitle(title)
                .setUrl(info.uri)
                .setThumbnail(info.artworkUrl)
                .addField(authorLabel, author, true)
                .addField(durationLabel, duration, true);
    }

    private String trimTitle(String title) {
        int endIndex = Math.min(title.length(), MAX_TEXT_LENGTH);
        return title.substring(0, endIndex);
    }

    private String trimAuthor(String author) {
        int endIndex = Math.min(author.length(), MAX_TEXT_LENGTH);
        return author.substring(0, endIndex);
    }

    private String formatDuration(long durationMilis) {
        Duration duration = Duration.of(durationMilis, ChronoUnit.MILLIS);
        return messages.getMessageFormatted(
                "DURATION_FORMAT",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());
    }

    private void appendFooter(
            EmbedBuilder builder,
            AudioRequest request,
            Guild guild) {
        long requesterId = request.getRequesterId();
        Member requester =
                Objects.requireNonNull(guild.getMemberById(requesterId));
        String requesterName = requester.getEffectiveName();
        String requesterAvatar = requester.getEffectiveAvatarUrl();
        String footer = messages.getMessageFormatted(
                "REQUESTER_FOOTER_FORMAT",
                requesterName);
        builder.setFooter(footer, requesterAvatar);
    }

    private Collection<ItemComponent> buildButtons() {
        return List.of(
                Button.secondary("togglepause", Emoji.fromUnicode("⏯️")),
                Button.secondary("skip", Emoji.fromUnicode("⏭️")),
                Button.secondary("stop", Emoji.fromUnicode("⏹️")));
    }
}
