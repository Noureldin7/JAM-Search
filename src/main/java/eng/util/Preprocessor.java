package eng.util;

import java.util.ArrayList;
import org.tartarus.snowball.ext.EnglishStemmer;

public class Preprocessor {

    public static ArrayList<String> preprocess(String text) {
        // Create a new EnglishStemmer object for stemming
        EnglishStemmer stemmer = new EnglishStemmer();
        text = text.toLowerCase();
        // Split input text into tokens
        // Tracking whether we are inside quotes or not and adding tokens accordingly
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
        // Add the last token
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }

        // Print tokens
        // System.out.println(tokens);
        
        // [the, quick, "brown fox", jump]

        ArrayList<String> stemmedTokens = new ArrayList<String>();
        for (int i = 0; i < tokens.size(); i++) {
            // Stem what is inside quotes
            

            String token = tokens.get(i);
            if (token.charAt(0) == '"'){
                token = token.substring(1, token.length()-1);
                String temp = "\"";
                for (String word : token.split(" ")) {
                    stemmer.setCurrent(word);
                    stemmer.stem();
                    temp += stemmer.getCurrent() + " ";    
                }
                temp = temp.substring(0, temp.length()-1);
                temp += "\"";
                stemmedTokens.add(temp);
                continue;
            }
            stemmer.setCurrent(token);
            stemmer.stem();
            stemmedTokens.add(stemmer.getCurrent());
        }

        return stemmedTokens;
    }

    // // Test
    // public static void main(String[] args) {
    //     final String text = "the quick \"browning fox\" jumps";
    //     ArrayList<String> words = preprocess(text);
    //     System.out.println(words);
    // }
}