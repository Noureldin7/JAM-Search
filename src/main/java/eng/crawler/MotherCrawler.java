package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
// import java.util.LinkedList;
// import java.util.Queue;
import java.util.Vector;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import io.github.cdimascio.dotenv.Dotenv;


class urlObj{
    public ObjectId id;
    public String url;
    public String hash;
    public Double score;
    public Document doc;
    public urlObj(Document urlDoc){
        id = urlDoc.getObjectId("_id");
        url = urlDoc.getString("url");
        hash = urlDoc.get("hash","");
        try {
            score = urlDoc.getDouble("score");
        } catch (Exception e) {
            score = urlDoc.getInteger("score").doubleValue();
        }
        doc = urlDoc;
    }
}
public class MotherCrawler {
    MongoClient client;
    MongoDatabase db;
    MongoCollection<Document> seed_set;
    Vector<Queue<urlObj>> urlQueue;
    int threads;
    MotherCrawler(String connString, int threads) throws IOException{
        this.client = MongoClients.create(connString);
        this.db = client.getDatabase("search_engine");
        this.seed_set = db.getCollection("seed_set");
        this.threads = threads;
        this.urlQueue = new Vector<Queue<urlObj>>(threads);
        for (int i = 0; i < threads; i++) {
            urlQueue.insertElementAt(new LinkedList<urlObj>(), i);
        }
    }
    public void prepCrawl(int limit){
        // Add Filters in find to sort based on score and limit to 6000 url
        // Score = changes*0.8 + frequency*0.2
        // BasicBSONObject queryObj = new BasicBSONObject();
        // queryObj.putAll(Sorts.descending("score").toBsonDocument());
        // queryObj.putAll(Aggregates.limit(limit).toBsonDocument());
        // Document query =  new Document(queryObj);
        // Bson query = new Document(){{
        //     putAll(new BasicBSONObject(Sorts.descending("score").toBsonDocument()).toMap());
        //     Sorts.descending("score");
        //     Aggregates.limit(limit);
        // }};
        urlQueue.replaceAll(queue->queue=new LinkedList<urlObj>());
        FindIterable<Document> toBeCrawled = seed_set.find().limit(limit).sort(Sorts.descending("score"));
        // seed_set.updateMany(query,Updates.set("score", 0));
        int threadIndex = 0;
        toBeCrawled.batchSize(Math.round(limit/threads));
        for (Document doc : toBeCrawled) {
            seed_set.updateOne(doc,Updates.set("score", 0));
            urlQueue.get(threadIndex++).add(new urlObj(doc));
            threadIndex%=(threads);
        }
    }
    public void spawn() throws IOException, MalformedURLException, NoSuchAlgorithmException{
        for (int i = 0; i < threads; i++) {
            new Thread(new MinionCrawler(seed_set, new LinkedList<urlObj>(urlQueue.get(i)))).start();
        }
        // new MinionCrawler(seed_set, new LinkedList<urlObj>(urlQueue.get(0))).run();
    }
    public static void main(String[] args) throws InterruptedException, MalformedURLException, NoSuchAlgorithmException, IOException{
        Dotenv dotenv = Dotenv.load();
        String connString = dotenv.get("CONN_STRING");
        MotherCrawler crawler_obj = new MotherCrawler(connString,2);
        while(true)
        {
            crawler_obj.prepCrawl(8);
            crawler_obj.spawn();
            System.out.println("Sleeping...");
            Thread.sleep(12000);
        }
        // long x = System.nanoTime();
        // System.out.println(System.nanoTime()-x);
    }
}
