package eng.indexer.classes;

import java.util.ArrayList;
import java.util.Hashtable;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

public class MongoStorage implements Storage {
    MongoClient client;
    MongoDatabase db;
    MongoCollection<Document> inverted_index;
    MongoCollection<Document> indexer_stack;
    MongoCollection<Document> entry_count;
    public MongoStorage(String connectionString) {                
        this.client = MongoClients.create(connectionString);
        this.db = client.getDatabase("search_engine");
        this.inverted_index = db.getCollection("inverted_index");
        this.indexer_stack = db.getCollection("indexer_stack");
        this.entry_count = db.getCollection("entry_count"); // don't judge me
    }

    private void pushOutputToStack(String url, IndexerOutput indexerOutput) {
        Document doc = new Document("url", url);
        Hashtable<String, ArrayList<WordOccurrence>> wordList = indexerOutput.wordList;
        ArrayList<Document> words = new ArrayList<Document>();
        for (String word : wordList.keySet()) {
            Document wordDoc = new Document("word", word);
            ArrayList<Document> wordOccurrences = new ArrayList<Document>();
            for (WordOccurrence wordOccurrence : wordList.get(word)) {
                Document wordOccurrenceDoc = new Document("wordPosition", wordOccurrence.getPosition());
                wordOccurrenceDoc.append("parentTag", wordOccurrence.getParentTag());
                wordOccurrences.add(wordOccurrenceDoc);
            }
            wordDoc.append("wordOccurrences", wordOccurrences);
            words.add(wordDoc);
        }
        doc.append("wordList", words);
        indexer_stack.updateOne(
            Filters.eq("url", url),
            new Document("$set", doc),
            new UpdateOptions().upsert(true)
        );
    }

    private IndexerOutput popOutputFromStack(String url) {
        FindIterable<Document> iterable = indexer_stack.find(Filters.eq("url", url));
        Document doc = iterable.first();
        Hashtable<String, ArrayList<WordOccurrence>> wordList = new Hashtable<String, ArrayList<WordOccurrence>>();
        ArrayList<Document> words = (ArrayList<Document>) doc.get("wordList");
        for (Document wordDoc : words) {
            String word = wordDoc.getString("word");
            ArrayList<Document> wordOccurrences = (ArrayList<Document>) wordDoc.get("wordOccurrences");
            ArrayList<WordOccurrence> wordOccurrencesList = new ArrayList<WordOccurrence>();
            for (Document wordOccurrenceDoc : wordOccurrences) {
                int wordPosition = wordOccurrenceDoc.getInteger("wordPosition");
                String parentTag = wordOccurrenceDoc.getString("parentTag");
                wordOccurrencesList.add(new WordOccurrence(wordPosition, parentTag));
            }
            wordList.put(word, wordOccurrencesList);
        }
        indexer_stack.deleteOne(Filters.eq("url", url));
        return new IndexerOutput(0, wordList);
    }

    private void incrementCount(int delta) {
        entry_count.updateOne(
            Filters.eq("name", "count"),
            new Document("$inc", new Document("count", delta)),
            new UpdateOptions().upsert(true)
        );
    }

    public void store(String url, IndexerOutput indexerOutput, Boolean maintainStack) {
        if (maintainStack) {
            pushOutputToStack(url, indexerOutput);
        }

        Hashtable<String, ArrayList<WordOccurrence>> wordList = indexerOutput.wordList;
        int wordPosition = indexerOutput.wordPosition;

        for (String word : wordList.keySet()) {
            synchronized(word.intern()) {
                FindIterable<Document> iterable = inverted_index.find(Filters.eq("word", word));
                Document doc = iterable.first();
                if (doc == null) {
                    doc = new Document("word", word);
                    doc.append("invertedDocuments", new ArrayList<Document>());
                }
                ArrayList<Document> invertedDocuments = (ArrayList<Document>) doc.get("invertedDocuments");
    
                Document documentEntry = new Document("url", url);
                documentEntry.append("wordCount", wordPosition); //TODO: make that word count instead
                
                ArrayList<Document> wordOccurrences = new ArrayList<Document>();
    
                for (WordOccurrence wordOccurrence : wordList.get(word)) {
                    wordOccurrences.add(new Document("position", wordOccurrence.getPosition()).append("parentTag", wordOccurrence.getParentTag()));
                }
                documentEntry.append("wordOccurrences", wordOccurrences);
                invertedDocuments.add(documentEntry);
                doc.append("invertedDocuments", invertedDocuments);
                inverted_index.updateOne(
                    Filters.eq("word", word),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
                );
            }
        }
        incrementCount(1);
    }

