import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Listener extends ListenerAdapter {
    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        switch (event.getName()) {
            case "count" -> {
                Guild guild = event.getGuild();
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
        }
    }
}
