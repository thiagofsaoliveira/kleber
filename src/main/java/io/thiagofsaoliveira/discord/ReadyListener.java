package io.thiagofsaoliveira.discord;

import io.thiagofsaoliveira.audio.AudioPlayer;
import io.thiagofsaoliveira.audio.AudioPlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadyListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(ReadyListener.class);

    private final AudioPlayerManager audioManager;

    public ReadyListener(AudioPlayerManager audioManager) {
        this.audioManager = audioManager;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            log.info("Bot connected.");

            JDA jda = event.getJDA();
            jda.getGuilds().forEach(guild -> {
                AudioPlayer audioPlayer =
                        audioManager.getAudioPlayer(guild.getIdLong());
                var adapter = audioPlayer.getAudioConnectionAdapter();
                AudioManager guildAudioManager = guild.getAudioManager();
                guildAudioManager.setSendingHandler(adapter);
            });
        }
    }
}
