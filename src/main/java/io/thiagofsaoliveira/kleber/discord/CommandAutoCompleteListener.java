package io.thiagofsaoliveira.kleber.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.thiagofsaoliveira.kleber.Messages;
import io.thiagofsaoliveira.kleber.audio.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CommandAutoCompleteListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(CommandAutoCompleteListener.class);

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_CHOICES_SIZE = 10;
    private static final int MAX_TEXT_LENGTH = 40;

    private final AudioPlayerManager audioManager;
    private final Messages messages;

    public CommandAutoCompleteListener(
            AudioPlayerManager audioManager,
            Messages messages) {
        this.audioManager = audioManager;
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof CommandAutoCompleteInteractionEvent e) {
            AutoCompleteQuery option = e.getFocusedOption();
            String query = option.getValue();

            if (query.isBlank()
                    || query.length() < MIN_QUERY_LENGTH
                    || isUrl(query)) {
                e.replyChoices(List.of()).queue();
                return;
            }

            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();

            audioManager.loadItem(
                    guildId,
                    query,
                    new AudioLoadResultHandler() {

                        @Override
                        public void trackLoaded(AudioTrack track) {
                            AudioTrackInfo info = track.getInfo();
                            String title = trimTitle(info.title);
                            String author = trimAuthor(info.author);
                            String duration = formatDuration(info.length);
                            String choiceName = messages.getMessageFormatted(
                                    "CHOICE_FORMAT",
                                    title,
                                    author,
                                    duration);
                            e.replyChoice(choiceName, info.uri).queue();
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            Collection<Command.Choice> choices =
                                    playlist.getTracks().stream()
                                            .map(AudioTrack::getInfo)
                                            .map(info -> formatToChoice(info))
                                            .limit(MAX_CHOICES_SIZE)
                                            .toList();
                            e.replyChoices(choices).queue();
                        }

                        @Override
                        public void noMatches() {
                            e.replyChoices(List.of()).queue();
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            log.debug("Load failed: ", exception);
                            e.replyChoices(List.of()).queue();
                        }
                    });
        }
    }

    private boolean isUrl(String input) {
        try {
            URI.create(input).toURL();
            return true;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return false;
        }
    }

    private Command.Choice formatToChoice(AudioTrackInfo info) {
        String title = trimTitle(info.title);
        String author = trimAuthor(info.author);
        String duration = formatDuration(info.length);
        String choiceName = messages.getMessageFormatted(
                "CHOICE_FORMAT",
                title,
                author,
                duration);
        return new Command.Choice(choiceName, info.uri);
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
}
