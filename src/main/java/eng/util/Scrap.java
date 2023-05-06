package eng.util;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Scrap {
    public Document page;
    public String bodyHtml;
    public String urlHash;
    public Scrap(String url) throws NoSuchAlgorithmException, IOException{
        page = Jsoup.connect(url).timeout(0).get();
        // page.select("style, script").remove();
        bodyHtml = page.body().html();
        urlHash = Hash.encrypt(bodyHtml, "SHA-1");
    }
}
