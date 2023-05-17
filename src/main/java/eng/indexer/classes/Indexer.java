package eng.indexer.classes;

public interface Indexer {
    // takes in a dom representation of a webpage and its identifier
    public void index(String identifier, String document);
    // takes in a dom representation of a webpage and its identifier
    // repeated indexing of the same webpage is not allowed
    // clear method must be called before-hand
    public void batchIndex(String identifier, String document);
    // takes in a word and returns an index entry
    public IndexEntry retrieve (String word);
    // can be used by the ranker to compute idf
    public int documentCount();
    // removes all entries
    public void clear();
}