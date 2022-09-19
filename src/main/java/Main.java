import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import javax.security.auth.login.LoginException;
import static com.mongodb.client.model.Filters.eq;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.List;

/**
 * A Discord Bot that works with MongoMod.
 * @author William Li
 * @version 1.0.1
 */
public class Main extends ListenerAdapter {
	private static String auth, defaultServer;
	public static MongoManager manager;
	public static HashMap<String, Server> servers;
	final static Class<? extends List> ListClass = ArrayList.class;

	public static MongoClient client;
	public static MongoDatabase serverDatabase;
	public static MongoCollection<Document> serverList;

	public static void main(String[] args) throws IOException, LoginException {
		Scanner in = new Scanner(new File("Config.txt"));
		//Config: bot token, logging mongodb string, id string
		//	public static JDA jda;
		String token = in.nextLine();
		JDA jda = JDABuilder.createDefault(token)
				.build();
		jda.addEventListener(new Main());
		System.err.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
		System.err.println("jvm version: " + System.getProperty("java.version"));
		String mongoConnectionString = in.nextLine();
		manager = new MongoManager(mongoConnectionString);
		auth = in.nextLine();

		client = MongoClients.create(mongoConnectionString);
		serverDatabase = client.getDatabase("mc");
		serverList = serverDatabase.getCollection("servers");
		servers = new HashMap();
		defaultServer = in.nextLine();
		for(Document i: serverList.find()) servers.put(i.getString("name"), Server.fromDocument(i));

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Document myDoc = manager.getCollection(defaultServer, "startlogs").find().sort(new Document("_id", -1)).first();
				if(myDoc.getString("type").equalsIgnoreCase("start")) {
					jda.getPresence().setActivity(Activity.playing(servers.get(defaultServer).getOfficialname() + " - ONLINE"));
				}
				else{
					jda.getPresence().setActivity(Activity.playing(servers.get(defaultServer).getOfficialname() + " - OFFLINE"));
				}
			}
		}, 0, 5000);
	}
	private Main(){
//		MongoClient mongoClient = MongoClients.create(connectionString);
//		MongoDatabase lermit4 = mongoClient.getDatabase("lermit4");
//		joinlogs = lermit4.getCollection("joinlogs");
//		onlinelogs = lermit4.getCollection("onlinelogs");
//		startlogs = lermit4.getCollection("startlogs");

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		for(Logger logger : loggerContext.getLoggerList()) {
			if(logger.getName().startsWith("com.mongodb") || logger.getName().startsWith("org.mongodb") || logger.getName().startsWith("net.dv8tion")) {
				logger.setLevel(Level.WARN);
			}
		}
	}
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		String serverName;
		OptionMapping om = event.getOption("server");
		if(om != null) serverName = om.getAsString();
		else serverName = defaultServer;

		if(!servers.containsKey(serverName)){
			event.getHook().sendMessage("This server does not exist. Use `servers` to see a list of available servers. ").queue();
		}

		if(event.getName().equals("help")){
			EmbedBuilder eb = new EmbedBuilder().setTitle("Command List")
					.addField("Slash Commands", "The bot recently migrated to slash commands. Message commands will no longer work. ", false)
					.addField("Server Commands", "`uptime`: See the uptime of the server. \n"
							 + "`joinlogs`: See the past 5 player join/leave logs. \n"
							 + "`startlogs`: See the past 5 server start/stop logs. \n"
							 + "`online`: See a list of the online players on The server. \n "
							 + "`start`: Start the server. \n "
							 + "`stop`: Stop the server. \n "
							 + "These commands need to have a server name listed after the command. If no server name is specified, it will default to `lermit4`. For a list of registered server names run the `servers` command.  \n", false)
					.addField("Misc Commands", ""
							 + "`botuptime`: See the uptime of the bot. \n"
							 + "`servers`: See the current list of available servers. \n"
							 + "`ping`: See the ping with Discord. \n"
							 + "`help`: See this message. ", false);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("joinlogs")){
			MongoCursor<Document> recent = manager.getCollection(serverName, "joinlogs").find().sort(new Document("_id", -1)).iterator();
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Player Join/Leave Activity");
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("playername") + "\n" + cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			event.getHook().sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(event.getName().equals("ping")){
			event.getHook().sendMessage(event.getJDA().getGatewayPing() + " ms").queue();
		}
		else if(event.getName().equals("startlogs")){
			MongoCursor<Document> recent = manager.getCollection(serverName, "startlogs").find().sort(new Document("_id", -1)).iterator();
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Server Activity");
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			event.getHook().sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(event.getName().equals("uptime")){
			Document cur = manager.getCollection(serverName, "startlogs").find().sort(new Document("_id", -1)).first();
			if(cur.getString("type").equalsIgnoreCase("start")){
				long uptime = System.currentTimeMillis() - cur.getLong("date");
				long ms = uptime % 1000, seconds = uptime/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime")
						.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
			else{
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime")
						.setDescription("The server is not on right now. Use `*start` to start the server. ");
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
		}
		else if(event.getName().equals("botuptime")){
			RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
			long uptimems = rb.getUptime();
			long ms = uptimems % 1000, seconds = uptimems/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
			EmbedBuilder uptime = new EmbedBuilder().setTitle("Uptime")
					.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
			event.getHook().sendMessageEmbeds(uptime.build()).queue();
		}
		else if (event.getName().equals("online")){
			EmbedBuilder eb = new EmbedBuilder().setTitle("Current Online Players: ");
			Document get = manager.getCollection(serverName, "onlinelogs").find(eq("_id", 1)).first();
			List<Document> players = get.get("players", ListClass);
			for(int i = 0;i < players.size();i++){
				eb.appendDescription(" ◈ " + players.get(i) + "\n");
			}
			if(players.isEmpty()) {
				eb.appendDescription("No online players :(");
				eb.setImage("https://media.discordapp.net/attachments/780590148517756971/995826061092855918/unknown.png");
			}
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("addserver")){
			String name = event.getOption("id").getAsString(), bigname = event.getOption("name").getAsString(), ip = event.getOption("ip").getAsString();
			if(event.getMember().getId().equals(auth)){
				Server serverToInsert = new Server(name, bigname, ip);
				serverList.insertOne(serverToInsert.toDocument()); //adds to the "mc" collection which stores server info
				servers.put(name, serverToInsert);//add to hashmap
				manager.newCollection(name); //adds to the main database as a new server with logs
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Added a new Server").appendDescription("Added a new server with name " + name + ", alternate name " + bigname + ", and IP " + ip + "\n").appendDescription("Server raw Document data: \n" + serverToInsert.toDocument().toString()).build()).queue();
			}
			else{
				event.getHook().sendMessage("Authorization Required").queue();
			}
		}
		else if(event.getName().equals("start")){
			if(!servers.containsKey(serverName)) {
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
				return;
			}
			Server server = servers.get(serverName);
			if(manager.getCollection(serverName, "startlogs").find().sort(new Document("_id", -1)).first().getString("type").equalsIgnoreCase("start")){
				EmbedBuilder eb = new EmbedBuilder().setTitle("Already Started")
						.setDescription("The server is already on. Connect to " + server.getIp() + " to play on the server. ")
						.setImage("https://media.discordapp.net/attachments/780590148517756971/995826028842844222/unknown.png");
				event.getHook().sendMessageEmbeds(eb.build()).queue();
				return;
			}
			event.getHook().sendMessage("Starting...").queue();
			try {
				System.err.println("start");
				ProcessBuilder pb = new ProcessBuilder("ssh", "-tt", "-i", "~/.ssh/ssh-key.key", "opc@" + server.getIp(), "tmux", "send-keys", "'~/" + server.getName() + "/minecraft/run.sh'", "Enter", "C-m").inheritIO();
				//ProcessBuilder pb = new ProcessBuilder("ssh", "-tt", "-i", "~/.ssh/ssh-key-2022-06-18.key", "opc@138.2.230.47", "tmux", "send-keys", "'~/pararcana/minecraft/run.sh'", "Enter", "C-m");
				Process p = pb.start();
			} catch (IOException e) {
				event.getHook().sendMessage("IOException").queue();
				e.printStackTrace();
				return;
			}
			EmbedBuilder eb = new EmbedBuilder().setTitle("Started Server").appendDescription("Connect to `" + server.getIp() + "` to play on the server. ")
					.setImage("https://media.discordapp.net/attachments/780590148517756971/995826028842844222/unknown.png");
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("stop")){
			if(!servers.containsKey(serverName)) {
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
				return;
			}
			Server server = servers.get(serverName);
			event.getHook().sendMessage("Stopping...").queue();
			try {
				System.err.println("stop");
				ProcessBuilder pb = new ProcessBuilder("ssh", "-tt", "-i", "~/.ssh/ssh-key.key", "opc@" + server.getIp(), "tmux", "send-keys", "/stop", "Enter", "C-m").inheritIO();
				Process p = pb.start();
			} catch (IOException e) {
				event.getHook().sendMessage("IOException").queue();
				e.printStackTrace();
				return;
			}
			EmbedBuilder eb = new EmbedBuilder().setTitle("Stopped Server").appendDescription("The server is stopped.");
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("servers")){
			StringBuilder serverNames = new StringBuilder("Use the names in the second column for commands. ");
			for(Server server : servers.values()){
				serverNames.append("\n ◈ ").append(server.getOfficialname()).append("\t").append(server.getName()).append("\n");
			}
			event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Servers").appendDescription(serverNames.toString()).build()).queue();
		}
		super.onSlashCommandInteraction(event);
	}
	@Override
	public void onReady(@NotNull ReadyEvent event) {
		event.getJDA().updateCommands().addCommands(
				Commands.slash("botuptime", "See the uptime of the bot. "),

				Commands.slash("joinlogs", "See the past 5 player join/leave logs. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", true),

				Commands.slash("startlogs", "See the past 5 server start/stop logs. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", true),

				Commands.slash("online", "See a list of the online players on the server. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", true),

				Commands.slash("uptime", "See the uptime of the server. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", true),

				Commands.slash("servers", "See the current list of available servers. "),

				Commands.slash("help", "Show this help message."),

				Commands.slash("start", "Start the specified server. ")
						.addOption(OptionType.STRING, "server", "Server to start. ", true),

				Commands.slash("stop", "Stop the specified server. ")
						.addOption(OptionType.STRING, "server", "Server to stop. ", true),

				Commands.slash("ping", "Current ping to Discord"),

				Commands.slash("addserver", "Add a new server to the list of available servers. ")
						.addOption(OptionType.STRING, "id", "ID of server to add. (ex. lermit4)", true)
						.addOption(OptionType.STRING, "name", "Name of server to add. (ex. Lermit 4)", true)
						.addOption(OptionType.STRING, "ip", "IP of server to add. (ex. 192.168.0.0)", true)
		).queue();
	}
}
