import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Objects;

public class Listener extends ListenerAdapter {
    final HashMap<Guild, Instant> cooldownMap = new HashMap<>();
    final MessageEmbed helpEmbed = new EmbedBuilder()
            .setTitle("Help Menu")
            .addField("/count [user]", "Counts a user's messages in this guild.", true)
            .addField("/help", "Opens this help menu.", true)
            .build();

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        switch (event.getName()) {
            case "count" -> {
                Guild guild = event.getGuild();
                if (cooldownMap.containsKey(guild)) {
                    // If current time is after the cooldown
                    if (Instant.now().isAfter(cooldownMap.get(guild))) {
                        // Reset cooldown
                        cooldownMap.put(guild, Instant.now().plus(3, ChronoUnit.MINUTES));
                    }
                    // Otherwise, tell them they're on cooldown
                    else {
                        event.reply("You can't use `/count` in this server until the cooldown expires <t:" + cooldownMap.get(guild).getEpochSecond() + ":R>.").queue();
                        return;
                    }
                } else {
                    // Add them to cooldown
                    cooldownMap.put(guild, Instant.now().plus(3, ChronoUnit.MINUTES));
                }
                assert guild != null; // Slash command
//                TextChannel channel = event.getTextChannel();
                User user = Objects.requireNonNull(event.getOption("user")).getAsUser(); // Required option
                try {
                    guild.retrieveMember(user).complete();
                } catch (ErrorResponseException e) {
                    if (e.getErrorResponse().equals(ErrorResponse.UNKNOWN_USER)) {
                        event.reply("This user does not exist.").queue();
                        return;
                    }
                }
                InteractionHook hook = event.deferReply().complete();
                new MessageFetcher(hook, guild, user, event.getUser()).fetchMessageCount();
            }
            case "help" -> {
                event.replyEmbeds(helpEmbed).setEphemeral(true).queue();
            }
        }
    }
}
