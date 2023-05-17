package eng.indexer.classes;


public interface Storage {
    public void store(String url, IndexerOutput indexerOutput, Boolean maintainStack);
    public boolean contains(String url);
    public void update(String url, IndexerOutput indexerOutput);
    public IndexEntry retrieve(String word);
    public int getCount();
    public void clear();
}