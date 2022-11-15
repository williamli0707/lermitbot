import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Server {
    private String name, bigname, ip, runcommand = "", stopcommand = "";
    public List<String> aliases;
    public Server(String name, String bigname, String ip) {
        aliases = new ArrayList<>();
        this.name = name;
        this.bigname = bigname;
        this.ip = ip;
        aliases.add(name);
//        System.err.println("adding server with name: " + name + " bigname: " + bigname + " ip: " + ip);
    }

    public Server(String name, String bigname, String ip, String runcommand, String stopcommand) {
        aliases = new ArrayList<>();
        this.name = name;
        this.bigname = bigname;
        this.ip = ip;
//        System.err.println("adding server with name: " + name + " bigname: " + bigname + " ip: " + ip);
        if(runcommand != null) this.runcommand = runcommand;
        if(stopcommand != null) this.stopcommand = stopcommand;
        aliases.add(name);
    }

    public String getName() {
        return name;
    }
    public String getOfficialname() {
        return bigname;
    }
    public String getIp() {
        return ip;
    }

    public String getRuncommand() {return runcommand;}
    public void setRuncommand(String runcommand) {this.runcommand = runcommand;}
    public String getStopcommand() {return stopcommand;}
    public void setStopcommand(String stopcommand) {this.stopcommand = stopcommand;}

    public List<String> getAliases() {return aliases;}
    public void setAliases(ArrayList<String> aliases) {this.aliases = aliases;}

    public Document toDocument() {
        Document doc = new Document();
        doc.append("name", name);
        doc.append("officialname", bigname);
        doc.append("ip", ip);
        doc.append("runcommand", runcommand);
        doc.append("stopcommand", stopcommand);
        doc.append("aliases", aliases);
        return doc;
    }
    public static Server fromDocument(Document doc) {
        Server server = new Server(doc.getString("name"), doc.getString("officialname"), doc.getString("ip"));
        server.setRuncommand(doc.getString("runcommand"));
        server.setStopcommand(doc.getString("stopcommand"));
        server.setAliases(doc.get("aliases", ArrayList.class));
        return server;
    }

    @Override
    public String toString() {
        return "Name: " + name + "\nAlt name: " + bigname +
                "\nIP: " + ip + "\nRun command: " + runcommand +
                "\nStop command: " + stopcommand + "\nAliases: " + aliases.toString();
    }
}
