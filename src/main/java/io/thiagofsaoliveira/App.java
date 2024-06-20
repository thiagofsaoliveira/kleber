package io.thiagofsaoliveira;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.thiagofsaoliveira.discord.ReadyListener;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.LoggerFactory;

public class App {

    public static void main(String[] args) {
        var config = Configuration.newConfiguration();

        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger log = context.getLogger("root");
        String logLevel = config.getLogLevel().toUpperCase();
        log.setLevel(Level.toLevel(logLevel));

        Object[] discordListeners = {
                new ReadyListener(),
        };

        JDABuilder.createDefault(config.getToken())
                .setActivity(Activity.watching(config.getActivity()))
                .addEventListeners(discordListeners)
                .build();
    }
}