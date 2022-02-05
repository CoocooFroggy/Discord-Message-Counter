import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFetcher {
    private final InteractionHook hook;
    private final TextChannel channel;
    private final Guild guild;
    private final User target;
    private final User runner;

    private final EmbedBuilder embedBuilder = new EmbedBuilder()
            .setColor(new Color(0xFF6A00))
            .setTitle("Working...")
            .setDescription("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.");

    private Message counterEmbedMessage;
    private final ArrayList<GuildChannel> channelsToCount = new ArrayList<>();
    private int total = 0;

    public MessageFetcher(InteractionHook hook, TextChannel channel, Guild guild, User user, User runner) {
        this.hook = hook;
        this.channel = channel;
        this.guild = guild;
        this.target = user;
        this.runner = runner;

        embedBuilder.setAuthor(user.getName(), null, user.getAvatarUrl());
    }

    public void startCounter() {
        Message message = hook.sendMessage("Started. Check the sent embed for the current status.").complete();

        if (!channel.canTalk()) {
            // Send in DMs
            counterEmbedMessage = runner.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).complete();
        } else {
            counterEmbedMessage = channel
                    .sendMessageEmbeds(embedBuilder.build())
                    .reference(message)
                    .complete();
        }

        // Clone channels into our own editable list
        channelsToCount.addAll(guild.getChannels());
        // Start counting
        countNextChannel();
    }

    private void countNextChannel() {
        if (channelsToCount.size() == 0) {
            sendFinishedPing();
            Listener.removeFromCurrentlyCounting(guild);
            return;
        }
        GuildChannel channel = channelsToCount.get(0);
        channelsToCount.remove(channel);

        // If it is a category
        if (channel instanceof Category) {
            addCategoryToEmbed((Category) channel);
            countNextChannel();
            return;
        } else if (channel instanceof VoiceChannel) {
            // Do nothing for VCs
            countNextChannel();
            return;
        }

        TextChannel textChannel = (TextChannel) channel;
        // If they can't read messages, go to next channel
        if (!guild.getSelfMember().hasPermission(textChannel, Permission.MESSAGE_READ)) {
            embedNoPerms(textChannel);
            countNextChannel();
            return;
        }
        // Start counting the next channel
        embedStartChannel(textChannel);
        countMessagesInChannel(textChannel, (count) -> {
            addChannelToEmbed(textChannel, count);
            total += count;
            countNextChannel();
        });
    }

    private void sendFinishedPing() {
        addTotalToEmbed(total);
        // If they can't send messages in this channel
        if (!channel.canTalk()) {
            runner.openPrivateChannel().queue((privateChannel -> {
                // Send them a link to the finished embed
                privateChannel
                        .sendMessage("Finished counting.")
//                        .setEmbeds(embedBuilder.build())
                        .queue();
            }));
            return;
        }
        channel
                .sendMessage("Finished counting.\n" + runner.getAsMention())
                .reference(counterEmbedMessage)
                .queue();
    }

    private void countMessagesInChannel(TextChannel channel, Consumer<Integer> callback) {
        // We have to make it an array of 1 element for some reason
        final int[] channelCount = {0};
        channel.getIterableHistory().forEachAsync((message) -> {
            if (message.getAuthor().equals(target))
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
//        counterEmbedMessage.editMessageEmbeds(embedBuilder.build()).queue();
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
//        counterEmbedMessage.editMessageEmbeds(embedBuilder.build()).queue();
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
//        counterEmbedMessage.editMessageEmbeds(embedBuilder.build()).queue();
    }

    private void addCategoryToEmbed(Category category) {
        embedBuilder.setDescription(
                embedBuilder.getDescriptionBuilder().toString()
                        // Get rid of counting message and ping message
                        .replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "")
                        +
                        // Text Channels (in bold)
                        "**" + category.getName() + "**\n" +
                        // Ping message
                        "\nThis make take a while depending on the size of the server. We'll ping you when we're done counting."
        );
//        counterEmbedMessage.editMessageEmbeds(embedBuilder.build()).queue();
    }

    private void addTotalToEmbed(int total) {

        // Ensure we're not over 4096 messages
        String description = embedBuilder.getDescriptionBuilder().toString().replace("\nThis make take a while depending on the size of the server. We'll ping you when we're done counting.", "");
        String finalLine = // Total: 5000 messages
                "\nTotal: **" + total + "** messages";

        if (description.length() + finalLine.length() > 4096) {
            String tooManyMessage = "\nMore channels were counted, but could not fit in the embed.\n";
            String maximumDescription = description.substring(0, 4096 - tooManyMessage.length() - finalLine.length());
            // Cut off at the last new line
            Pattern pattern = Pattern.compile(".*\\n", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(maximumDescription);
            if (matcher.find())
                description = matcher.group(0) + tooManyMessage + finalLine;
            else
                description += finalLine;
        } else {
            description = description + finalLine;
        }

        embedBuilder.setDescription(description);
        embedBuilder.setTitle(target.getName() + "'s Messages in " + guild.getName());
        embedBuilder.setColor(new Color(0x00BB05));
        counterEmbedMessage.editMessageEmbeds(embedBuilder.build()).queue();
    }

    // GETTERS

    public Message getCounterEmbedMessage() {
        return counterEmbedMessage;
    }

    public User getRunner() {
        return runner;
    }

    public Guild getGuild() {
        return guild;
    }

    public User getTarget() {
        return target;
    }
}
