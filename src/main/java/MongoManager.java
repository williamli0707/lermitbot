import com.mongodb.ConnectionString;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.HashMap;

public class MongoManager {
    private final HashMap<String, MongoCollection<Document>[]> map;
    private final MongoClient client;
	public MongoManager(String connectionString) {
        ConnectionString string = new ConnectionString(connectionString);
        client = MongoClients.create(string);
        map = new HashMap<>();
        for(String i: client.listDatabaseNames()) if(!i.equalsIgnoreCase("mc")) addCollection(i);
    }
    public void addCollection(String id){
        MongoDatabase db = client.getDatabase(id);
        MongoCollection<Document> joinlogs = db.getCollection("joinlogs"),
                onlinelogs = db.getCollection("onlinelogs"),
                startlogs = db.getCollection("startlogs");
        map.put(id, new MongoCollection[]{joinlogs, onlinelogs, startlogs});
    }

    public void removeCollection(String id) {
        map.remove(id);
        client.getDatabase(id).drop();
    }

    public MongoCollection<Document> getCollection(String id, String log){
        if(log.equalsIgnoreCase("joinlogs")) return map.get(id)[0];
        else if(log.equalsIgnoreCase("onlinelogs")) return map.get(id)[1];
        else if(log.equalsIgnoreCase("startlogs")) return map.get(id)[2];
        return null;
    }

    public void newCollection(String id) throws MongoCommandException {
        MongoDatabase db = client.getDatabase(id);
        try {db.createCollection("joinlogs");} catch (MongoCommandException ignored) {}
        try {db.createCollection("onlinelogs");} catch (MongoCommandException ignored) {}
        try {db.createCollection("startlogs");} catch (MongoCommandException ignored) {}
        addCollection(id);
    }

//    public void updateCollection(String id, Document)
}
