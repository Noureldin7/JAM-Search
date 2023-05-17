package eng.indexer.classes;

import java.util.ArrayList;
import java.util.Hashtable;

public class IndexerOutput {
    // The word that is indexed
    public int wordPosition;
    public Hashtable<String, ArrayList<WordOccurrence>> wordList;
    IndexerOutput(int wordPosition, Hashtable<String, ArrayList<WordOccurrence>> wordList){
        this.wordPosition = wordPosition;
        this.wordList = wordList;
    }
}