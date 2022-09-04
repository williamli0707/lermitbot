import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;

public class test {
	public static void main(String[] args) throws IOException {
		MongoClient client = MongoClients.create("mongodb+srv://mongomod:FzT7L7KlDwGfSuMn@cluster0.hix0z.mongodb.net/?retryWrites=true&w=majority");
		MongoDatabase db = client.getDatabase("vaulthunters");
//		MongoCollection collection = db.
	}
}
