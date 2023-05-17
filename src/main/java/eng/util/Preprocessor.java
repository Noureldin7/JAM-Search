package eng.util;

import java.util.ArrayList;
import org.tartarus.snowball.ext.EnglishStemmer;

public class Preprocessor {

    public static ArrayList<String> preprocess(String text) {
        // Create a new EnglishStemmer object for stemming
        EnglishStemmer stemmer = new EnglishStemmer();

        // Split input text into tokens
        ArrayList<String> tokens = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : text.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
                if (inQuotes) {
                    sb.append(c);
                } else {
                    sb.append(c);
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (inQuotes) {
                sb.append(c);
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }

        // Combine neighboring quote tokens and apply Snowball stemming to each token
        ArrayList<String> stemmedTokens = new ArrayList<String>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("\"")) {
                if (i > 0 && i < tokens.size() - 1 && tokens.get(i - 1).length() > 0 && tokens.get(i + 1).length() > 0) {
                    stemmedTokens.add("\"" + tokens.get(i - 1) + " " + tokens.get(i + 1) + "\"");
                    i += 2;
                } else {
                    stemmedTokens.add("\"");
                }
            } else if (token.length() > 1) {
                stemmer.setCurrent(token);
                stemmer.stem();
                stemmedTokens.add(stemmer.getCurrent());
            } else {
                stemmedTokens.add(token);
            }
        }

        return stemmedTokens;
    }

    // // Test
    // public static void main(String[] args) {
    //     final String text = "the quick \"brown fox\" jumps";
    //     ArrayList<String> words = preprocess(text);
    //     System.out.println(words);
    // }
}