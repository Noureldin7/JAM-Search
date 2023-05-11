package eng.crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;

import org.bson.Document;

import io.github.cdimascio.dotenv.Dotenv;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Seeder {
    public static void main(String[] args) throws IOException, MalformedURLException, NoSuchAlgorithmException{
        String fileName = "seed.txt";
        Dotenv dotenv = Dotenv.load();
        String connString = dotenv.get("CONN_STRING");
        MongoClient client = MongoClients.create(connString);
        MongoDatabase db = client.getDatabase("search_engine");
        MongoCollection<Document> seed_set = db.getCollection("seed_set");
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = br.readLine();
        while(line != null){
            String x = line;
            Document doc = new Document(){{
                put("url", x);
                put("encounters", 0);
                put("score", 100);;
            }};
            seed_set.insertOne(doc);
            line = br.readLine();
        }
        br.close();
    }
}
