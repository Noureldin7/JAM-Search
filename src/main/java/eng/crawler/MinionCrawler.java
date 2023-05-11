package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;


import eng.util.*;


//TODO Send new urls to indexer
//DONE Check Robots.txt
//TODO URL Normalization
//DONE Performance Issues
//DONE Synchronization
public class MinionCrawler extends Thread {
    
    MongoCollection<Document> seed_set;
    Queue<urlObj> urlQueue;
    AtomicInteger count;
    MinionCrawler(MongoCollection<Document> seed_set, AtomicInteger count, Queue<urlObj> urlQueue) throws IOException{
        this.count = count;
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
                tmp = System.nanoTime();
                synchronized(seed_set)
                {
                    doc = seed_set.find(Filters.and(Filters.eq("url", urlObject.url),Filters.exists("hash"))).first();
                }
                totalWithoutDB+=System.nanoTime()-tmp;
                if(doc!=null){
                    prevRecord = new urlObj(doc);
                }
                if(prevRecord!=null){
                    if(!prevRecord.hash.equals(hash))
                    {
                        // Repeated & Changed
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.combine(Updates.set("hash", hash), Updates.inc("score",50), Updates.inc("encounters",1)));
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                    }
                    else
                    {
                        // Repeated
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.combine(Updates.inc("score",20), Updates.inc("encounters", 1)));
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                        
                    }
                    tmp = System.nanoTime();
                    synchronized(seed_set)
                    {
                        seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                    }
                    totalWithoutDB+=System.nanoTime()-tmp;
                    continue;
                }
                else
                {
                    tmp = System.nanoTime();
                    synchronized(seed_set)
                    {
                        doc = seed_set.find(Filters.eq("hash", hash)).first();
                    }
                    totalWithoutDB+=System.nanoTime()-tmp;
                    if(doc!=null){
                        prevRecord = new urlObj(doc);
                    }
                    if(prevRecord!=null)
                    {
                        // Repeated
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id),Updates.combine(Updates.inc("score", 20),Updates.inc("encounters", 1)));
                            seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                        continue;
                    }
                    else
                    {
                        // Admit to DB
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                            seed_set.insertOne(new Document(){{
                                put("url", urlObject.url);
                                put("encounters", urlObject.encounters +1);
                                put("score", urlObject.score);
                                put("hash", hash);
                            }});
                        }
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
                    urls.add(new Document(){{put("url", url);put("encounters", 0);put("score", 100);}});
                }
                tmp = System.nanoTime();
                synchronized(seed_set)
                {
                    seed_set.insertMany(urls);
                }
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
        System.out.println("Minion Finished");
        count.decrementAndGet();
        synchronized(count)
        {
            count.notify();
        }
    }
}