package eng.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;


public class Scrap {
    static String httpsPattern = "(https?://)";
    static String wwwPattern = "(www\\.)?";
    static String domainNamePattern = "[a-z]{1,63}(\\.[a-z]{1,63})*";
    static String tldPattern = "(\\.[a-z]{2,6})+";
    static String routePattern = "(/[-_a-z]+)*(.html)?";
    static String domainPattern = httpsPattern+wwwPattern+domainNamePattern+tldPattern;
    static String urlPattern = httpsPattern+wwwPattern+domainNamePattern+tldPattern+routePattern;
    public Document page;
    SimpleRobotRulesParser robots;
    Pattern urlRegex;
    Pattern domainRegex;
    public Scrap(String url) throws NoSuchAlgorithmException, IOException{
        robots = new SimpleRobotRulesParser();
        page = Jsoup.connect(url).timeout(4000).get();
        this.domainRegex = Pattern.compile(domainPattern,Pattern.CASE_INSENSITIVE);
        this.urlRegex = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
    }
    SimpleRobotRules getrobots(String url) throws MalformedURLException, IOException{
        byte[] content = IOUtils.toByteArray(new URL(url).openStream());
        return robots.parseContent(url, content, "text/plain", "*");
    }
    Predicate<String> filterPredicate = (String url) -> {
        if(!urlRegex.matcher(url).matches()) return true;
        try {
            Matcher m = domainRegex.matcher(url);;
            m.find();
            if(getrobots(m.group()+"/robots.txt").isAllowed(url)){
                return false;
            }
            else{
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    };
    public List<String> getUrls(){
        long x = System.nanoTime();
        List<String> urls = page.body().getElementsByTag("a").eachAttr("href");
        urls.removeIf(filterPredicate);
        System.out.println((System.nanoTime()-x)/1000000000);
        return urls;
    }
    public String getBodyHtml(){
        return page.body().html();
    }
    public String getUrlHash() throws NoSuchAlgorithmException{
        return Hash.encrypt(page.body().html(), "SHA-1");
    }

    public static void main(String args[]) throws NoSuchAlgorithmException, IOException{
        // Pattern urlRegex = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        // String res = urlRegex.matcher("https://google.com").group();
        new Scrap("https://google.com").getUrls();
    }
}
