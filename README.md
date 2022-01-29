# Discord Message Counter
This bot will count every message a user has ever sent in a server. It will also count per channel, giving you an embed with the number of messages from the user per channel as well as the server total.

[You can invite the bot here.](https://discord.com/api/oauth2/authorize?client_id=936819952374120479&permissions=65536&scope=bot%20applications.commands)

## Setup
Run `gradlew shadowjar` for a jar file which you can run with `java -jar`. You can also download a built jar file from the [Releases](https://github.com/CoocooFroggy/Discord-Message-Counter/releases) tab.

Set the environment variable `MESSAGE_COUNTER_TOKEN` to your bot's token.

## Bot usage
`/count [user]`: Counts how many messages a user has sent in the server, sending an embed with the number of messages per channel and in total.
`/help`: Shows a help menu.

Make sure the bot has permission to see messages in each channel you want counted, otherwise those channels will be skipped.
