package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
// import java.util.LinkedList;
// import java.util.Queue;
import java.util.Vector;
import java.util.regex.Pattern;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;


import eng.util.*;


//TODO Send new urls to indexer
//TODO Check Robots.txt
//TODO URL Normalization
//TODO Synchronization
public class MinionCrawler extends Thread {
    static String httpsPattern = "(https?://)";
    static String wwwPattern = "(www\\.)?";
    static String domainPattern = "[a-z]{1,63}(\\.[a-z]{1,63})*";
    static String tldPattern = "(\\.[a-z]{2,6})";
    static String routePattern = "(/[-_a-z]+)*(.html)?";
    static String urlPattern = httpsPattern+wwwPattern+domainPattern+tldPattern+routePattern;
    MongoDatabase db;
    MongoCollection<Document> seed_set;
    Vector<String> urlQueue;
    Pattern urlRegEx;
    MinionCrawler(MongoClient client, Vector<String> urlQueue) throws IOException{
        
        this.db = client.getDatabase("search_engine");
        this.seed_set = db.getCollection("seed_set");
        this.urlQueue = urlQueue; 
        this.urlRegEx = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
    }
    public void crawl() throws IOException, MalformedURLException, NoSuchAlgorithmException{
        Scrap parsedPage;
        while (!urlQueue.isEmpty()) {
            long x = System.nanoTime();
            String urlString = urlQueue.remove(urlQueue.size()-1);
            try {
                parsedPage = new Scrap(urlString);
            } catch (Exception e) {
                continue;
            }
            for (String url : parsedPage.getUrls()) {
                if(!urlRegEx.matcher(url).matches()) continue;
                try {
                    parsedPage = new Scrap(url);
                } catch (Exception e) {
                    continue;
                }
                Document prevRecord = seed_set.find(Filters.eq("url", url)).first();
                String hash = parsedPage.getUrlHash();
                if(prevRecord!=null){
                    if(!((String)prevRecord.get("hash")).equals(hash))
                    {
                        // Repeated & Changed
                        seed_set.updateOne(prevRecord, Updates.combine(Updates.set("hash", hash), Updates.inc("score",1)));
                    }
                    else
                    {
                        // Repeated
                        seed_set.updateOne(prevRecord, Updates.combine(Updates.set("hash", hash), Updates.inc("score",0.2)));

                    }
                    continue;
                }
                else if((prevRecord = seed_set.find(Filters.eq("hash", hash)).first())!=null)
                {
                    // Repeated
                    seed_set.updateOne(prevRecord, Updates.combine(Updates.inc("score", 0.2)));
                    continue;
                }
                seed_set.insertOne(new Document(){{
                    put("url", url);
                    put("hash", hash);
                    put("score", 10);
                }});
            }
            System.out.println(System.nanoTime()-x);
        }
    }
    public void run(){
        try {
            this.crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done");
        // crawler_obj.crawl(10);
    }
}
