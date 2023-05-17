package eng.ranker;

import java.util.*;
import java.util.Map.Entry;
import eng.indexer.classes.*;

public class Ranker {
    private Indexer index;

    public Ranker(Indexer index) {
        this.index = index;
    }

    private Hashtable<String, Integer> getFrequencies(ArrayList<IndexEntry> entries) {
        Hashtable<String, Integer> frequencies = new Hashtable<String, Integer>();
        for (IndexEntry entry : entries) {
            for (InvertedDocument doc : entry.getInvertedDocuments()) {
                frequencies.put(doc.getIdentifier(), frequencies.getOrDefault(doc.getIdentifier(), 0) + 1);
            }
        }
        return frequencies;
    }

    // word entries should appear in the same order as the query
    // returns null if it fails to match the phrase
    public ArrayList<IndexEntry> PhraseFilter(ArrayList<IndexEntry> entries) {
        Hashtable<String, Integer> frequencies = getFrequencies(entries);
        HashSet<String> frequentUrls = new HashSet<String>();

        for (String url : frequencies.keySet()) {
            if (frequencies.get(url) == entries.size()) {
                frequentUrls.add(url);
            }
        }

        ArrayList<IndexEntry> intersectedEntries = new ArrayList<IndexEntry>();
        for (IndexEntry entry : entries) {
            ArrayList<InvertedDocument> filteredDocs = new ArrayList<InvertedDocument>();
            for (InvertedDocument doc : entry.getInvertedDocuments()) {
                if (frequentUrls.contains(doc.getIdentifier())) {
                    filteredDocs.add(doc);
                }
            }
            if (filteredDocs.size() == 0) return null;
            intersectedEntries.add(new IndexEntry(entry.getWord(), filteredDocs));
        }

        Hashtable<String, Queue<WordOccurrence>> consecutiveDocs = new Hashtable<String, Queue<WordOccurrence>>();
        for (InvertedDocument doc : intersectedEntries.get(0).getInvertedDocuments()) {
            consecutiveDocs.put(doc.getIdentifier(), new LinkedList<>(doc.getWordOccurrences()));
        }
        
        for (int i = 1; i < intersectedEntries.size(); i++) {
            IndexEntry entry = intersectedEntries.get(i);
            for (InvertedDocument doc : entry.getInvertedDocuments()) {
                Queue<WordOccurrence> q = consecutiveDocs.get(doc.getIdentifier());
                int qSize = q.size();
                for (int j = 0; j < qSize; j++) {
                    int target = q.poll().getPosition() + 1;
                    ArrayList<WordOccurrence> wordOccurrences = doc.getWordOccurrences();

                    int result = Collections.binarySearch(
                        wordOccurrences,
                        target, 
                        (element, value) -> Integer.compare(((WordOccurrence) element).getPosition(), (int) value)
                    );

                    if (result >= 0) {
                        q.add(wordOccurrences.get(result));
                    }
                }
            }
        }

        HashSet<String> consecutiveUrls = new HashSet<String>();
        for (String url : consecutiveDocs.keySet()) {
            if (consecutiveDocs.get(url).size() != 0) {
                consecutiveUrls.add(url);
            }
        }

        ArrayList<IndexEntry> consecutiveEntries = new ArrayList<IndexEntry>();
        for (IndexEntry entry : entries) {
            ArrayList<InvertedDocument> filteredDocs = new ArrayList<InvertedDocument>();
            for (InvertedDocument doc : entry.getInvertedDocuments()) {
                if (consecutiveUrls.contains(doc.getIdentifier())) {
                    filteredDocs.add(doc);
                }
            }
            if (filteredDocs.size() == 0) return null;
            consecutiveEntries.add(new IndexEntry(entry.getWord(), filteredDocs));
        }

        return consecutiveEntries;
    }

    Comparator<Entry<String, Double>> comp = (Entry<String, Double> a, Entry<String, Double> b) -> {
        Double res = a.getValue() - b.getValue();
        if (res > 0) {
            return -1;
        } else if (res < 0) {
            return 1;
        } else {
            return 0;
        }
    };

    public ArrayList<String> rank(String[] query) {
        Hashtable<String, Double> leaderboard = new Hashtable<String,Double>();
        
        for (String word : query) {
            if(word.charAt(0)=='"')
            {
                // Phrase
                word = word.replace("\"", "");
                ArrayList<IndexEntry> indexArray = new ArrayList<IndexEntry>();
                for(String phraseWord : word.split(" "))
                {
                    IndexEntry entry = index.retrieve(phraseWord);
                    if(entry==null) return null;
                    indexArray.add(entry);
                }
                ArrayList<IndexEntry> filteredArray = PhraseFilter(indexArray);
                for (IndexEntry entry : filteredArray)
                {
                    Double idf = Math.log((double) index.documentCount() / entry.getDF());
                    for (InvertedDocument doc : entry.getInvertedDocuments()) {
                        Double oldValue = leaderboard.get(doc.getIdentifier());
                        if(oldValue == null) {
                            leaderboard.put(doc.getIdentifier(), idf * doc.getTF());
                        } else {
                            leaderboard.put(doc.getIdentifier(), oldValue + idf * doc.getTF());
                        }
                    }
                }

            }
            else
            {
                IndexEntry entry = index.retrieve(word);
                if(entry==null) continue;
                Double idf = Math.log((double) index.documentCount() / entry.getDF());
                for (InvertedDocument doc : entry.getInvertedDocuments()) {
                    Double oldValue = leaderboard.get(doc.getIdentifier());
                    if(oldValue == null) {
                        leaderboard.put(doc.getIdentifier(), idf * doc.getTF());
                    } else {
                        leaderboard.put(doc.getIdentifier(), oldValue + idf * doc.getTF());
                    }
                }
            }
        }

        ArrayList<String> rankedDocs = new ArrayList<String>();
        ArrayList<Entry<String, Double>> list = new ArrayList<Entry<String, Double>>(leaderboard.entrySet());
        list.sort(comp);
        for (Entry<String, Double> entry : list) {
            rankedDocs.add(entry.getKey());
        }
        return rankedDocs;
    }
}