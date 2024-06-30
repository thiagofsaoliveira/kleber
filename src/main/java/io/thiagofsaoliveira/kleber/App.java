package io.thiagofsaoliveira.kleber;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.thiagofsaoliveira.kleber.audio.AudioEventListener;
import io.thiagofsaoliveira.kleber.audio.AudioPausedListener;
import io.thiagofsaoliveira.kleber.audio.AudioPlayerManager;
import io.thiagofsaoliveira.kleber.audio.AudioResumedListener;
import io.thiagofsaoliveira.kleber.audio.AudioStartedListener;
import io.thiagofsaoliveira.kleber.audio.AudioStoppedListener;
import io.thiagofsaoliveira.kleber.discord.CommandAutoCompleteListener;
import io.thiagofsaoliveira.kleber.discord.GuildJoinListener;
import io.thiagofsaoliveira.kleber.discord.HelloCommandListener;
import io.thiagofsaoliveira.kleber.discord.PingCommandListener;
import io.thiagofsaoliveira.kleber.discord.PlayCommandListener;
import io.thiagofsaoliveira.kleber.discord.QueueCommandListener;
import io.thiagofsaoliveira.kleber.discord.ReadyListener;
import io.thiagofsaoliveira.kleber.discord.SkipCommandListener;
import io.thiagofsaoliveira.kleber.discord.StopCommandListener;
import io.thiagofsaoliveira.kleber.discord.TogglePauseCommandListener;
import io.thiagofsaoliveira.kleber.spotify.SpotifyManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class App {

    public static void main(String[] args) {
        var config = Configuration.newConfiguration();

        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger log = context.getLogger("root");
        String logLevel = config.getLogLevel().toUpperCase();
        log.setLevel(Level.toLevel(logLevel));

        var messages = Messages.newMessages();
        var spotifyManager = SpotifyManager.newSpotifyManager(config);
        var audioManager =
                AudioPlayerManager.newAudioPlayerManager(spotifyManager);
        var requestsManager = AudioRequestsManager.newAudioRequestsManager();

        Object[] discordListeners = {
                new ReadyListener(audioManager),
                new GuildJoinListener(audioManager),
                new PingCommandListener(),
                new HelloCommandListener(messages),
                new CommandAutoCompleteListener(audioManager, messages),
                new PlayCommandListener(
                        audioManager,
                        requestsManager,
                        messages),
                new TogglePauseCommandListener(
                        audioManager,
                        requestsManager,
                        messages),
                new SkipCommandListener(
                        audioManager,
                        requestsManager,
                        messages),
                new StopCommandListener(
                        audioManager,
                        requestsManager,
                        messages),
                new QueueCommandListener(requestsManager, messages)
        };

        JDA jda = JDABuilder.createDefault(config.getToken())
                .setActivity(Activity.watching(config.getActivity()))
                .addEventListeners(discordListeners)
                .build();

        String pingDescription = messages.getMessage("PING_DESCRIPTION_MSG");
        String helloDescription = messages.getMessage("HELLO_DESCRIPTION_MSG");
        String playDescription = messages.getMessage("PLAY_DESCRIPTION_MSG");
        String queryDescription = messages.getMessage("QUERY_DESCRIPTION_MSG");
        String togglePauseDescription =
                messages.getMessage("TOGGLE_PAUSE_DESCRIPTION_MSG");
        String skipDescription = messages.getMessage("SKIP_DESCRIPTION_MSG");
        String stopDescription = messages.getMessage("STOP_DESCRIPTION_MSG");
        String queueDescription = messages.getMessage("QUEUE_DESCRIPTION_MSG");
        String pageDescription = messages.getMessage("PAGE_DESCRIPTION_MSG");

        Collection<SlashCommandData> commands = List.of(
                Commands.slash("ping", pingDescription).setGuildOnly(true),
                Commands.slash("hello", helloDescription).setGuildOnly(true),
                Commands.slash("play", playDescription)
                        .addOption(
                                OptionType.STRING,
                                "query",
                                queryDescription,
                                true,
                                true)
                        .setGuildOnly(true),
                Commands.slash("togglepause", togglePauseDescription)
                        .setGuildOnly(true),
                Commands.slash("skip", skipDescription).setGuildOnly(true),
                Commands.slash("stop", stopDescription).setGuildOnly(true),
                Commands.slash("queue", queueDescription)
                        .addOption(
                                OptionType.INTEGER,
                                "page",
                                pageDescription,
                                false)
                        .setGuildOnly(true)
        );

        jda.updateCommands().addCommands(commands).queue();

        Collection<AudioEventListener> audioListeners = List.of(
                new AudioStartedListener(jda, requestsManager, messages),
                new AudioStoppedListener(jda, requestsManager, audioManager),
                new AudioPausedListener(jda, requestsManager, messages),
                new AudioResumedListener(jda, requestsManager, messages));

        audioManager.addEventListeners(audioListeners);
    }
}