package io.thiagofsaoliveira.kleber.discord;

import io.thiagofsaoliveira.kleber.audio.AudioPlayer;
import io.thiagofsaoliveira.kleber.audio.AudioPlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildJoinListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(GuildJoinListener.class);

    private final AudioPlayerManager audioManager;

    public GuildJoinListener(AudioPlayerManager audioManager) {
        this.audioManager = audioManager;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof GuildJoinEvent) {
            JDA jda = event.getJDA();
            jda.getGuilds().stream()
                    .filter(guild -> !audioManager.containsAudioPlayer(
                            guild.getIdLong()))
                    .forEach(guild -> {
                        long guildId = guild.getIdLong();
                        log.debug("Guild joinned: {}", guildId);

                        AudioPlayer audioPlayer =
                                audioManager.getAudioPlayer(guildId);
                        var adapter =
                                audioPlayer.getAudioConnectionAdapter();
                        AudioManager guildAudioManager =
                                guild.getAudioManager();
                        guildAudioManager.setSendingHandler(adapter);
                    });
        }
    }
}
