package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
// import java.util.LinkedList;
// import java.util.Queue;
import java.util.Vector;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;


import eng.util.*;


//TODO Send new urls to indexer
//DONE Check Robots.txt
//TODO URL Normalization
//TODO Performance Issues
//DONE Synchronization
public class MinionCrawler extends Thread {
    
    // MongoDatabase db;
    MongoCollection<Document> seed_set;
    Vector<String> urlQueue;
    MinionCrawler(MongoCollection<Document> seed_set, Vector<String> urlQueue) throws IOException{
        
        // this.db = client.getDatabase("search_engine");
        this.seed_set = seed_set;
        this.urlQueue = urlQueue; 
    }
    public void crawl() throws IOException, MalformedURLException, NoSuchAlgorithmException{
        Scrap parsedPage;
        while (!urlQueue.isEmpty()) {
            String urlString = urlQueue.remove(urlQueue.size()-1);
            try {
                parsedPage = new Scrap(urlString);
            } catch (Exception e) {
                continue;
            }
            for (String url : parsedPage.getUrls()) {
                try {
                    parsedPage = new Scrap(url);
                } catch (Exception e) {
                    continue;
                }
                Document prevRecord;
                synchronized(seed_set)
                {
                    prevRecord = seed_set.find(Filters.eq("url", url)).first();
                }
                String hash = parsedPage.getUrlHash();
                if(prevRecord!=null){
                    if(!((String)prevRecord.get("hash")).equals(hash))
                    {
                        // Repeated & Changed
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(prevRecord, Updates.combine(Updates.set("hash", hash), Updates.inc("score",1)));
                        }
                    }
                    else
                    {
                        // Repeated
                        seed_set.updateOne(prevRecord, Updates.inc("score",0.2));

                    }
                    continue;
                }
                else
                {
                    synchronized(seed_set)
                    {
                        prevRecord = seed_set.find(Filters.eq("hash", hash)).first();
                    }
                    if(prevRecord!=null)
                    {
                        // Repeated
                        seed_set.updateOne(prevRecord, Updates.inc("score", 0.2));
                        continue;
                    }
                }
                synchronized(seed_set)
                {
                    seed_set.insertOne(new Document(){{
                        put("url", url);
                        put("hash", hash);
                        put("score", 10);
                    }});
                }
            }
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
