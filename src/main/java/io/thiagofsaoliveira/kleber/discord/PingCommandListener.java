package io.thiagofsaoliveira.kleber.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class PingCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(PingCommandListener.class);

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent e
                && e.getName().equals("ping")) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug("PingCommandEvent received for guild: {}", guildId);

            JDA jda = e.getJDA();
            jda.getRestPing().queue(
                    time -> e.reply("Ping: %dms".formatted(time)).queue());
        }
    }
}
