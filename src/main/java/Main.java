import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
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
 * @version 1.5.0
 */
public class Main extends ListenerAdapter {
	private static String defaultServer;
	private static List<String> auth;
	public static MongoManager manager;
	public static HashMap<String, Server> servers;
	public static HashMap<String, String> aliases;
	final static Class<? extends List> ListClass = ArrayList.class;

	public static MongoClient client;
	public static MongoDatabase serverDatabase;
	public static MongoCollection<Document> serverList;
	public static final String TEXT_CYAN = "\u001B[36m";
	public static final String TEXT_RESET = "\u001B[0m";

	public static void main(String[] args) throws IOException, LoginException {
		Scanner in = new Scanner(new File("Config.txt"));
		//Config: bot token, logging mongodb string, id string
		//	public static JDA jda;
		String token = in.nextLine();
		JDA jda = JDABuilder.createDefault(token)
				.build();
		jda.addEventListener(new Main());
		System.err.println(TEXT_CYAN + "Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator() + TEXT_RESET);
		System.err.println(TEXT_CYAN + "jvm version: " + System.getProperty("java.version") + TEXT_RESET);
		String mongoConnectionString = in.nextLine();
		manager = new MongoManager(mongoConnectionString);
		auth = Arrays.asList(in.nextLine().split(" "));

		client = MongoClients.create(mongoConnectionString);
		serverDatabase = client.getDatabase("mc");
		serverList = serverDatabase.getCollection("servers");
		servers = new HashMap<>();
		aliases = new HashMap<>();
		defaultServer = in.nextLine();
		for(Document i: serverList.find()) {
			String name = i.getString("name");
			servers.put(name, Server.fromDocument(i));
			if(servers.get(name).getAliases() == null) {
				ArrayList<String> newAliases = new ArrayList<>();
				newAliases.add(name);
				servers.get(name).setAliases(newAliases);
			}
			for(String alias: servers.get(i.getString("name")).getAliases()) {
				aliases.put(alias, i.getString("name"));
			}
		}
		if(servers.containsKey(defaultServer)) {
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					Document myDoc = manager.getCollection(defaultServer, "startlogs").find().sort(new Document("_id", -1)).first();
					if(myDoc == null) return;
					if (myDoc.getString("type").equalsIgnoreCase("start")) {
						jda.getPresence().setActivity(Activity.playing(servers.get(defaultServer).getOfficialname() + " - ONLINE"));
					} else {
						jda.getPresence().setActivity(Activity.playing(servers.get(defaultServer).getOfficialname() + " - OFFLINE"));
					}
				}
			}, 0, 5000);
		}
