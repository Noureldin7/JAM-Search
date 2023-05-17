package eng.util;

import java.util.ArrayList;
import org.tartarus.snowball.ext.EnglishStemmer;

public class Preprocessor {

    public static ArrayList<String> preprocess(String text) {
        // Create a new EnglishStemmer object for stemming
        EnglishStemmer stemmer = new EnglishStemmer();

        // Split input text into tokens
        String[] tokens = text.split("\\s+");

        // Apply Snowball stemming to each token
        ArrayList<String> stemmedTokens = new ArrayList<String>();
        for (String token : tokens) {
            // Remove non-alphanumeric characters
			token = token.replaceAll("[^a-zA-Z0-9]", "");

			// Stem token
			stemmer.setCurrent(token);
			stemmer.stem();
			String stemmedToken = stemmer.getCurrent();

			// Add stemmed token to list of stemmed tokens
			stemmedTokens.add(stemmedToken);
        }

        return stemmedTokens;
    }

	// // Test
	// public static void main(String[] args) {
	// 	final String text = "re-indexing indexable re-indexed re-indexes re-index re-indexing re-indexes re-indexed re-indexable re/index rein]dex";
	// 	ArrayList<String> words = preprocess(text);
	// 	System.out.println(words);
	// }
}

/* 
interface integration:
Front-end conect to java procces this java proccess is Ranker
*/