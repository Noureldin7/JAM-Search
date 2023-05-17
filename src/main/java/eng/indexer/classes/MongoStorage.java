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
    public MongoStorage(String connectionString) {                
        this.client = MongoClients.create(connectionString);
        this.db = client.getDatabase("search_engine");
        this.inverted_index = db.getCollection("inverted_index");
    }

    public void store(String url, IndexerOutput indexerOutput) {
        Hashtable<String, ArrayList<WordOccurrence>> wordList = indexerOutput.wordList;
        int wordPosition = indexerOutput.wordPosition;

        for (String word : wordList.keySet()) {
            synchronized(word.intern()) {
                FindIterable<org.bson.Document> iterable = inverted_index.find(Filters.eq("word", word));
                org.bson.Document doc = iterable.first();
                if (doc == null) {
                    doc = new org.bson.Document("word", word);
                    doc.append("invertedDocuments", new ArrayList<org.bson.Document>());
                }
                ArrayList<org.bson.Document> invertedDocuments = (ArrayList<org.bson.Document>) doc.get("invertedDocuments");
    
                org.bson.Document documentEntry = new org.bson.Document("url", url);
                documentEntry.append("wordCount", wordPosition); //TODO: make that word count instead
                
                ArrayList<org.bson.Document> wordOccurrences = new ArrayList<org.bson.Document>();
    
                for (WordOccurrence wordOccurrence : wordList.get(word)) {
                    wordOccurrences.add(new org.bson.Document("position", wordOccurrence.getPosition()).append("parentTag", wordOccurrence.getParentTag()));
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
    }
    public void update(String url, IndexerOutput indexerOutput){

    }

    public boolean contains(String url){
        return false;
    }

    public String [] retrieve(String word){
        return null;
    }
    public void clear(){
        inverted_index.drop();
        inverted_index = db.getCollection("inverted_index");
    }
}
