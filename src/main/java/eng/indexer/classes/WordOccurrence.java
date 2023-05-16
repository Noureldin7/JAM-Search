package eng.indexer.classes;

public class WordOccurrence {
    // The position of the word in the document
    private int position;
    // The parent tag of the associated word
    private String parentTag;

    public WordOccurrence(int position, String parentTag) {
        this.position = position;
        this.parentTag = parentTag;
    }
    // get the position of the word in the document
    public int getPosition() {
        return position;
    }
    // get the parent tag of the word
    public String getParentTag() {
        return parentTag;
    }
}