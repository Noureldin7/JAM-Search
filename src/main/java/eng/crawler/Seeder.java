package eng.crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import org.bson.Document;

import eng.util.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class Seeder {
    public static void main(String[] args) throws IOException, MalformedURLException, NoSuchAlgorithmException{
        HashSet<String> exploredSet = new HashSet<String>();
        String fileName = "seed.txt";
        String connString = "mongodb://root:password@localhost:27017/?authSource=admin";
        MongoClient client = MongoClients.create(connString);
        MongoDatabase db = client.getDatabase("search_engine");
        MongoCollection<Document> seed_set = db.getCollection("seed_set");
        seed_set.find(Filters.empty()).forEach(doc -> exploredSet.add((String)doc.get("hash")));
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while((line = br.readLine()) != null){
            Scrap parsedPage = new Scrap(line);
            if(exploredSet.contains(parsedPage.urlHash)){
                continue;
            }
            Document doc = new Document();
            doc.append("url", line);
            doc.append("hash", parsedPage.urlHash);
            doc.append("score", 5);
            seed_set.insertOne(doc);
        }
        br.close();
    }
}
