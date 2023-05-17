package eng.ranker;

import eng.indexer.classes.Indexer;
import eng.indexer.classes.MongoStorage;
import eng.indexer.classes.Storage;
import eng.indexer.classes.WebIndexer;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String args[]) {
        Dotenv dotenv = Dotenv.load();
        String connectionString = dotenv.get("CONN_STRING");
        Storage storage = new MongoStorage(connectionString);
        Indexer indexer = new WebIndexer(storage);
        Ranker ranker = new Ranker(indexer);

        String query = "computer science";
        System.out.println(ranker.rank(query));
    }
}