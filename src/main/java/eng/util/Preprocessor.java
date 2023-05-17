package eng.util;

import java.util.ArrayList;

public class Preprocessor {
    //TODO: Apply proper preprocessing such as stemming, removing stop words, etc.
    public static ArrayList<String> preprocess(String text) {
        String[] unprocessed = text.split("\\s+");
        //return array list
        ArrayList<String> words = new ArrayList<String>();
        for (String word : unprocessed) {
            word = word.replaceAll("[^a-zA-Z0-9]", "").trim(); //TODO: pretty shit approach fix
            if (word.isEmpty()) continue;
            word = word.toLowerCase();
            words.add(word);
        }
        return words;
    }
}