    public void update(String url, IndexerOutput newOutput){
        IndexerOutput oldOutput = popOutputFromStack(url);
        incrementCount(-1);
        pushOutputToStack(url, newOutput);
        Hashtable<String, ArrayList<WordOccurrence>> oldWordList = oldOutput.wordList;
        for (var word : oldWordList.keySet()) {
            synchronized(word.intern()) {
                FindIterable<Document> iterable = inverted_index.find(Filters.eq("word", word));
                Document doc = iterable.first();
                if (doc == null) continue;
                ArrayList<Document> invertedDocuments = (ArrayList<Document>) doc.get("invertedDocuments");
                if (invertedDocuments == null) continue;
                for (Document invertedDocument : invertedDocuments) {
                    if (invertedDocument.getString("url").equals(url)) {
                        invertedDocuments.remove(invertedDocument);
                        break;
                    }
                }
                doc.append("invertedDocuments", invertedDocuments);
                inverted_index.updateOne(
                    Filters.eq("word", word),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
                );
            }
        }
        store(url, newOutput, true);
    }

    public boolean contains(String url) {
        FindIterable<Document> iterable = indexer_stack.find(Filters.eq("url", url));
        if (iterable.first() != null) return true;
        return false;
    }

    public IndexEntry retrieve(String word) {
        FindIterable<Document> iterable = inverted_index.find(Filters.eq("word", word));
        Document doc = iterable.first();
        if (doc == null) return null;

        ArrayList<Document> invertedDocumentsMongo = (ArrayList<Document>) doc.get("invertedDocuments");
        ArrayList<InvertedDocument> invertedDocuments = new ArrayList<InvertedDocument>();
        for (Document invertedDocument : invertedDocumentsMongo) {
            String url = invertedDocument.getString("url");
            int wordCount = invertedDocument.getInteger("wordCount");

            ArrayList<Document> wordOccurrencesMongo = (ArrayList<Document>) invertedDocument.get("wordOccurrences");

            ArrayList<WordOccurrence> wordOccurrencesList = new ArrayList<WordOccurrence>();
            for (Document wordOccurrenceDoc : wordOccurrencesMongo) {
                int wordPosition = wordOccurrenceDoc.getInteger("position");
                String parentTag = wordOccurrenceDoc.getString("parentTag");
                wordOccurrencesList.add(new WordOccurrence(wordPosition, parentTag));
            }
            
            invertedDocuments.add(new InvertedDocument(url, wordCount, wordOccurrencesList));
        }
        return new IndexEntry(word, invertedDocuments);
    }

    
    public int getCount() {
        FindIterable<Document> iterable = entry_count.find(Filters.eq("name", "count"));
        Document doc = iterable.first();
        if (doc == null) {
            doc = new Document("name", "count");
            doc.append("count", 0);
            entry_count.insertOne(doc);
        }
        return doc.getInteger("count");
    }

    public void clear() {
        inverted_index.drop();
        inverted_index = db.getCollection("inverted_index");
        indexer_stack.drop();
        indexer_stack = db.getCollection("indexer_stack");
        indexer_stack.drop();
        entry_count.updateOne(
            Filters.eq("name", "count"),
            new Document("$set", new Document("count", 0)),
            new UpdateOptions().upsert(true)
            );
    }
}