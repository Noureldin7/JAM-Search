package eng.indexer.classes;
import java.util.ArrayList;

public class IndexEntry {
    // The word that is indexed
    private String word;
    // Each entry is a structure that holds 
    // The url of a webpage that references this word
    // The normalized frequency of the word in that webpage (tf)
    // A list of positions, and the parent tag of the word in that webpage
    private ArrayList<InvertedDocument> invertedDocuments;

    public IndexEntry(String word, ArrayList<InvertedDocument> invertedDocuments) {
        this.word = word;
        this.invertedDocuments = invertedDocuments;
    }
    // get the word that is indexed
    public String getWord() {
        return word;
    }
    // DF is inferred from the size of the invertedDcouments list
    public int getDF() {
        return invertedDocuments.size();
    }
    // get a list of documents that reference this word
    public ArrayList<InvertedDocument> getInvertedDocuments() {
        return invertedDocuments;
    }
}