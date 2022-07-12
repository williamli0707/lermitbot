import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import static com.mongodb.client.model.Filters.eq;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {

	public static JDA jda;

	private static String token;

	private static MongoCollection<Document> joinlogs, onlinelogs, startlogs;


	private ArrayList<String> commands = new ArrayList<>();

	final static Class<? extends List> ListClass = new ArrayList<Document>().getClass();

	public static void main(String[] args) throws IOException {
		Scanner in = new Scanner(new File("Config.txt"));

		token = in.nextLine();

		try {
			jda = JDABuilder.createDefault(token).build();
			System.out.println("Logged in as " + jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator());
		} catch (Exception e) {
			System.out.println("Login failed");
			e.printStackTrace();
		}

		System.err.println("jvm version: " + System.getProperty("java.version"));

		ConnectionString connectionString = new ConnectionString(in.nextLine());
		MongoClient client = MongoClients.create(connectionString);

		jda.addEventListener(new Main(connectionString, token));
		jda.getPresence().setActivity(Activity.playing("lermit"));

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Document myDoc = startlogs.find().sort(new Document("_id", -1)).first();
				if(myDoc.getString("type").equalsIgnoreCase("start")) {
					jda.getPresence().setActivity(Activity.playing("Lermit 4 - ONLINE"));
				}
				else{
					jda.getPresence().setActivity(Activity.playing("Lermit 4 is OFFLINE"));
				}
			}
		}, 0, 5000);

	}
	private Main(ConnectionString connectionString, String token){
		MongoClient mongoClient = MongoClients.create(connectionString);
		MongoDatabase lermit4 = mongoClient.getDatabase("lermit4");
		joinlogs = lermit4.getCollection("joinlogs");
		onlinelogs = lermit4.getCollection("onlinelogs");
		startlogs = lermit4.getCollection("startlogs");
		commands.add("ssh -i ~/.ssh/ssh-key-2022-06-18.key opc@192.9.249.213");
		commands.add("tmux attach");
		commands.add("./run.sh");

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		for(Logger logger : loggerContext.getLoggerList()) {
			if(logger.getName().startsWith("com.mongodb") || logger.getName().startsWith("org.mongodb") || logger.getName().startsWith("net.dv8tion")) {
				logger.setLevel(Level.INFO);
			}
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		String message = event.getMessage().getContentRaw();
		long guildId = event.getGuild().getIdLong(), memberId = event.getMember().getIdLong();
		MessageChannel channel = event.getChannel();
		GuildChannel guildChannel = event.getGuildChannel();
		if(event.getAuthor().isBot()) return;
		if(message.equalsIgnoreCase("*help")){
			EmbedBuilder eb = new EmbedBuilder().setTitle("Command List")
					.addField("Lermit 4 Commands", "`*uptime`: See the uptime of the server. \n"
							 + "`*botuptime`: See the uptime of the bot. \n"
							 + "`*joinlogs`: See the past 5 player join/leave logs. \n"
							 + "`*startlogs`: See the past 5 server start/stop logs. \n"
							 + "`*online`: See a list of the online players on Lermit 4.\n "
							 + "`*ping`: See the ping with Discord. \n"
							 + "`*help`: See this message. ", false);
			channel.sendMessageEmbeds(eb.build()).queue();
		}
		else if(message.equalsIgnoreCase("*joinlogs")){
			MongoCursor<Document> recent = joinlogs.find().sort(new Document("_id", -1)).iterator();
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Player Join/Leave Activity");
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("playername") + "\n" + cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			channel.sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(message.equalsIgnoreCase("*ping")){
			channel.sendMessage(jda.getGatewayPing() + " ms").queue();
		}
		else if(message.equalsIgnoreCase("*startlogs")){
			MongoCursor<Document> recent = startlogs.find().sort(new Document("_id", -1)).iterator();
			EmbedBuilder eb = new EmbedBuilder().setTitle("Recent Server Activity");
			for(int i = 0;i < 5;i++){
				Document cur = recent.next();
				eb.addField(new Date(cur.getLong("date")).toString(), cur.getString("type"), false);
				if(!recent.hasNext()) break;
			}
			channel.sendMessageEmbeds(eb.build()).queue();
			recent.close();
		}
		else if(message.equalsIgnoreCase("*uptime")){
			Document cur = startlogs.find().sort(new Document("_id", -1)).first();
			if(cur.getString("type").equalsIgnoreCase("start")){
				long uptime = System.currentTimeMillis() - cur.getLong("date");
				long ms = uptime % 1000, seconds = uptime/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime")
						.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
				event.getChannel().sendMessageEmbeds(eb.build()).queue();
			}
			else{
				EmbedBuilder eb = new EmbedBuilder().setTitle("Uptime")
						.setDescription("The server is not on right now. Use `*start` to start the server. ");
				event.getChannel().sendMessageEmbeds(eb.build()).queue();
			}
		}
		else if(message.equalsIgnoreCase("*botuptime")){
			RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
			long uptimems = rb.getUptime();
			long ms = uptimems % 1000, seconds = uptimems/1000, minutes = seconds/60, hours = minutes/60, days = hours/24;
			EmbedBuilder uptime = new EmbedBuilder().setTitle("Uptime")
					.setDescription(days + " days, " + hours % 24 + " hours, " + minutes % 60 + " minutes, " + seconds % 60 + " seconds, " + ms + " milliseconds");
			event.getChannel().sendMessageEmbeds(uptime.build()).queue();
		}
		else if (message.equalsIgnoreCase("*online")){
			EmbedBuilder eb = new EmbedBuilder().setTitle("Current Online Players: ");
			Document get = onlinelogs.find(eq("_id", 1)).first();
			List<Document> players = get.get("players", ListClass);
			for(int i = 0;i < players.size();i++){
				eb.appendDescription(" ◈ " + players.get(i) + "\n");
			}
			if(players.isEmpty()) {
				eb.appendDescription("No online players :(");
				eb.setImage("https://media.discordapp.net/attachments/780590148517756971/995826061092855918/unknown.png");
			}
			event.getChannel().sendMessageEmbeds(eb.build()).queue();
		}
		else if(message.startsWith("*start")){
			if(startlogs.find().sort(new Document("_id", -1)).first().getString("type").equalsIgnoreCase("start")){
				EmbedBuilder eb = new EmbedBuilder().setTitle("Already Started")
						.setDescription("The server is already on. Connect to 192.9.249.213 to play on the server. ")
						.setImage("https://media.discordapp.net/attachments/780590148517756971/995826028842844222/unknown.png");
				event.getChannel().sendMessageEmbeds(eb.build()).queue();
				return;
			}
			channel.sendMessage("Starting...").queue();
			String[] split = message.split(" ");
			try {
				System.err.println("start");
				ProcessBuilder pb = new ProcessBuilder("/home/williamli/Documents/lermitbot/start.sh");
//				ProcessBuilder pb = new ProcessBuilder("/Users/williamli/eclipse-workspace/lermitbot/start.sh");
				Process p = pb.start();
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
				System.out.println("output: ");
				String s;
				while ((s = stdInput.readLine()) != null) {
					System.out.println(s);
				}
				p.waitFor(3, TimeUnit.SECONDS);
				p.destroy();
//				pb.directory(new File("/Users/williamli/eclipse-workspace/lermitbot"));
			} catch (IOException e) {
				channel.sendMessage("IOException").queue();
				e.printStackTrace();
				return;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			EmbedBuilder eb = new EmbedBuilder().setTitle("Started Server").appendDescription("Connect to `192.9.249.213` to play on the server. ")
					.setImage("https://media.discordapp.net/attachments/780590148517756971/995826028842844222/unknown.png");
			channel.sendMessageEmbeds(eb.build()).queue();
		}
		super.onMessageReceived(event);
	}
}
