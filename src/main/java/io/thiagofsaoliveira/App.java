package io.thiagofsaoliveira;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.thiagofsaoliveira.discord.PingCommandListener;
import io.thiagofsaoliveira.discord.ReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
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

        Object[] discordListeners = {
                new ReadyListener(),
                new PingCommandListener()
        };

        JDA jda = JDABuilder.createDefault(config.getToken())
                .setActivity(Activity.watching(config.getActivity()))
                .addEventListeners(discordListeners)
                .build();

        String pingDescription = messages.getMessage("PING_DESCRIPTION_MSG");

        Collection<SlashCommandData> commands = List.of(
                Commands.slash("ping", pingDescription).setGuildOnly(true)
        );

        jda.updateCommands().addCommands(commands).queue();
    }
}