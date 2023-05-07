package eng.util;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class Scrap {
    public Document page;
    // public List<String> urls;
    // public String bodyHtml;
    // public String urlHash;
    public List<String> getUrls(){
        return page.body().getElementsByTag("a").eachAttr("href");
    }
    public String getBodyHtml(){
        return page.body().html();
    }
    public String getUrlHash() throws NoSuchAlgorithmException{
        return Hash.encrypt(page.body().html(), "SHA-1");
    }
    public Scrap(String url) throws NoSuchAlgorithmException, IOException{
        // OkHttpClient okhttp = new OkHttpClient();
        // Request req = new Request.Builder().url(url).get().build();
        // page = Jsoup.parse(okhttp.newCall(req).execute().body().string());
        page = Jsoup.connect(url).timeout(4000).get();
        // page.select("style, script").remove();
        // urls = page.body().getElementsByTag("a").eachAttr("href");
        // bodyHtml = page.body().html();

        // urlHash = Hash.encrypt(bodyHtml, "SHA-1");
    }

    public static void main(String args[]) throws NoSuchAlgorithmException, IOException{
        new Scrap("https://google.com");
    }
}
