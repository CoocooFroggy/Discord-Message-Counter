import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class Listener extends ListenerAdapter {
    final HashMap<Guild, Instant> cooldownMap = new HashMap<>();
    static final HashMap<Guild, MessageFetcher> currentlyCounting = new HashMap<>();
    private HashMap<Message, MessageFetcher> messageFetcherMap = new HashMap<>();
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
                if (guild == null) {
                    event.reply("This command can only be used in a server.").queue();
                }
                TextChannel channel = event.getTextChannel();
                User user = Objects.requireNonNull(event.getOption("user")).getAsUser(); // Required option

                InteractionHook hook = event.deferReply().complete();

                if (!channel.canTalk()) {
                    Message message = hook
                            .sendMessage("I can't send messages in this channel—results will be delivered via DM. Continue anyways?")
                            .addActionRow(
                                    Button.secondary("continue_anyway", Emoji.fromUnicode("✅")),
                                    Button.secondary("do_not_continue", Emoji.fromUnicode("❌"))
                            )
                            .complete();
                    messageFetcherMap.put(message, new MessageFetcher(hook, channel, guild, user, event.getUser()));
                    return;
                }

                MessageFetcher fetcher = new MessageFetcher(hook, channel, guild, user, event.getUser());
                cooldownAndCount(hook, fetcher);
            }
            case "help" -> event.replyEmbeds(helpEmbed).setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        switch (event.getButton().getId()) {
            case "continue_anyway" -> {
                Guild guild = event.getGuild();
                assert guild != null; // Slash command, guild is never null
                MessageFetcher fetcher = messageFetcherMap.get(event.getMessage());
                if (fetcher == null) {
                    event.reply("I forgot who the owner of this menu was. Start a count with `/count`.").setEphemeral(true).queue();
                    return;
                }

                if (!event.getUser().equals(fetcher.getRunner())) {
                    event.reply("This isn't your menu. Start your own count with `/count`.").setEphemeral(true).queue();
                }

                event.editComponents().setContent("Starting...").queue();
                cooldownAndCount(event.getHook(), fetcher);
            }
            case "do_not_continue" -> {
                event.editComponents().setContent("Cancelled.").queue();
            }
        }
    }

    private void cooldownAndCount(InteractionHook hook, MessageFetcher fetcher) {
        Guild guild = fetcher.getGuild();
        User target = fetcher.getTarget();

        if (checkCooldownActive(hook, guild)) return;

        try {
            guild.retrieveMember(target).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse().equals(ErrorResponse.UNKNOWN_USER)) {
                hook.sendMessage("This user does not exist.").queue();
                return;
            }
        }
        fetcher.startCounter();
        // Add them to currently counting cooldown
        currentlyCounting.put(guild, fetcher);
    }

    private boolean checkCooldownActive(InteractionHook hook, Guild guild) {
        // Currently counting cooldown
        if (currentlyCounting.containsKey(guild)) {
            MessageFetcher fetcher = currentlyCounting.get(guild);
            // If the count is happening in a server channel
            if (fetcher.getCounterEmbedMessage().getChannelType().equals(ChannelType.TEXT)) {
                hook.sendMessage(
                                "You can't use `/count` in this server until the previous count is finished.\n" +
                                        fetcher.getCounterEmbedMessage().getJumpUrl())
                        .queue();
            } else {
                hook.sendMessage(
                                "You can't use `/count` in this server until the previous count is finished.\n" +
                                        "Currently, " + fetcher.getRunner().getAsMention() + " is counting messages for " + fetcher.getTarget().getAsMention() + " in DMs.")
                        .allowedMentions(Set.of())
                        .queue();
            }
            return true;
        }
        // Timer cooldown
        if (cooldownMap.containsKey(guild)) {
            // If current time is after the cooldown
            if (Instant.now().isAfter(cooldownMap.get(guild))) {
                // Reset cooldown
                cooldownMap.put(guild, Instant.now().plus(3, ChronoUnit.MINUTES));
            }
            // Otherwise, tell them they're on cooldown
            else {
                hook.sendMessage("You can't use `/count` in this server until the cooldown expires <t:" + cooldownMap.get(guild).getEpochSecond() + ":R>.").queue();
                return true;
            }
        } else {
            // Add them to cooldown
            cooldownMap.put(guild, Instant.now().plus(3, ChronoUnit.MINUTES));
        }
        return false;
    }

    public static void removeFromCurrentlyCounting(Guild guild) {
        currentlyCounting.remove(guild);
    }
}
