package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Queue;
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
    Queue<urlObj> urlQueue;
    MinionCrawler(MongoCollection<Document> seed_set, Queue<urlObj> urlQueue) throws IOException{
        
        // this.db = client.getDatabase("search_engine");
        this.seed_set = seed_set;
        this.urlQueue = urlQueue; 
    }
    public void crawl() throws IOException, MalformedURLException, NoSuchAlgorithmException{
        Scrap parsedPage;
        long tmp;
        long total = System.nanoTime();
        long totalWithoutScrap = System.nanoTime();
        long totalWithoutDB = System.nanoTime();
        while (!urlQueue.isEmpty()) {
            urlObj urlObject = urlQueue.poll();
            try {
                tmp = System.nanoTime();
                parsedPage = new Scrap(urlObject.url);
                totalWithoutScrap+=System.nanoTime()-tmp;
            } catch (Exception e) {
                continue;
            }
            tmp = System.nanoTime();
            String hash = parsedPage.getUrlHash();
            totalWithoutScrap+=System.nanoTime()-tmp;
            if(urlObject.hash.equals(""))
            {
                urlObj prevRecord = null;
                Document doc = null;
                synchronized(seed_set)
                {
                    tmp = System.nanoTime();
                    doc = seed_set.find(Filters.and(Filters.eq("url", urlObject.url),Filters.exists("hash"))).first();
                    totalWithoutDB+=System.nanoTime()-tmp;
                }
                if(doc!=null){
                    prevRecord = new urlObj(doc);
                }
                if(prevRecord!=null){
                    if(!prevRecord.hash.equals(hash))
                    {
                        // Repeated & Changed
                        synchronized(seed_set)
                        {
                            tmp = System.nanoTime();
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.combine(Updates.set("hash", hash), Updates.inc("score",1)));
                            totalWithoutDB+=System.nanoTime()-tmp;
                        }
                    }
                    else
                    {
                        // Repeated
                        tmp = System.nanoTime();
                        seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.inc("score",0.2));
                        totalWithoutDB+=System.nanoTime()-tmp;
                        
                    }
                    tmp = System.nanoTime();
                    seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                    totalWithoutDB+=System.nanoTime()-tmp;
                    continue;
                }
                else
                {
                    synchronized(seed_set)
                    {
                        tmp = System.nanoTime();
                        doc = seed_set.find(Filters.eq("hash", hash)).first();
                        totalWithoutDB+=System.nanoTime()-tmp;
                    }
                    if(doc!=null){
                        prevRecord = new urlObj(doc);
                    }
                    if(prevRecord!=null)
                    {
                        // Repeated
                        tmp = System.nanoTime();
                        seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.inc("score", 0.2));
                        totalWithoutDB+=System.nanoTime()-tmp;
                        continue;
                    }
                    else
                    {
                        tmp = System.nanoTime();
                        seed_set.updateOne(Filters.eq("_id",urlObject.id), Updates.set("hash", hash));
                        totalWithoutDB+=System.nanoTime()-tmp;
                    }
                }
            }
            tmp = System.nanoTime();
            List<String> urlList = parsedPage.getUrls();
            totalWithoutScrap+=System.nanoTime()-tmp;
            if(!urlList.isEmpty())
            {
                Vector<Document> urls = new Vector<Document>();
                for (String url : urlList) {
                    urls.add(new Document(){{put("url", url);put("score", 10);}});
                }
                tmp = System.nanoTime();
                seed_set.insertMany(urls);
                totalWithoutDB+=System.nanoTime()-tmp;
            }
        }
        System.out.println("total="+(System.nanoTime()-total)/1000000000);
        System.out.println("totalWithoutScrap="+(System.nanoTime()-totalWithoutScrap)/1000000000);
        System.out.println("totalWithoutDB="+(System.nanoTime()-totalWithoutDB)/1000000000);
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