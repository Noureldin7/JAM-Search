package eng.util;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import crawlercommons.robots.SimpleRobotRulesParser;


public class Scrap {
    static String httpsPattern = "(https?://)";
    static String wwwPattern = "(www\\.)?";
    static String domainNamePattern = "[-a-z]{1,63}(\\.[-a-z]{1,63})*";
    static String tldPattern = "(\\.[a-z]{2,6})+";
    static String routePattern = "((/[-_a-z]+)*(.html)?|/)";
    static String domainPattern = httpsPattern+wwwPattern+domainNamePattern+tldPattern;
    static String urlPattern = httpsPattern+wwwPattern+domainNamePattern+tldPattern+routePattern;
    static Pattern urlRegex = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
    static Pattern domainRegex = Pattern.compile(domainPattern,Pattern.CASE_INSENSITIVE);
    public Document page;
    SimpleRobotRulesParser robots;
    public Scrap(String url) throws Exception{
        this.robots = new SimpleRobotRulesParser();
        this.page = Jsoup.connect(url).timeout(4000).get();
        // this.domainRegex = Pattern.compile(domainPattern,Pattern.CASE_INSENSITIVE);
        // this.urlRegex = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
    }
    public boolean robotsAllow(String url) throws Exception{
        Matcher m = domainRegex.matcher(url);
        m.find();
        String robotUrl = m.group()+"/robots.txt";
        try {
            byte[] content = IOUtils.toByteArray(new URL(robotUrl).openStream());
            if(robots.parseContent(robotUrl, content, "text/plain", "*").isAllowed(url)){
                return true;
            }
            else{
                return false;
            }
        } catch (Exception e) {
            return true;
        }
    }
    public List<String> getUrls(){
        Predicate<String> filterPredicate = (String url) -> {
            if(!urlRegex.matcher(url).matches()) return true;
            return false;
        };
        Consumer<String> normalizeDomain = (String url) -> {
            Matcher mat = domainRegex.matcher(url);
            mat.find();
            String dom = mat.group().toLowerCase();
            //DONE Filter out slash
            int endIndex = url.length();
            if(url.endsWith("/"))
            {
                endIndex--;
            }
            url = dom+url.substring(endIndex);
        };
        List<String> urls = page.body().getElementsByTag("a").eachAttr("href");
        urls.removeIf(filterPredicate);
        urls.forEach(normalizeDomain);
        Collections.shuffle(urls);
        if(urls.size()>10)
        {
            return urls.subList(0, 10);
        }
        return urls;
    }
    public String getBodyHtml(){
        return page.body().html();
    }
    public String getUrlHash() throws Exception{
        Elements head = page.head().select("meta,title");
        Element body = page.body();
        body.select("script,style").remove();
        String importantHtml = head.text()+body.text();
        return Hash.encrypt(importantHtml, "SHA-1");
    }

    public static void main(String args[]) throws Exception{
        // Pattern urlRegex = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        // Matcher mat = urlRegex.matcher("https://twitter.com/PLinUSA");
        // mat.find();
        // String res = mat.group();
        // String url = "htTp://tWitter.Com/PLinUSA";
        // Matcher mat = domainRegex.matcher(url);
        // mat.find();
        // String dom = mat.group().toLowerCase().replace("https", "http");
        // url = dom+url.substring(mat.end());
        System.out.println(new Scrap("https://www.wikipedia.com").getUrls());
    }
}
