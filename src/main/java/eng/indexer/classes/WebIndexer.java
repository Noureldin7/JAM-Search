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
        
        int wordPosition = 0;
        try {
            String keywords = document.select("meta[name=keywords]").get(0).attr("content");
            if (!keywords.trim().isEmpty())
                wordPosition = processText(keywords, "meta", wordList, wordPosition) + 1;
        } catch (Exception e) { }

        try {
            String description = document.select("meta[name=description]").get(0).attr("content");
            if (!description.trim().isEmpty())
                wordPosition = processText(description, "meta", wordList, wordPosition) + 1;
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

        wordPosition = extractWords(document.body(), wordList, wordPosition);

        int blockTagcount = 0;
        for (String tag : BLOCK_TAGS) {
            blockTagcount += document.select(tag).size();
        }
        return new ProcessHTMLReturn(wordList, wordPosition - blockTagcount);
    }

    private static final List<String> BLOCK_TAGS = Arrays.asList("p", "h1", "h2", "h3", "h4", "h5", "h6");
    private static int processText(String text, String parentType, Hashtable<String, ArrayList<WordOccurrence>> wordList, int wordPosition) {
        String [] sentences = text.split("\\n");
        for (String sentence : sentences) {
            ArrayList<String> words = Preprocessor.preprocess(sentence);
            for (String word : words) {
                if (wordList.get(word) == null) wordList.put(word, new ArrayList<WordOccurrence>());
                wordList.get(word).add(new WordOccurrence(wordPosition++, parentType));
            }
            wordPosition++;
        }
        return wordPosition - 1;
    }

    private static int extractWords(Element element, Hashtable<String, ArrayList<WordOccurrence>> wordList, int wordPosition) {
        for (var child : element.childNodes()) {
            if (child instanceof Element) {
                wordPosition = extractWords((Element)child, wordList, wordPosition);
            } else if (child instanceof org.jsoup.nodes.TextNode) {
                String text = ((org.jsoup.nodes.TextNode)child).text();
                if (text.trim().isEmpty()) continue;
                String parentType = element.tagName();
                wordPosition = processText(text, parentType, wordList, wordPosition);
                if (BLOCK_TAGS.contains(element.nodeName())) {
                    wordPosition++;
                }
            }
        }
        return wordPosition;
    }
}