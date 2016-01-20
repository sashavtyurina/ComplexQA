//opennlp
import opennlp.tools.tokenize.SimpleTokenizer;

//util
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


// io
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Utils {
  private static Vector<String> stopwords;

public static Vector<String> tokenizeAndClean(String text, boolean lower, boolean nopunct, boolean shrink_rep, 
    boolean drop_stop, boolean stem) {
    if (lower) {
      text = text.toLowerCase();
    }

    if (nopunct) {
      //Remove all punctuation marks 
      text = removePunct(text);
    }

    // Replace chars repeating 3 or more times with the same char repeated once
    if (shrink_rep){
      text = shrinkRepeatedChars(text);
    }

    //tokenize question
    SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    Vector<String> tokens = new Vector<String>(Arrays.asList(tokenizer.tokenize(text)));

      if (drop_stop) {
        // drop stop words
        dropStopWords(tokens);
      }

      Vector<String> stemmed = new Vector<String>(tokens);
      if (stem) {
       // Light stemming to get rid of plurals
        stemmed = s_stemmer(tokens);
      }


      return stemmed;
  }

  private static String removePunct(String str) {
    return str.replaceAll("[^A-Za-z\\s\\d]", "");
  }

  private static String shrinkRepeatedChars(String str) {
    Pattern p = Pattern.compile("(.)\\1{2,}");
    Matcher m = p.matcher(str);
    String res = m.replaceAll("$1"); 
    return res;
  }

  private static Vector<String> dropStopWords(Vector<String> tokens) {
    tokens.removeAll(stopwords);
    return tokens;
  }

  private static void loadStopWords() throws FileNotFoundException, IOException {
      stopwords = new Vector<String>();
      FileInputStream fstream = new FileInputStream("stop_words.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
      String strLine;
      
      while ((strLine = br.readLine()) != null)   {
          stopwords.add(strLine.trim().toLowerCase());
      }
      br.close();
  }

  private static Vector<String> s_stemmer(Vector<String> tokens) {
    Vector<String> stemmed = new Vector<String>();
    for (int i = 0; i < tokens.size(); ++i) {
      String word = tokens.get(i);
      StringBuilder b = new StringBuilder(word);

      if ((word.endsWith("ies")) && (!word.endsWith("eies")) && (!word.endsWith("aies"))) {
        b.replace(word.lastIndexOf("ies"), word.length(), "y");
      } else if ((word.endsWith("es")) && (!word.endsWith("aes")) && (!word.endsWith("ees")) && (!word.endsWith("oes"))) {
        b.replace(word.lastIndexOf("es"), word.length(), "e");
      } else if ((word.endsWith("s")) && (!word.endsWith("us")) && (!word.endsWith("ss"))) {
        b.replace(word.lastIndexOf("s"), word.length(), "");
      }

      // get rid of empty tokens and single character tokens
      if (!(b.toString() == null || b.toString().trim().equals("") || b.toString().trim().length() == 1)) {
        stemmed.add(b.toString());  
      }
    }
    return stemmed;
  }
}