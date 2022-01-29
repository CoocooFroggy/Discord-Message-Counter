import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.util.List;

public class MessageFetcher {
    private final InteractionHook hook;
    private final Guild guild;
    private final User user;
    private final User runner;
    private final EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(new Color(0xFF6A00))
            .setTitle("Working...")
            .setDescription("We'll ping you when we're done counting.");

    public MessageFetcher(InteractionHook hook, Guild guild, User user, User runner) {
        this.hook = hook;
        this.guild = guild;
        this.user = user;
        this.runner = runner;

        embedBuilder.setAuthor(user.getName(), null, user.getAvatarUrl());
    }

    public void fetchMessageCount() {
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
        List<TextChannel> channels = guild.getTextChannels();
        int total = 0;
        for (TextChannel channel : channels) {
            int channelCount = 0;
            // If they can't read messages, go to next channel
            if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_READ)) {
                embedNoPerms(channel);
                continue;
            }
            embedStartChannel(channel);
            // Loop through all messages
            for (Message message : channel.getIterableHistory()) {
                if (message.getAuthor().equals(user))
                    channelCount++;
            }
            addChannelToEmbed(channel, channelCount);
            total += channelCount;
        }
        addTotalToEmbed(total);
        hook.sendMessage("Finished counting.\n" + runner.getAsMention())
                .queue();
    }

    private void embedStartChannel(TextChannel channel) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message
                        .replace("We'll ping you when we're done counting.", "") +
                        // Counting #general...
                        "Counting messages in " + channel.getAsMention() + "...\n" +
                        // Ping message
                        "We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void embedNoPerms(TextChannel channel) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message
                        .replace("We'll ping you when we're done counting.", "") +
                        // Cannot read messages in #general.
                        "Cannot read messages in " + channel.getAsMention() + ".\n" +
                        // Ping message
                        "We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void addChannelToEmbed(TextChannel channel, int channelCount) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message and ping message
                        .replace("Counting messages in " + channel.getAsMention() + "...\n", "")
                        .replace("We'll ping you when we're done counting.", "")
                        +
                        // #general: 5
                        channel.getAsMention() + ": " + channelCount + "\n" +
                        // Ping message
                        "We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void addTotalToEmbed(int total) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString().replace("We'll ping you when we're done counting.", "") +
                        // Total: 5000 messages
                        "\nTotal: **" + total + "** messages"
        );
        embedBuilder.setTitle(user.getName() + "'s Messages in " + guild.getName());
        embedBuilder.setColor(new Color(0x00BB05));
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }
}
