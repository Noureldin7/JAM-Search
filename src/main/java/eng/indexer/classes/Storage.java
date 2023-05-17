package eng.indexer.classes;

import java.util.ArrayList;
import java.util.Hashtable;

public interface Storage {
    public void store(String url, IndexerOutput indexerOutput);
    public boolean contains(String url);
    public void update(String url, IndexerOutput indexerOutput);
    public String [] retrieve(String word);
    public void clear();
}