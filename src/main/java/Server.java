import org.bson.Document;

public class Server {
    private String name, bigname, ip;
    public Server(String name, String bigname, String ip) {
        this.name = name;
        this.bigname = bigname;
        this.ip = ip;


    }
    public Server(){

    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOfficialname() {
        return bigname;
    }
    public void setOfficialname(String officialname) {
        this.bigname = officialname;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }

    public Document toDocument() {
        Document doc = new Document();
        doc.append("name", name);
        doc.append("bigname", bigname);
        doc.append("ip", ip);
        return doc;
    }
    public static Server fromDocument(Document doc) {
        Server server = new Server();
        server.setName(doc.getString("name"));
        server.setOfficialname(doc.getString("officialname"));
        server.setIp(doc.getString("ip"));
        return server;
    }
}
