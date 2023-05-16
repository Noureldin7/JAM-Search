package eng.ranker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map.Entry;

import eng.indexer.classes.IndexEntry;
import eng.indexer.classes.Indexer;
import eng.indexer.classes.InvertedDocument;

public class Ranker {
    private Indexer index;
    Ranker(Indexer index)
    {
        this.index = index;
    }
    Comparator<Entry<String,Double>> comp = (Entry<String,Double> a, Entry<String,Double> b) -> {
        Double res = a.getValue()-b.getValue();
        if(res>0)
        {
            return 1;
        }
        else if(res<0)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    };
    ArrayList<String> rank(String query)
    {
        String[] words = query.split(" ");
        Hashtable<String,Double> leaderboard = new Hashtable<String,Double>();
        for (String word : words) {
            IndexEntry entry = index.retrieve(word);
            Double idf = Math.log(index.documentCount()/entry.getDF());
            for (InvertedDocument doc : entry.getInvertedDocuments()) {
                Double oldValue = leaderboard.get(doc.getUrl());
                if(oldValue==null)
                {
                    leaderboard.put(doc.getUrl(), idf*doc.getTF());
                }
                else
                {
                    leaderboard.put(doc.getUrl(),oldValue+idf*doc.getTF());
                }
            }
        }
        ArrayList<String> rankedDocs = new ArrayList<String>();
        ArrayList<Entry<String,Double>> list = new ArrayList<Entry<String,Double>>(leaderboard.entrySet());
        list.sort(comp);
        for (Entry<String,Double> entry : list) {
            rankedDocs.add(entry.getKey());
        }
        return rankedDocs;
    }
}
