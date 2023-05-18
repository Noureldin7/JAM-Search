package eng.indexer.classes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import eng.util.Preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.jsoup.nodes.Document;

public class WebIndexer implements Indexer {
    private static Storage storage;

    public WebIndexer(Storage _storage) {
        storage = _storage;
    }

    public void index(String url, String HTML) {
        synchronized(url.intern()) {
            ProcessHTMLReturn ret = processHTML(HTML);
            Hashtable<String, ArrayList<WordOccurrence>> wordList = ret.wordList;
            int wordPosition = ret.wordCount;
            IndexerOutput indexerOutput = new IndexerOutput(wordPosition, wordList);

            if (storage.contains(url)) {
                System.out.println("Re-Indexing " + url);
                storage.update(url, indexerOutput);
            } else {
                storage.store(url, indexerOutput);
            }
        }
    }

    public IndexEntry retrieve(String word) {
        return storage.retrieve(word);
    }

    public long documentCount() {
        return storage.getCount();
    }

    public void clear() {
        synchronized(this) {
            storage.clear();
        }
    }
    
    private class ProcessHTMLReturn{
        public Hashtable<String, ArrayList<WordOccurrence>> wordList;
        public int wordCount;
        public ProcessHTMLReturn(Hashtable<String, ArrayList<WordOccurrence>> wordList, int wordCount) {
            this.wordList = wordList;
            this.wordCount = wordCount;
        }
    }

    private ProcessHTMLReturn processHTML(String HTML) {
        Hashtable<String, ArrayList<WordOccurrence>> wordList = new Hashtable<String, ArrayList<WordOccurrence>>();
        Document document = Jsoup.parse(HTML);
        
        ProcessTextReturn ret = new ProcessTextReturn();
        try {
            String keywords = document.select("meta[name=keywords]").get(0).attr("content");
            if (!keywords.trim().isEmpty())
                processText(keywords, "meta", wordList, ret);
                ret.wordPosition += 1;
        } catch (Exception e) { }

        try {
            String description = document.select("meta[name=description]").get(0).attr("content");
            if (!description.trim().isEmpty())
                processText(description, "meta", wordList, ret);
                ret.wordPosition += 1;
        } catch (Exception e) { }

        try {
            String title = document.title();
            if (!title.trim().isEmpty())
                processText(title, "title", wordList, ret);
                ret.wordPosition += 1;
        } catch (Exception e) { }

        document.getElementsByTag("meta").remove();
        document.getElementsByTag("script").remove();
        document.getElementsByTag("style").remove();
        document.getElementsByTag("noindex").remove();
        document.getElementsByTag("link").remove();
        document.getElementsByTag("img").remove();
        document.getElementsByTag("iframe").remove();
        document.getElementsByTag("button").remove();
        document.getElementsByTag("textarea").remove();
        document.getElementsByTag("svg").remove();

        extractWords(document.body(), wordList, ret);

        return new ProcessHTMLReturn(wordList, ret.wordCount);
    }

    private class ProcessTextReturn {
        public int wordPosition;
        public int wordCount;
        public ProcessTextReturn() {
            this.wordPosition = 0;
            this.wordCount = 0;
        }
    }

    private static final List<String> BLOCK_TAGS = Arrays.asList("p", "h1", "h2", "h3", "h4", "h5", "h6");
    private static void processText(String text, String parentType, Hashtable<String, ArrayList<WordOccurrence>> wordList, ProcessTextReturn ret) {
        String [] sentences = text.split("\\n");
        for (String sentence : sentences) {
            ArrayList<String> words = Preprocessor.preprocess(sentence);
            for (String word : words) {
                if (wordList.get(word) == null) wordList.put(word, new ArrayList<WordOccurrence>());
                wordList.get(word).add(new WordOccurrence(ret.wordPosition++, parentType));
                ret.wordCount++;
            }
            ret.wordPosition++;
        }
        ret.wordPosition--;
    }

    private static void extractWords(Element element, Hashtable<String, ArrayList<WordOccurrence>> wordList, ProcessTextReturn ret) {
        for (var child : element.childNodes()) {
            if (child instanceof Element) {
                extractWords((Element)child, wordList, ret);
            } else if (child instanceof org.jsoup.nodes.TextNode) {
                String text = ((org.jsoup.nodes.TextNode)child).text();
                if (text.trim().isEmpty()) continue;
                String parentType = element.tagName();
                processText(text, parentType, wordList, ret);
                if (BLOCK_TAGS.contains(element.nodeName())) {
                    ret.wordPosition++;
                }
            }
        }
    }
}