package eng.indexer.classes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String args[]) throws Exception{
        Dotenv dotenv = Dotenv.load();
        String connectionString = dotenv.get("CONN_STRING");
        Storage storage = new MongoStorage(connectionString);
        Indexer indexer = new WebIndexer(storage);
        
        String seederConnection = dotenv.get("CONN_READ");
        MongoClient seederClient = MongoClients.create(seederConnection);
        MongoDatabase seederDb = seederClient.getDatabase("search_engine");
        MongoCollection<org.bson.Document> seed_set = seederDb.getCollection("seed_set");

        // org.bson.Document query = new org.bson.Document("hash", new org.bson.Document("$exists", true));

        org.bson.Document query = new org.bson.Document("indexed", false);

        ExecutorService executor = Executors.newFixedThreadPool(16);

        for (var ans : seed_set.find(query)) {
            executor.execute(() -> {
                String url = ans.getString("url");
                System.out.println(url);
                try {
                    Document document = Jsoup.connect(url).timeout(4000).get();
                    indexer.index(url, document.html());
                    seed_set.updateOne(ans, new org.bson.Document("$set", new org.bson.Document("indexed", true)));
                } catch (Exception e) {
                    System.out.println("Error in indexing " + url);
                    System.out.println(e);
                }
            });
        }
    }
}