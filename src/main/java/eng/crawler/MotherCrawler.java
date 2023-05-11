package eng.crawler;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import io.github.cdimascio.dotenv.Dotenv;


class urlObj{
    public ObjectId id;
    public String url;
    public String hash;
    public int encounters;
    public int score;
    public Document doc;
    public urlObj(Document urlDoc){
        this.id = urlDoc.getObjectId("_id");
        this.url = urlDoc.getString("url");
        this.hash = urlDoc.get("hash","");
        this.encounters = urlDoc.getInteger("encounters");
        this.score = urlDoc.getInteger("score");
        this.doc = urlDoc;
    }
}
class threadTracker{
    public AtomicInteger x;
    threadTracker(){
        x.set(0);;
    }
}
public class MotherCrawler {
    MongoClient client;
    MongoDatabase db;
    MongoCollection<Document> seed_set;
    int threads;
    MotherCrawler(String connString, int threads) throws Exception{
        this.client = MongoClients.create(connString);
        this.db = client.getDatabase("search_engine");
        this.seed_set = db.getCollection("seed_set");
        this.threads = threads;
    }
    public void operate(int limit) throws Exception{
        AtomicInteger count = new AtomicInteger(0);
        while(true)
        {
            while(count.get()<threads)
            {
                if(seed_set.countDocuments(Filters.exists("hash"))>6000)
                {
                    System.out.println("Max Cap Reached");
                    MongoIterable<ObjectId> ids = seed_set.find(Filters.exists("hash")).limit(4000).sort(Sorts.ascending("encounters")).map(doc->doc.getObjectId("_id"));
                    seed_set.deleteMany(Filters.in("_id", ids));
                }
                FindIterable<Document> toBeCrawled = seed_set.find().limit(limit).sort(Sorts.descending("score"));
                seed_set.updateMany(Filters.empty(), Updates.inc("score", 10));
                seed_set.updateMany(Filters.in("_id", toBeCrawled.map(doc->doc.getObjectId("_id"))), Updates.set("score", 0));
                // new MinionCrawler(seed_set, count, toBeCrawled.map(doc->new urlObj(doc)).into(new LinkedList<urlObj>())).run();
                new Thread(new MinionCrawler(seed_set, count, toBeCrawled.map(doc->new urlObj(doc)).into(new LinkedList<urlObj>()))).start();
                count.incrementAndGet();
                System.out.println("Minion Spawned");
            }
            System.out.println("Mother Waiting...");
            synchronized(count)
            {
                count.wait();
            }
        }
    }
    public static void main(String[] args) throws Exception{
        Dotenv dotenv = Dotenv.load();
        String connString = dotenv.get("CONN_STRING");
        MotherCrawler crawler_obj = new MotherCrawler(connString,4);
        crawler_obj.operate(10);
    }
}
