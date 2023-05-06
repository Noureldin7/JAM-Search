package eng.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.Aggregates;

import eng.util.*;

class urlObj extends Document{
    String url;
    String hash;
    Integer score;
    public urlObj(String url, String hash, Integer score){
        this.url = url;
        this.hash = hash;
        this.score = score;
    }
}
public class Crawler {
    static String httpsPattern = "(https?://)";
    static String wwwPattern = "(www\\.)?";
    static String domainPattern = "[a-z]{1,63}(\\.[a-z]{1,63})*";
    static String tldPattern = "(\\.[a-z]{2,6})";
    static String routePattern = "(/[a-z]+)*";
    static String urlPattern = httpsPattern+wwwPattern+domainPattern+tldPattern+routePattern;
    MongoDatabase db;
    MongoCollection<Document> seed_set;
    HashSet<String> urlHashes;
    HashSet<String> urlSet;
    Queue<String> urlQueue;
    Vector<Document> newUrls;

    Crawler(String connString) throws IOException{
        MongoClient client = MongoClients.create(connString);
        // newUrls = new Vector<Document>();
        // urlHashes = new HashSet<String>();
        // urlSet = new HashSet<String>();
        urlQueue = new LinkedList<String>();
        db = client.getDatabase("search_engine");
        seed_set = db.getCollection("seed_set");
        // Add Filters in find to sort based on score and limit to 6000 url
        // Score = changes*0.8 + frequency*0.2
        Bson query = new Document(){{
            Sorts.descending("score");
            Aggregates.limit(5);
        }};
        FindIterable<Document> toBeCrawled = seed_set.find(query);
        seed_set.updateMany(query,Updates.set("score", 0));
        toBeCrawled.forEach(doc -> {
            // urlHashes.add((String)doc.get("hash"));
            // urlSet.add((String)doc.get("url"));
            urlQueue.add((String)doc.get("url"));
        });
    }
    public void crawl() throws IOException, MalformedURLException, NoSuchAlgorithmException{
        Scrap parsedPage;
        while (!urlQueue.isEmpty()) {
            String urlString = urlQueue.poll();
            try {
                parsedPage = new Scrap(urlString);
            } catch (Exception e) {
                continue;
            }
            Pattern urlRegEx = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
            Matcher matchObj = urlRegEx.matcher(parsedPage.bodyHtml);
            while(matchObj.find()){
                String url = matchObj.group();
                try {
                    parsedPage = new Scrap(url);
                } catch (Exception e) {
                    continue;
                }
                String hash = parsedPage.urlHash;
                Document prevRecord = seed_set.find(Filters.eq("url", url)).first();
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
                // if(urlHashes.contains(hash)||urlSet.contains(url)){
                //     continue;
                // }
                // urlHashes.add(hash);
                // urlSet.add(url);
                seed_set.insertOne(new Document(){{
                    put("url", url);
                    put("hash", hash);
                    put("score", 5);
                }});
                // newUrls.add(new Document() {{
                //     put("url", url);
                //     put("hash", hash);
                //     put("score", 0);
                // }});
            }
        }
        // if(!newUrls.isEmpty()){
        //     //TODO Send new urls to indexer
        //     seed_set.insertMany(newUrls);
        // }
        
    }
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException{
        // String httpsPattern = "(https?://)?";
        // String wwwPattern = "(www\\.)?";
        // String domainPattern = "[a-z]{1,63}(\\.[a-z]{1,63})*";
        // String tldPattern = "(\\.[a-z]{2,6})";
        // String routePattern = "(/[a-z]+)*";
        // Pattern urlRegEx = Pattern.compile(httpsPattern+wwwPattern+domainPattern+tldPattern+routePattern,Pattern.CASE_INSENSITIVE);
        // Matcher matchObj = urlRegEx.matcher("http://eng.cu.edu.eg/en/<d>www.google.com");
        // while(matchObj.find()){
        //     String s = matchObj.group();
        //     System.out.println(s);
        // }
        Crawler crawler_obj = new Crawler("mongodb://root:password@localhost:27017/?authSource=admin");
        long x = System.nanoTime();
        crawler_obj.crawl();
        System.out.println(System.nanoTime()-x);
        // crawler_obj.crawl(10);
    }
}
