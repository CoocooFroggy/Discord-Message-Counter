import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;

public class MessageFetcher {
    private final InteractionHook hook;
    private final Guild guild;
    private final User user;
    private final User runner;
    private final EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(new Color(0xFF6A00))
            .setTitle("Working...")
            .setDescription("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.");

    private final ArrayList<TextChannel> channelsToCount = new ArrayList<>();
    private int total = 0;

    public MessageFetcher(InteractionHook hook, Guild guild, User user, User runner) {
        this.hook = hook;
        this.guild = guild;
        this.user = user;
        this.runner = runner;

        embedBuilder.setAuthor(user.getName(), null, user.getAvatarUrl());
    }

    public void fetchMessageCount() {
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
        // Clone channels into our own editable list
        channelsToCount.addAll(guild.getTextChannels());
        // Loop through all messages
        countNextChannel();
    }

    private void countNextChannel() {
        if (channelsToCount.size() == 0) {
            addTotalToEmbed(total);
            hook.sendMessage("Finished counting.\n" + runner.getAsMention())
                    .queue();
            return;
        }
        TextChannel channel = channelsToCount.get(0);
        // If they can't read messages, go to next channel
        if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_READ)) {
            embedNoPerms(channel);
            channelsToCount.remove(channel);
            countNextChannel();
            return;
        }
        embedStartChannel(channel);
        countMessagesInChannel(channel, (count) -> {
            addChannelToEmbed(channel, count);
            total += count;
            channelsToCount.remove(channel);
            countNextChannel();
        });
    }

    private void countMessagesInChannel(TextChannel channel, Consumer<Integer> callback) {
        // We have to make it an array of 1 element for some reason
        final int[] channelCount = {0};
        channel.getIterableHistory().forEachAsync((message) -> {
            if (message.getAuthor().equals(user))
                channelCount[0]++;
            return true;
        }).thenRun(() -> callback.accept(channelCount[0]));
    }

    private void embedStartChannel(TextChannel channel) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message
                        .replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "") +
                        // Counting #general...
                        "Counting messages in " + channel.getAsMention() + "...\n" +
                        // Ping message
                        "\nThis make take a while depending on the size of the server. We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void embedNoPerms(TextChannel channel) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message
                        .replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "") +
                        // Cannot read messages in #general.
                        "Cannot read messages in " + channel.getAsMention() + ".\n" +
                        // Ping message
                        "\nThis make take a while depending on the size of the server. We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void addChannelToEmbed(TextChannel channel, int channelCount) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message and ping message
                        .replace("Counting messages in " + channel.getAsMention() + "...\n", "")
                        .replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "")
                        +
                        // #general: 5
                        channel.getAsMention() + ": " + channelCount + "\n" +
                        // Ping message
                        "\nThis make take a while depending on the size of the server. We'll ping you when we're done counting."
        );
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void addTotalToEmbed(int total) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString().replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "") +
                        // Total: 5000 messages
                        "\nTotal: **" + total + "** messages"
        );
        embedBuilder.setTitle(user.getName() + "'s Messages in " + guild.getName());
        embedBuilder.setColor(new Color(0x00BB05));
        hook.editOriginalEmbeds(embedBuilder.build()).queue();
    }
}
