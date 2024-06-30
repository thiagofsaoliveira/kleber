package io.thiagofsaoliveira.kleber.discord;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.thiagofsaoliveira.kleber.AudioRequest;
import io.thiagofsaoliveira.kleber.AudioRequests;
import io.thiagofsaoliveira.kleber.AudioRequestsManager;
import io.thiagofsaoliveira.kleber.Messages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(QueueCommandListener.class);

    private static final Pattern REGEX = Pattern.compile("\\{(\\d+)\\}");
    private static final int MAX_TEXT_LENGTH = 40;
    private static final int MAX_PAGE_SIZE = 10;

    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public QueueCommandListener(
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof IReplyCallback e && isQueueCommandEvent(e)) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug("QueueCommandEvent received for guild: {}", guildId);

            AudioRequests requests = requestsManager.getAudioRequests(guildId);
            Optional<AudioRequest> request = requests.getCurrentRequest();
            request.ifPresentOrElse(
                    req -> {
                        if (!requests.hasNextRequest()) {
                            String msg =
                                    messages.getMessage("EMPTY_QUEUE_MSG");
                            e.reply(msg).queue();
                            return;
                        }

                        var builder = new EmbedBuilder();
                        long page = resolvePageValue(e);
                        long totalPage = calculateTotalPage(requests);
                        appendHeader(builder, guild);
                        appendBody(builder, requests, page);
                        appendFooter(builder, page, totalPage);
                        Collection<ItemComponent> buttons =
                                buildButtons(page, totalPage);

                        e.replyEmbeds(builder.build())
                                .addActionRow(buttons)
                                .queue();
                    },
                    () -> {
                        String msg =
                                messages.getMessage("NO_AUDIO_PLAYING_MSG");
                        e.reply(msg).queue();
                    });
        }
    }

    private boolean isQueueCommandEvent(IReplyCallback event) {
        return (event instanceof SlashCommandInteractionEvent slash
                && slash.getName().equals("queue"))
                || (event instanceof ButtonInteractionEvent button
                && button.getComponentId().startsWith("page"));
    }

    private void appendHeader(EmbedBuilder builder, Guild guild) {
        Member self = guild.getSelfMember();
        String selfName = self.getEffectiveName();
        String selfAvatar = self.getEffectiveAvatarUrl();
        String title = messages.getMessage("UPCOMING_LABEL");

        builder.setAuthor(selfName, null, selfAvatar).setTitle(title);
    }

    private long resolvePageValue(IReplyCallback event) {
        Guild guild = Objects.requireNonNull(event.getGuild());

        if (event instanceof SlashCommandInteractionEvent e) {
            long optValue = e.getOption("page", 1L, OptionMapping::getAsLong);
            long totalPage = calculateTotalPage(guild.getIdLong());
            optValue = Math.min(optValue, totalPage);
            return Math.max(optValue, 1L);
        }

        if (event instanceof ButtonInteractionEvent e) {
            String id = e.getComponentId();
            Matcher matcher = REGEX.matcher(id);

            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "IReplyCallback#getComponentId doesn't have a valid pattern: %s"
                                .formatted(id));
            }

            String token = matcher.group(1);
            long value = Long.parseLong(token);
            long totalPage = calculateTotalPage(guild.getIdLong());
            value = Math.min(value, totalPage);
            return Math.max(value, 1L);
        }

        throw new UnsupportedOperationException();
    }

    private long calculateTotalPage(long guildId) {
        AudioRequests requests = requestsManager.getAudioRequests(guildId);
        return calculateTotalPage(requests);
    }

    private long calculateTotalPage(AudioRequests requests) {
        long totalPage = requests.nextRequestsLeft() / MAX_PAGE_SIZE;
        totalPage += requests.nextRequestsLeft() % MAX_PAGE_SIZE != 0 ? 1 : 0;
        return totalPage;
    }

    private void appendBody(
            EmbedBuilder builder,
            AudioRequests requests,
            long currentPage) {
        Collection<String> nextRequests = requests.getNextRequests().stream()
                .map(AudioRequest::getTrack)
                .map(AudioTrack::getInfo)
                .map(this::formatToAudioInfoMessage)
                .skip((currentPage - 1) * MAX_PAGE_SIZE)
                .limit(MAX_PAGE_SIZE)
                .toList();

        var counter = new AtomicInteger();
        StringBuilder sb = new StringBuilder();

        nextRequests.forEach(arg -> {
            String line = messages.getMessageFormatted(
                    "QUEUE_LINE_FORMAT",
                    counter.incrementAndGet(),
                    arg);
            sb.append(line);
        });

        int remainingCount = requests.nextRequestsLeft() - MAX_PAGE_SIZE;

        if (remainingCount > 0) {
            String remaining = messages.getMessageFormatted(
                    "REMAINING_FORMAT",
                    remainingCount);
            sb.append(remaining);
        }

        String queue = sb.toString();

        if (!queue.isBlank()) {
            builder.appendDescription(queue);
        }
    }

    private String trimTitle(String title) {
        int endIndex = Math.min(title.length(), MAX_TEXT_LENGTH);
        return title.substring(0, endIndex);
    }

    private String trimAuthor(String author) {
        int endIndex = Math.min(author.length(), MAX_TEXT_LENGTH);
        return author.substring(0, endIndex);
    }

    private String formatToAudioInfoMessage(AudioTrackInfo info) {
        String title = trimTitle(info.title);
        String author = trimAuthor(info.author);
        return messages.getMessageFormatted(
                "AUDIO_INFO_FORMAT",
                title,
                author);
    }

    private void appendFooter(
            EmbedBuilder builder,
            long currentPage,
            long totalPage) {
        String footer = messages.getMessageFormatted(
                "PAGE_FOOTER_FORMAT",
                currentPage,
                totalPage);
        builder.setFooter(footer);
    }

    private Collection<ItemComponent> buildButtons(
            long currentPage,
            long totalPage) {
        long previousBtnValue = currentPage - 1;
        Button previous = Button.secondary(
                "page previous{%d}".formatted(previousBtnValue),
                Emoji.fromUnicode("⏮️"));

        if (previousBtnValue == 0) {
            previous = previous.asDisabled();
        }

        long nextBtnValue = currentPage + 1;
        Button next = Button.secondary(
                "page next{%d}".formatted(nextBtnValue),
                Emoji.fromUnicode("⏭️"));

        if (nextBtnValue > totalPage) {
            next = next.asDisabled();
        }

        return List.of(previous, next);
    }
}
