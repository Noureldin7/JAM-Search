package eng.indexer.classes;

import java.util.ArrayList;
import java.util.Hashtable;

public class IndexerOutput {
    // The word that is indexed
    public int wordCount;
    public Hashtable<String, ArrayList<WordOccurrence>> wordList;
    IndexerOutput(int wordCount, Hashtable<String, ArrayList<WordOccurrence>> wordList){
        this.wordCount = wordCount;
        this.wordList = wordList;
    }
}