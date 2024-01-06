Mod page: [https://www.curseforge.com/minecraft/mc-mods/discord-server-view](https://www.curseforge.com/minecraft/mc-mods/discord-server-view)

This **server side** mod uploads a log of online players to MongoDB every 10 seconds. It also logs when players join and when they leave. The data is then interpreted by a Discord bot I made that works with the mod. The bot (self hosted for now) reads the MongoDB logs so you can tell without starting up Minecraft whether or not the server is on, or what people are on it

This mod is untested on Windows, but works on MacOS and Linux.

### You will need:

-   A server to host a Discord bot (can be the same as the server used to host the Minecraft server)

### Setting up the bot and adding your first server:

-   Create a discord bot at the Discord Developer Portal.
-   Follow the instructions under “Creating a Discord Bot” and “Add your Discord Bot to a Server” at [https://jda.wiki/using-jda/getting-started/](https://jda.wiki/using-jda/getting-started/).
-   Get your bot token from the Bot tab on the sidebar. Do not show this token to anyone. Download the bot jar file, as well as the Config.txt file given. In the line where it says to put the bot token, paste your bot token.
    -   If you don't see the Config.txt file in the files of this project, copy the file at https://pastebin.com/vUHaLj0B. 
-   Go to the MongoDB website and create a database. The free tier should be more than enough, however if you have a very large/active server you may want to consider paying for a higher tier. In the database page, click “Browse Collections” and create a new database called “mc” with a collection called “servers”.
-   Go back to the database page and click “Connect,” “Connect your application,” and then Java 4.3 or later. Copy the connection string (should start with “mongob+srv://“ and end with “retryWrites=true&w=majority”). Save this connection string.
-   Paste the connection string into the config file for the bot. Paste your Discord UID in the config file as well.
-   In your server, put the mod into the mods folder. Start the server and then stop it so that the config file is generated. Depending on your minecraft version, the config file will generate in different places, so look in the server console for a message from the mod about where to look for the config file.
-   Open the config file, and put the connection string there as well.
-   In your server properties file, make sure your “level-name” is something that uniquely identifies your server among all the servers that you have, not just “world.” The reason for this is that the mod uses the level-name to upload logs to MongoDB, so if you have multiple servers it is best to separate them. **If you change the level-name property, change your world folder to match the level-name, or the server will regenerate everything.**
-   Start the server, then the bot. The bot can be started by running the command java -jar lermitbot-version-SNAPSHOT.jar
-   In the bot, use the /addserver command.
    -   The “id” field should be the same as the level-name property. (for example, hpx, hypixel)
    -   The “name” field should be an alternate name, or more readable name for the server. (for example, Hypixel)
    -   The “ip” field is for the ip of the server, or what people use to connect to the server. (for example, [mc.hypixel.net](http://mc.hypixel.net/))
    -   The runcommand and stopcommand are optional. If you want a way to start the server from the bot, you should fill this out.
    -   The run command is a one line command used to start the server. If you use ssh, use the -tt option. For example, if you use tmux to run your server and your Minecraft server is separate from your bot server, your run command could be:  
      ``ssh -tt -i ~/.ssh/ssh-key.key user@192.168.0.0 tmux send-keys ~/path/to/script.sh Enter C-m```
    -   You could probably have a simpler command than this. 
-   It is recommended to have a script file to start your server. In the script file, have the java command to run the server. Also, before the java command, add a line which moves to the folder in which the server is located. (cd ~/path/to/server)
-   The stop command is similar. Once again, if you use tmux, a stop command could be:  
    ```ssh -tt -i ~/.ssh/ssh-key.key user@192.168.0.0 tmux send-keys /stop Enter C-m```
-   or you could have your own script on your bot server to stop the server, instead of having a really long command
-   Once the server is added, you might need to restart the bot.
-   Continue as needed with as many servers as you want.

### Using the bot:
-   In the server, the bot takes care of everything automatically. However, when starting the server, make sure that it says “MongoDB Connection established.” This will confirm that the mod is working. If not, there should be an error. You should double check your connection string.
-   In Discord, you can use these slash commands:
    -   uptime - find the uptime of the server
    -   online - see a list of currently online players
    -   botuptime - find the uptime of the bot
    -   joinlogs - see the past 5 player join/leave events
    -   startlogs - see the past 5 server start/stop events
    -   servers - see a list of servers used by the bot
    -   ping - ping to Discord
    -   addserver - add a new server
    -   removeserver - remove a server
    -   addalias - adds an alias for the server. This can make it easier to start a server if the id is annoying to type.
    -   removealias - remove an alias
    -   help - show a help message
    -   start - start a server
    -   stop - stop a server

### Some things to keep in mind:

- The start/stop command
- When you create a server, if you want to use the start/stop functionality, you need to provide the terminal command to start the server in one line. For example, if you use tmux and ssh to start the server, your start command might be:  
    ```ssh -tt -i ~/.ssh/ssh-key.key user@ip tmux send-keys ~/path/to/start/script.sh Enter C-m```.
    If you don’t have a .sh file that starts the server for you, it’s pretty simple to create one. Create a file called “run.sh” or something similar, and inside the file write:  
  
        #!/bin/sh
        cd /path/to/directory
        java -jar forge._._._.jar

    Save the file and run `sudo chmod +x run.sh`. You can now use ./run.sh to run the server, or ~/path/to/file/run.sh.
-   Putting `cd path/to/directory` before the java command is recommended, at the very beginning of your run.sh file, whether or not you have one already. In this way the bot can start multiple different servers. This is because the sh file executes from the current directory, so if you send the command /path/to/run.sh it will say it can’t find the jar file or something like that. If you put the `cd path/to/directory` it will execute in the directory the sh file is located in.
-   If you don’t want to use the start/stop functionality, just don’t add the option in the slash command.
-   Make sure the “ID” of the server from the bot is the same as the level-name in the server.properties file in the server. The mod uploads to MongoDB based on the level-name, and the bot takes information based on the ID of the server which is the same as the collection name in the database.

This is all still experimental, and there are almost certainly bugs, so feel free to leave feedback, questions, or issues you have :)
