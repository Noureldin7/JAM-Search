package eng.crawler;

import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;


import eng.util.*;


//DONE Send new urls to indexer
//DONE Check Robots.txt
//DONE URL Normalization
//DONE Track Changes
//DONE Performance Issues
//DONE Synchronization
public class MinionCrawler extends Thread {
    
    MongoCollection<Document> seed_set;
    Queue<urlObj> urlQueue;
    AtomicInteger count;
    MinionCrawler(MongoCollection<Document> seed_set, AtomicInteger count, Queue<urlObj> urlQueue) throws Exception{
        this.count = count;
        this.seed_set = seed_set;
        this.urlQueue = urlQueue; 
    }
    public void crawl() throws Exception{
        Scrap scrapedPage;
        long tmp;
        long total = System.nanoTime();
        long totalWithoutScrap = System.nanoTime();
        long totalWithoutDB = System.nanoTime();
        while (!urlQueue.isEmpty()) {
            urlObj urlObject = urlQueue.poll();
            try {
                tmp = System.nanoTime();
                scrapedPage = new Scrap(urlObject.url);
                totalWithoutScrap+=System.nanoTime()-tmp;
            } catch (Exception e) {
                continue;
            }
            tmp = System.nanoTime();
            String hash = scrapedPage.getUrlHash();
            totalWithoutScrap+=System.nanoTime()-tmp;
            if(urlObject.hash.equals(""))
            {
                tmp = System.nanoTime();
                boolean allowed = scrapedPage.robotsAllow(urlObject.url);
                totalWithoutScrap+=System.nanoTime()-tmp;
                if(!allowed)
                {
                    synchronized(seed_set)
                    {
                        seed_set.deleteOne(Filters.eq("_id", urlObject.id));
                    }
                }
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
                        // Repeated & Changed | Send to Indexer
                        // new Thread(new indexer(reindex(scrapedPage.page))).start()
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.combine(
                                Updates.set("hash", hash),
                                Updates.set("score",prevRecord.timeSinceLastVisit*(Math.log10(prevRecord.encounters+1)+prevRecord.changes+1)),
                                Updates.inc("encounters",1),
                                Updates.inc("changes", 1)));
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                    }
                    else
                    {
                        // Repeated | Don't Send to Indexer
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id), Updates.combine(
                                Updates.set("score",prevRecord.timeSinceLastVisit*(Math.log10(prevRecord.encounters+1)+prevRecord.changes)),
                                Updates.inc("encounters", 1)));
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
                        // Repeated | Don't Send to Indexer
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.updateOne(Filters.eq("_id",prevRecord.id),Updates.combine(
                                Updates.set("score",prevRecord.timeSinceLastVisit*(Math.log10(prevRecord.encounters+1)+prevRecord.changes)),
                                Updates.inc("encounters", 1)));
                            seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                        continue;
                    }
                    else
                    {
                        // Admit to DB | Send to Indexer
                        // new Thread(new indexer(index(scrapedPage.page))).start()
                        tmp = System.nanoTime();
                        synchronized(seed_set)
                        {
                            seed_set.deleteOne(Filters.eq("_id",urlObject.id));
                            seed_set.insertOne(new Document(){{
                                put("url", urlObject.url);
                                put("encounters", urlObject.encounters +1);
                                put("visits", urlObject.visits);
                                put("changes", urlObject.changes);
                                put("time_since_last_visit", urlObject.timeSinceLastVisit);
                                put("score", urlObject.score);
                                put("hash", hash);
                            }});
                        }
                        totalWithoutDB+=System.nanoTime()-tmp;
                    }
                }
            }
            else
            {
                if(urlObject.hash.equals(hash))
                {
                    // Didn't change | Don't Send to Indexer
                    continue;
                }
                else
                {
                    // Changed | Send to Indexer
                    // new Thread(new indexer(reindex(scrapedPage.page))).start()
                    tmp = System.nanoTime();
                    synchronized(seed_set)
                    {
                        seed_set.updateOne(Filters.eq("_id",urlObject.id), Updates.combine(
                            Updates.set("hash", hash),
                            Updates.set("score", urlObject.timeSinceLastVisit*(Math.log10(urlObject.encounters)+urlObject.changes+1)),
                            Updates.inc("changes", 1)));
                    }
                    totalWithoutDB+=System.nanoTime()-tmp;
                }
            }
            tmp = System.nanoTime();
            List<String> urlList = scrapedPage.getUrls();
            totalWithoutScrap+=System.nanoTime()-tmp;
            if(!urlList.isEmpty())
            {
                Vector<Document> urls = new Vector<Document>();
                for (String url : urlList) {
                    urls.add(new Document(){{
                        put("url", url);
                        put("encounters", 0);
                        put("visits", 0);
                        put("changes", 0);
                        put("time_since_last_visit", 0);
                        put("score", 100);}});
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