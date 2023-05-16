package eng.indexer.classes;
import org.jsoup.nodes.Document;

public interface Indexer {
    // takes in a dom representation of a webpage and its url
    // to reindex a webpage used the reindex method as the index method 
    // won't work properly if an index entry already exists
    public void index(String url, Document document);
    // takes in a dom representation of both the old and new version of a webpage and its url
    // this operation is efficient as it will update only the entries that have changed
    public void reindex(String url, Document oldDocument, Document newDocument);
    // takes in a dom representation of the new version of a webpage and its url
    // this operation is inefficient as it must scan the entire inverted index
    public void reindex(String url, Document newDocument);
    // takes in a word and returns an index entry
    public IndexEntry retrieve (String word);
    // can be used by the ranker to compute idf
    public int documentCount();
    // removes all entries
    public void clear();
}