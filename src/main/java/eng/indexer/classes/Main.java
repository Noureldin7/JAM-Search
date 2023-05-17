package eng.indexer.classes;
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
        indexer.clear();
        
        String seederConnection = dotenv.get("CONN_READ");
        MongoClient seederClient = MongoClients.create(seederConnection);
        MongoDatabase seederDb = seederClient.getDatabase("search_engine");
        MongoCollection<org.bson.Document> seed_set = seederDb.getCollection("seed_set");

        org.bson.Document query = new org.bson.Document("hash", new org.bson.Document("$exists", true));
        long querySize = seed_set.countDocuments(query);
        int cnt = 0;
        for (var ans : seed_set.find(query)) {
            String url = ans.getString("url");
            System.out.println(url);
            try {
                Document document = Jsoup.connect(url).timeout(4000).get();
                indexer.index(url, document.html());
                System.out.println((double) ++cnt / querySize * 100 + "%");
            } catch (Exception e) {
                System.out.println("Error in indexing " + url);
                System.out.println(e);
                continue;
            }
        }
        // String url = "https://en.wikipedia.org/wiki/Queensland";
        // Document document = Jsoup.connect(url).timeout(4000).get();
        // indexer.index(url, document.html());
    }
}