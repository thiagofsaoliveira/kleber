package io.thiagofsaoliveira.kleber.discord;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.thiagofsaoliveira.kleber.AudioRequest;
import io.thiagofsaoliveira.kleber.AudioRequests;
import io.thiagofsaoliveira.kleber.AudioRequestsManager;
import io.thiagofsaoliveira.kleber.Messages;
import io.thiagofsaoliveira.kleber.audio.AudioPlayer;
import io.thiagofsaoliveira.kleber.audio.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class PlayCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(PlayCommandListener.class);

    private static final int MAX_TEXT_LENGTH = 40;

    private final AudioPlayerManager audioManager;
    private final AudioRequestsManager requestsManager;
    private final Messages messages;

    public PlayCommandListener(
            AudioPlayerManager audioManager,
            AudioRequestsManager requestsManager,
            Messages messages) {
        this.audioManager = audioManager;
        this.requestsManager = requestsManager;
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent e
                && e.getName().equals("play")) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug("PlayCommandEvent received for guild: {}", guildId);

            Member member = Objects.requireNonNull(e.getMember());
            GuildVoiceState voiceState =
                    Objects.requireNonNull(member.getVoiceState());

            if (!voiceState.inAudioChannel()) {
                String message = messages.getMessage("NOT_CONNECTED_MSG");
                e.reply(message).queue();
                return;
            }

            OptionMapping option = getOptionMapping(e);
            String query = option.getAsString();
            audioManager.loadItem(guildId, query, handler(e));
        }
    }

    private OptionMapping getOptionMapping(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("query");

        if (option == null) {
            throw new IllegalStateException(
                    "Command option is not provided: query");
        }

        return option;
    }

    private AudioLoadResultHandler handler(SlashCommandInteractionEvent e) {
        return new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                Member member = Objects.requireNonNull(e.getMember());
                Guild guild = Objects.requireNonNull(e.getGuild());
                long memberId = member.getIdLong();
                long textChannelId = e.getChannelIdLong();
                long guildId = guild.getIdLong();
                var request = new AudioRequest(memberId, textChannelId, track);
                AudioRequests requests =
                        requestsManager.getAudioRequests(guildId);
                requests.addRequest(request);

                String msg = formatToLoadedMessage(track.getInfo());
                e.reply(msg).queue(hook -> callback(e));
            }

            private void callback(
                    SlashCommandInteractionEvent e) {
                Guild guild = Objects.requireNonNull(e.getGuild());
                long guildId = guild.getIdLong();
                AudioPlayer audioPlayer = audioManager.getAudioPlayer(guildId);

                AudioRequests requests =
                        requestsManager.getAudioRequests(guildId);

                if (!audioPlayer.isPlaying()) {
                    Member member = Objects.requireNonNull(e.getMember());
                    GuildVoiceState voiceState =
                            Objects.requireNonNull(member.getVoiceState());
                    AudioChannelUnion union =
                            Objects.requireNonNull(voiceState.getChannel());
                    VoiceChannel channel = union.asVoiceChannel();
                    AudioManager guildAudioManager = guild.getAudioManager();

                    if (!guildAudioManager.isConnected()) {
                        guildAudioManager.openAudioConnection(channel);
                    }

                    Optional<AudioRequest> request =
                            requests.getCurrentRequest();
                    request.map(AudioRequest::getTrack)
                            .ifPresent(audioPlayer::playTrack);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getTracks().getFirst();
                trackLoaded(track);
            }

            @Override
            public void noMatches() {
                String msg = messages.getMessage("NO_MATCHES_MSG");
                e.reply(msg).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log.warn("Load failed: ", exception);
                String msg = messages.getMessage("LOAD_FAILED_MSG");
                e.reply(msg).queue();
            }
        };
    }

    private String formatToLoadedMessage(AudioTrackInfo info) {
        String title = trimTitle(info.title);
        String author = trimAuthor(info.author);
        return messages.getMessageFormatted("LOADED_FORMAT", title, author);
    }

    private String trimTitle(String title) {
        int endIndex = Math.min(title.length(), MAX_TEXT_LENGTH);
        return title.substring(0, endIndex);
    }

    private String trimAuthor(String author) {
        int endIndex = Math.min(author.length(), MAX_TEXT_LENGTH);
        return author.substring(0, endIndex);
    }
}
