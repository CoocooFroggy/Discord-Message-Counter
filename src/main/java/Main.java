import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import javax.security.auth.login.LoginException;

public class Main {
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        String token = System.getenv("MESSAGE_COUNTER_TOKEN");
        JDABuilder jdaBuilder = JDABuilder
                .createDefault(token)
                .setActivity(Activity.watching("users' messages"));
        try {
            jda = jdaBuilder.build();
        } catch (LoginException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        jda.awaitReady();

        if (args.length > 1 && args[0].equals("slash")) {
            if (args[1].equals("debug")) {
                Guild guild = jda.getGuildById("685606700929384489"); // Coocoo's Test Server
                assert guild != null;
                guild.upsertCommand("count", "Counts a user's messages in this guild.")
                        .addOption(OptionType.USER, "user", "Count the messages from this user.", true)
                        .complete();
                guild.upsertCommand("help", "Opens the help menu.")
                        .complete();
                System.out.println("Upserted slash commands to " + guild.getName() + ".");
                System.exit(0);
                return;
            } else if (args[1].equals("global")) {
                jda.upsertCommand("count", "Counts a user's messages in this guild.")
                        .addOption(OptionType.USER, "user", "Count the messages from this user.", true)
                        .complete();
                jda.upsertCommand("help", "Opens the help menu.")
                        .complete();
                System.out.println("Upserted slash commands globally.");
                System.exit(0);
                return;
            }
        }

        jda.addEventListener(new Listener());
        System.out.println("Started Message Counter!");
    }
}
