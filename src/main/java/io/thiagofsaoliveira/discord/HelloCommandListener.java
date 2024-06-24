package io.thiagofsaoliveira.discord;

import io.thiagofsaoliveira.Messages;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class HelloCommandListener implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(HelloCommandListener.class);

    private final Messages messages;

    public HelloCommandListener(Messages messages) {
        this.messages = messages;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent e
                && e.getName().equals("hello")) {
            Guild guild = Objects.requireNonNull(e.getGuild());
            long guildId = guild.getIdLong();
            log.debug("HelloCommandEvent received for guild: {}", guildId);

            Member member = Objects.requireNonNull(e.getMember());
            String name = member.getEffectiveName();
            String msg = messages.getMessageFormatted("GREETING_FORMAT", name);
            e.reply(msg).queue();
        }
    }
}
