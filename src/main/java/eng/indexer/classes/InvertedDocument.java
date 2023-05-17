package eng.indexer.classes;
import java.util.ArrayList;

public class InvertedDocument {
    private String identifier;
    // The total number of words in the document
    private int wordCount;
    private ArrayList<WordOccurrence> wordOccurrences;
    
    public InvertedDocument(String url, int wordCount, ArrayList<WordOccurrence> WordsInfo) {
        this.identifier = url;
        this.wordCount = wordCount;
        this.wordOccurrences = WordsInfo;
    }
    // get the url of a webpage that references this word
    public String getIdentifier() {
        return identifier;
    }
    // get the normalized frequency of the word in that webpage (tf)
    public double getTF() {
        return (double) wordOccurrences.size() / wordCount;
    }
    // get a list of sorted positions, and the parent tag of the word in that webpage
    public ArrayList<WordOccurrence> getWordOccurrences() {
        return wordOccurrences;
    }
}    