//		System.err.println(servers);
	}
	private Main(){
//		MongoClient mongoClient = MongoClients.create(connectionString);
//		MongoDatabase lermit4 = mongoClient.getDatabase("lermit4");
//		joinlogs = lermit4.getCollection("joinlogs");
//		onlinelogs = lermit4.getCollection("onlinelogs");
//		startlogs = lermit4.getCollection("startlogs");

		LoggerFactory.getLogger("org.mongodb");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		for(Logger logger : loggerContext.getLoggerList()) {
//			System.err.println(logger.getName());
			if(logger.getName().startsWith("com.mongodb") || logger.getName().startsWith("org.mongodb") || logger.getName().startsWith("net.dv8tion")) {
				logger.setLevel(Level.WARN);
			}
		}
	}

	public boolean isOnline(String id) {
		try {
			Document cur = manager.getCollection(id, "startlogs").find().sort(new Document("_id", -1)).first();
			if (cur.getString("type").equalsIgnoreCase("start")) return true;
			else return false;
		}
		catch (NullPointerException e) {
			return false;
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		String serverName, serverNameF;
		OptionMapping om = event.getOption("server");
		if(om != null) {
			serverName = om.getAsString();
			if(!aliases.containsKey(serverName) || !servers.containsKey(aliases.get(serverName))){
				event.getHook().sendMessage("This server does not exist. Use `servers` to see a list of available servers. ").queue();
				return;
			}
			serverName = servers.get(aliases.get(serverName)).getName();
		}
		else serverName = defaultServer;
		serverNameF = servers.get(serverName).getOfficialname();

		if(!servers.containsKey(serverName)){
			event.getHook().sendMessage("This server does not exist. Use `servers` to see a list of available servers. ").queue();
			return;
		}

		if(event.getName().equals("help")){
			EmbedBuilder eb = new EmbedBuilder().setTitle("Command List (Slash commands)")
//					.addField("Slash Commands", "This bot recently migrated to slash commands. Message commands will no longer work. ", false)
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
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Player Join/Leave Activity for server " + serverNameF);
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("playername") + "\n" + cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			eb.setFooter("Server id " + serverName);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(event.getName().equals("ping")){
			event.getHook().sendMessage(event.getJDA().getGatewayPing() + " ms ping with Discord").queue();
		}
		else if(event.getName().equals("startlogs")){
			MongoCursor<Document> recent = manager.getCollection(serverName, "startlogs").find().sort(new Document("_id", -1)).iterator();
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Server Activity for server " + serverNameF);
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			eb.setFooter("Server id " + serverName);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(event.getName().equals("uptime")){
			Document cur = manager.getCollection(serverName, "startlogs").find().sort(new Document("_id", -1)).first();
			if(isOnline(serverName)){
				long uptime = System.currentTimeMillis() - cur.getLong("date");
				long ms = uptime % 1000, seconds = uptime/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime for server " + serverNameF)
						.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
			else{
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime for server " + serverNameF)
						.setDescription("The server is not on right now. Use `*start` to start the server. ");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
		}
		else if(event.getName().equals("botuptime")){
			RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
			long uptimems = rb.getUptime();
			long ms = uptimems % 1000, seconds = uptimems/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
			EmbedBuilder uptime = new EmbedBuilder().setTitle("Uptime for server " + serverNameF)
					.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
			uptime.setFooter("Server id " + serverName);
			event.getHook().sendMessageEmbeds(uptime.build()).queue();
		}
		else if (event.getName().equals("online")){
			if(isOnline(serverName)) {
				EmbedBuilder eb = new EmbedBuilder().setTitle("Current Online Players for server " + serverNameF + ": ");
//			System.out.println(manager.getCollection(serverName, "onlinelogs").find(eq("_id", 1)).first());
				Document get = manager.getCollection(serverName, "onlinelogs").find(eq("_id", 1)).first();
				List<Object> players = get.get("players", ListClass);
				for (Object player : players)
					eb.appendDescription(" ◈ " + player.toString() + "\n");
				if (players.isEmpty()) {
					eb.appendDescription("No online players :(");
				}
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
			else{
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime for server " + serverNameF)
						.setDescription("The server is not on right now. Use `*start` to start the server. ");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
		}
		else if(event.getName().equals("addserver")){
			String name = event.getOption("id").getAsString(), bigname = event.getOption("name").getAsString(), ip = event.getOption("ip").getAsString();
			OptionMapping rc = event.getOption("runcommand"), sc = event.getOption("stopcommand");
			String runcommand = null, stopcommand = null;
			if(rc != null) runcommand = rc.getAsString();
			if(sc != null) stopcommand = sc.getAsString();
			if(auth.contains(event.getMember().getId())){
		  		if(servers.containsKey(name)) {
					  event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server already exists").appendDescription("The server with name " + name + " already exists. If you want to override this server, use the `removeserver` command. ").build()).queue();
					  return;
				}
				Server serverToInsert = new Server(name, bigname, ip, runcommand, stopcommand);
				serverList.insertOne(serverToInsert.toDocument()); //adds to the "mc" collection which stores server info
				servers.put(name, serverToInsert);//add to hashmap
				aliases.put(name, name);
				manager.newCollection(name); //adds to the main database as a new server with logs
				EmbedBuilder eb = new EmbedBuilder().setTitle("Added a new Server").appendDescription("Added a new server with name " + name + ", alternate name " + bigname + ", and IP " + ip + "\n").appendDescription("Server raw Document data: \n" + serverToInsert.toDocument().toString());
				if(!servers.containsKey(defaultServer)){
					eb.appendDescription("Your default server is not set. If you want to set this server as the default, change the Config.txt file and put the id of this server into the specified line. You'll have to restart the bot for the status to display the default server's status. ");
				}
				event.getHook().sendMessageEmbeds(eb.build()).queue();
			}
			else{
				event.getHook().sendMessage("Authorization Required - only users specified in the Config.txt file can add or remove a server. ").queue();
			}
		}
		else if(event.getName().equals("removeserver")) {
			String name = event.getOption("id").getAsString();
			if(auth.contains(event.getMember().getId())) {
				if(name.equals(defaultServer)){
					event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Cannot Remove").appendDescription("You cannot remove the default server. To fix this, change the default server in the Config.txt file. ").build()).queue();
					return;
				}
				serverList.findOneAndDelete(eq("name", name));
				servers.remove(name);
				//TODO remove from aliases as well
				manager.removeCollection(name);
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Removed a server").appendDescription("Removed a server with name " + name + " from the bot. ").build()).queue();
			}
			else {
				event.getHook().sendMessage("Authorization Required - only users specified in the Config.txt file can add or remove a server. ").queue();
			}
		}
		else if(event.getName().equals("start")){
			if(!servers.containsKey(serverName)) {
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
				return;
			}
			Server server = servers.get(serverName);
			if(isOnline(serverName)){
				EmbedBuilder eb = new EmbedBuilder().setTitle("Already Started");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
				return;
			}
			if(server.getRuncommand() == null) {
				EmbedBuilder eb = new EmbedBuilder().setTitle("Error");
				eb.appendDescription("This server does not have a run command set up. ");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
				return;
			}
			event.getHook().sendMessage("Starting...").queue(); //
			try {
//				System.err.println("start");
				ProcessBuilder pb = new ProcessBuilder(servers.get(serverName).getRuncommand().split(" ")).inheritIO();
				Process p = pb.start();
			} catch (IOException e) {
				event.getHook().sendMessage("IOException").queue();
				e.printStackTrace();
				return;
			}
			EmbedBuilder eb = new EmbedBuilder().setTitle("Starting Server").appendDescription("Connect to `" + server.getIp() + "` to play on the server. ");
			eb.setFooter("Server id " + serverName);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("stop")){
			if(!servers.containsKey(serverName)) {
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
				return;
			}
			Server server = servers.get(serverName);
			if(server.getStopcommand() == null) {
				EmbedBuilder eb = new EmbedBuilder().setTitle("Error");
				eb.appendDescription("This server does not have a stop command set up. ");
				eb.setFooter("Server id " + serverName);
				event.getHook().sendMessageEmbeds(eb.build()).queue();
				return;
			}
			event.getHook().sendMessage("Stopping...").queue();
			try {
//				System.err.println("stop");
				ProcessBuilder pb = new ProcessBuilder(servers.get(serverName).getStopcommand().split(" ")).inheritIO();
				Process p = pb.start();
			} catch (IOException e) {
				event.getHook().sendMessage("IOException").queue();
				e.printStackTrace();
				return;
			}
			EmbedBuilder eb = new EmbedBuilder().setTitle("Stopped Server").appendDescription("The server is stopped.");
			eb.setFooter("Server id " + serverName);
			event.getHook().sendMessageEmbeds(eb.build()).queue();
		}
		else if(event.getName().equals("servers")){
			StringBuilder serverNames = new StringBuilder("Use the names in the second column for commands. \n");
			for(Map.Entry<String, Server> entry : servers.entrySet()){
				serverNames.append(" ◈  ").append(entry.getValue().getOfficialname()).append("\t").append("`").append(entry.getKey()).append("`").append("\n");
				if(entry.getValue().getAliases().size() > 1) {
					serverNames.append("       You can also use commands for this server with: ");
					for(String alias: entry.getValue().getAliases()) if(!alias.equals(entry.getKey())) serverNames.append("`").append(alias).append("`").append(" ");
					serverNames.append("\n");
				}
			}
			event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Servers").appendDescription(serverNames.toString()).build()).queue();
		}
		else if(event.getName().equals("addalias")){
			if(auth.contains(event.getMember().getId())) {
				if(!servers.containsKey(serverName)) {
					event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
					return;
				}
				String alias = event.getOption("alias").getAsString();
				if(aliases.containsKey(alias)){
					event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Alias already exists").appendDescription("This alias already exists. Remove it to use it again. ").build()).queue();
					return;
				}
				servers.get(serverName).aliases.add(alias);
				aliases.put(alias, serverName);
				serverList.findOneAndReplace(eq("name", serverName), servers.get(serverName).toDocument());
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Added new alias " + alias + " for server " + serverNameF).build()).queue();
			}
			else {
				event.getHook().sendMessage("Authorization Required - only users specified in the Config.txt file can perform this operation. ").queue();
			}
		}
		else if(event.getName().equals("removealias")){
			if(auth.contains(event.getMember().getId())) {
				if(!servers.containsKey(serverName)) {
					event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Server Not Found").appendDescription("The server wasn't found. Try running `servers` to see the list of servers. ").build()).queue();
					return;
				}
				String alias = event.getOption("alias").getAsString();
				servers.get(serverName).aliases.remove(alias);
				aliases.remove(alias);
				serverList.findOneAndReplace(eq("name", serverName), servers.get(serverName).toDocument());
				event.getHook().sendMessageEmbeds(new EmbedBuilder().setTitle("Removed alias " + alias + " for server " + serverNameF).build()).queue();
			}
			else {
				event.getHook().sendMessage("Authorization Required - only users specified in the Config.txt file can perform this operation. ").queue();
			}
		}
		super.onSlashCommandInteraction(event);
	}
	@Override
	public void onReady(@NotNull ReadyEvent event) {
		event.getJDA().updateCommands().addCommands(
				Commands.slash("botuptime", "See the uptime of the bot. "),

				Commands.slash("joinlogs", "See the past 5 player join/leave logs. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", false),

				Commands.slash("startlogs", "See the past 5 server start/stop logs. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", false),

				Commands.slash("online", "See a list of the online players on the server. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", false),

				Commands.slash("uptime", "See the uptime of the server. ")
						.addOption(OptionType.STRING, "server", "Server to show. ", false),

				Commands.slash("servers", "See the current list of available servers. "),

				Commands.slash("help", "Show the help message."),

				Commands.slash("start", "Start the specified server. ")
						.addOption(OptionType.STRING, "server", "Server to start. ", true),

				Commands.slash("stop", "Stop the specified server. ")
						.addOption(OptionType.STRING, "server", "Server to stop. ", true),

				Commands.slash("ping", "Current ping to Discord"),

				Commands.slash("addserver", "Add a new server to the list of available servers. ")
						.addOption(OptionType.STRING, "id", "ID of server to add. (ex. lermit4)", true)
						.addOption(OptionType.STRING, "name", "Name of server to add. (ex. Lermit 4)", true)
						.addOption(OptionType.STRING, "ip", "IP of server to add. (ex. 192.168.0.0)", true)
						.addOption(OptionType.STRING, "runcommand", "How to start the server. Look at the mod page for more information. ", false)
						.addOption(OptionType.STRING, "stopcommand", "How to stop the server. Look at the mod page for more information. ", false),

				Commands.slash("removeserver", "Remove a server from the list of available servers. ")
					.addOption(OptionType.STRING, "id", "ID of server to remove. (ex. lermit4)", true),

				Commands.slash("addalias", "Add an alias for a server for convenience. ")
						.addOption(OptionType.STRING, "server", "Server to add an alias for. (ex. lermit4)", true)
						.addOption(OptionType.STRING, "alias", "Alias of server to add. (ex. l4)", true),

				Commands.slash("removealias", "Remove an alias for a server. ")
						.addOption(OptionType.STRING, "server", "Server to remove an alias for. (ex. lermit4)", true)
						.addOption(OptionType.STRING, "alias", "Alias of server to remove. (ex. l4)", true)
		).queue();
	}
}
