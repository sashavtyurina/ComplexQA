import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
// import org.apache.lucene.codecs.BlockTreeTermsReader.FieldReader;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.chunker.*;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import org.json.*;
import java.util.Scanner;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Map.*;
import java.util.ArrayList;
import java.util.*;
import java.lang.Math.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.Math;
import java.util.Random;
import java.io.File;
import java.io.FileOutputStream;

import java.text.DecimalFormat;

import org.apache.lucene.document.Document;


public class KeywordRanking {

  private static Vector<String> stopwords;
  private static LuceneHelper luc; 
  private static int max_query_length = 5;

  public static class WordRanking {
    // a class to track and afterwards rank query words based on how often they appear in good queries

    // the word itself
    public String word;

    // a list of average kld scores a query with the word in it has got
    public Vector<Double> ave_kld;

    // KLD score averaged across all the documents
    public double average_kld;

    public WordRanking(String word) {
      this.word = word;
      this.ave_kld = new Vector<Double>();
      this.average_kld = -1;
    }

    public void addKLDScore(double score) {
      this.ave_kld.add(new Double(score));
    }

    public void findAveKLD(){
      double sum = 0;
      for (Double d : this.ave_kld) {
        sum += d.doubleValue();
      }
      this.average_kld = sum / this.ave_kld.size();
    }

    // given an average KLD score across all the documents, how many of those were retrieved
    // using a query that contained this word
    public int countDocsBelowAverage(double average) {
      int count = 0;
      for (Double score : this.ave_kld) {
        if (score < average) {
          count ++;
        }
      }
      return count;
    }

    public String toString() {
      return this.word + " - " + this.ave_kld.toString();
    }
  }

  static <K,V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

    List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());

    Collections.sort(sortedEntries, 
            new Comparator<Entry<K,V>>() {
                @Override
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            }
    );

    return sortedEntries;
  }

  public static void write_query_ranking(HashMap<String, Double> queries, boolean toFile, String filename) throws FileNotFoundException{
    List<Entry<String, Double>> sorted = entriesSortedByValues(queries);

    if (toFile) {
      // PrintWriter writer = new PrintWriter(filename);
      PrintWriter writer = new PrintWriter(new FileOutputStream(new File(filename),true));

      for (Entry<String, Double> e : sorted) {
        writer.println(e.toString());
      }
      writer.close();
    } else {
      for (Entry<String, Double> e : sorted) {
        System.out.println(e.toString());
      }
    }
  }

  public static Set <String> create_vocabulary(String text1, String text2) {
      SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
      String[] tokens1 = tokenizer.tokenize(text1);
      String[] tokens2 = tokenizer.tokenize(text2);
      Set <String> vocab1 = new HashSet<String>(Arrays.asList(tokens1));
      Set <String> vocab2 = new HashSet<String>(Arrays.asList(tokens2));
      Set <String> vocabulary = new HashSet<String>(vocab1);
      vocabulary.addAll(vocab2);

      // System.out.println(vocabulary.toString());

      return vocabulary;
  }

  public static Vector<Integer> vectorize(Set<String> vocabulary, String text) {
      SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
      Vector<String> tokens = new Vector<String>(Arrays.asList(tokenizer.tokenize(text)));

      Vector<Integer> vectorized = new Vector<Integer>();
      for (String s : vocabulary) {
          if (tokens.contains(s)) {
              vectorized.add(1);
          }
          else {
              vectorized.add(0);   
          }
      }
      return vectorized;
  }

  public static double cosine_sim(Vector<Integer> v1, Vector<Integer> v2) {
      double dot_product = 0;
      double norm_v1 = 0;
      double norm_v2 = 0;

      if (v1.size() != v2.size()) {
          System.out.println("Bad vectors of unequal size");
      }

      for (int i = 0; i < v1.size(); ++i) {
          dot_product += v1.get(i) * v2.get(i);

          norm_v1 += v1.get(i) * v1.get(i);
          norm_v2 += v2.get(i) * v2.get(i);
      }
      norm_v1 = Math.sqrt(norm_v1);
      norm_v2 = Math.sqrt(norm_v2);

      double similarity = dot_product / (norm_v1 * norm_v2);

      // System.out.println(similarity);
      return similarity;
  }

  private static boolean containsQuery(Vector<String> vect, String str) {
    Vector<String> t2 = new Vector<String>(Arrays.asList(str.split("\\s")));
    for (String s : vect) {
      Vector<String> t1 = new Vector<String>(Arrays.asList(s.split("\\s")));

      if (equalVect(t1, t2)) {
        return true;
      }
    }
    return false;
  }

  private static boolean equalVect(Vector<String> v1, Vector<String> v2) {

    for (String s : v1) {
      if (! v2.contains(s)) {
        return false;
      }
    }
    return true;
  }

  private static HashSet<HashSet<String>> composeQueriesSet(Vector<String> tokens, int maxQueryLength) {
    HashSet<HashSet<String>> existingCominations = new HashSet<HashSet<String>>();
    HashSet<HashSet<String>> prevSequence = new HashSet<HashSet<String>>();
    for (String t : tokens) {
      HashSet<String> tt = new HashSet<String>();
      tt.add(t);
      prevSequence.add(tt);
    }

    for (int i = 0; i < maxQueryLength; ++i) {
      HashSet<HashSet<String>> newCombinations = new HashSet<HashSet<String>>();
      for (String t : tokens) {
        for (HashSet<String> ps : prevSequence) {
          if (! ps.contains(t)){ // avoid "ab + a"
            HashSet<String> newComb = new HashSet<String> (ps);
            newComb.add(t);
            if (! newCombinations.contains(newComb)){
              newCombinations.add(newComb);
            }
          }
        }
      }
      existingCominations.addAll(prevSequence);
      prevSequence = newCombinations;
    }
    return existingCominations;
  }

  private static Vector<String> composeQueries(Vector<String> tokens, int maxQueryLength) {

      Vector<String> existingCombinations = new Vector<String>();
      Vector<String> prevSequence = new Vector<String>(tokens);
      System.out.println("Start composing queries");
      // System.out.println(tokens);

      for (int i = 0; i < maxQueryLength; ++i) {
          Vector<String> newCombinations = new Vector<String>();
          for (int ii = 0; ii < tokens.size(); ++ii) {
              for (int iii = 0; iii < prevSequence.size(); ++iii) {
                  // don't want to have duplicate words in our query

                  if (! (prevSequence.get(iii).contains(tokens.get(ii)))) {
                    String new_query = prevSequence.get(iii) + " " + tokens.get(ii);
                    // String new_query = prevSequence.get(iii) + tokens.get(ii);
                    if (! containsQuery (newCombinations, new_query)) {
                      newCombinations.add(new_query);
                    }
                  }       
              }
          }
          existingCombinations.addAll(prevSequence);
          prevSequence = newCombinations;
      }
      System.out.println("Done composing queries");
      // System.out.println(existingCombinations);
      return existingCombinations;
  }

  private static HashMap<Integer, Vector<String>> composeQueries1(Vector<String> tokens, int maxQueryLength) {
    // every vector<string> corresponds to a length of all queries in this vector
    // i.e. 1 -Vector<of all queries of length 1>, 2 - Vector<of all queries of length 2>, ...

    HashMap<Integer, Vector<String>> res = new HashMap<Integer, Vector<String>>();
    HashMap<String, Integer> queries = new HashMap<String, Integer>();
    for (int i = 0; i < tokens.size(); ++i) {
      queries.put(tokens.get(i), new Integer(i));
    }
    res.put(new Integer(1), new Vector<String>(queries.keySet()));


    // HashMap<String, Integer> all_queries = new HashMap<String, Integer> (queries);
    HashMap<String, Integer> new_queries = new HashMap<String, Integer> (queries);
    for (int i = 1; i < maxQueryLength; ++i ) {
      new_queries = addOne(new_queries, tokens);  
      // all_queries.putAll(new_queries);
      res.put(new Integer(i+1), new Vector<String>(new_queries.keySet())); 
    }

    return res;
  }

  private static HashMap<String, Integer> addOne(HashMap<String, Integer> queries, Vector<String> tokens) {

    HashMap<String, Integer> new_queries = new HashMap<String, Integer>();
    for (Entry<String, Integer> q : queries.entrySet()) {
      for (int i = q.getValue().intValue() + 1; i < tokens.size(); ++i){
        String qq = q.getKey() + " " + tokens.get(i);
        new_queries.put(qq, Math.max(q.getValue().intValue(), i));
      }
    }
    return new_queries;
  }

  private static String join_vector(Vector<String> vect, String delim) {
    String res = "";
    for (int i = 0; i < vect.size() - 1; ++i) {
      res += vect.get(i) + delim;
    }
    res += vect.get(vect.size() - 1);
    return res;
  }

  private static Vector<String> pick_queries_of_length(Vector<String> queries, int query_length) {
    Vector<String> res = new Vector<String>();
    for (int i = queries.size() - 1; i >= 0; --i) {
      int q_lenth = queries.get(i).split("\\s").length;
      if (q_lenth == query_length) {
        res.add(queries.get(i));
      }
    }
    return res;  
  }
  private static Vector<String> pickRandomSample(Vector<String> queries, int sample_size){
    // // first select all the queries of the length mazQueryLength
    // Vector<String> long_queries = new Vector<String>();
    
    // Vector<String> queries = pick_queries_of_length(all_queries, maxQueryLength);


    // for (int i = queries.size() - 1; i >= 0; --i) {
    //   Vector<String> query_tokens = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));
    //   if (query_tokens.size() < maxQueryLength) {
    //     break;
    //   }
    //   long_queries.add(join_vector(query_tokens, " "));
    // }

    // System.out.println(long_queries);
    // now long_queries is a vector of queries of size maxQueryLength. We will randomly pick a sample of 100 queries
    Random rand = new Random();
    // int max_sample_size = 100;
    int min = 0;
    int max = queries.size();
    Vector<String> randomSample = new Vector<String>();

    if (max <= sample_size) {
      return queries;
    }

    int count = 0;
    while (count < sample_size) {
      int randomNum = rand.nextInt((max - min)) + min;
      // System.out.print(randomNum + "; ");
      String query = queries.get(randomNum);
      if (randomSample.contains(query)) {
        continue;
      } else {
        randomSample.add(query);
        count ++;
      }
    }


    return randomSample;
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

  private static Vector<String> dropStopWords(Vector<String> tokens) {
    tokens.removeAll(stopwords);
    return tokens;
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

  private static String shrinkRepeatedChars(String str) {
    Pattern p = Pattern.compile("(.)\\1{2,}");
    Matcher m = p.matcher(str);
    String res = m.replaceAll("$1"); 
    return res;
  }

  private static String removePunct(String str) {
    return str.replaceAll("[^A-Za-z\\s\\d]", "");
  }

  private static Vector<String> removeDuplicateTokens(Vector<String> tokens) {
    Vector<String> result = new Vector<String>();
    for (int i = 0; i < tokens.size(); ++i) {
      if (!(result.contains(tokens.get(i)))) {
        result.add(tokens.get(i));
      }
    }
    return result;
  } 

  private static Vector<String> tokenizeAndClean(String text, boolean lower, boolean nopunct, boolean shrink_rep, 
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

  private static double KLD_JelinekMercerSmoothing(Vector<String> foreground, Vector<String> background, float lambda) 
  throws IOException, ParseException {

    HashMap<String, Double> fore_distr = buildDistribution(foreground);
    HashMap<String, Double> back_distr = buildDistribution(background);

    HashMap<String, Double> all_distr = new HashMap<String, Double> (fore_distr);
    all_distr.putAll(back_distr);
    // System.out.println(foreground.size());
    // System.out.println(background.size());
    // System.out.println("FOREGROUND:");
    // System.out.println(fore_distr.keySet());
    // System.out.println("BACKGROUND:");
    // System.out.println(back_distr.keySet());

    double kld_value = 0;
    Scanner input = new Scanner(System.in);
    for (String token : all_distr.keySet()) {
    // for (String token : fore_distr.keySet()) {
      

      double pi = 0;
      if (fore_distr.containsKey(token)) {
        pi = (double)(fore_distr.get(token));
      } 


      double ci = (double)(luc.totalTermFreq(token)) / (double)(luc.totalTerms());
      
      double qi = 0;
      if (back_distr.containsKey(token)) {
        qi = (double)(back_distr.get(token)); // / (double)(background.size());
      }
      if (ci == 0.0) {
        
        continue;
      }
      double smoothed_pi = lambda * pi + (1 - lambda) * ci;
      double smoothed_qi = lambda * qi + (1 - lambda) * ci;

      // System.out.println(qi);
      // System.out.println(qi_c);
      // System.out.println(smoothed_qi);

      
      double add = smoothed_pi * Math.log10(smoothed_pi / smoothed_qi);
      
      if (Double.isInfinite(add)) {
        // System.out.println(token);
        // System.out.println(pi);
        // System.out.println(luc.totalTermFreq(token));
        // System.out.println(qi);
        // System.out.println(qi_c);
        // System.out.println(smoothed_qi);
        // System.out.println("***");
        // input.next();
      } else {
        kld_value += add;
        if (Double.isNaN(kld_value)) {
          System.out.println("Token = " + token);
        System.out.println("PI = " + smoothed_pi);
        System.out.println("CI = " + ci);
        System.out.println("QI = " + qi);
        input.next();
        }
      }

      // String cmd = input.nextString();
      // if (cmd.equals("nextdoc")) {
      //   return kld_value;
      // }
    }
    return kld_value;
  }

  private static double similarityNoFreq(Vector<String> foreground, Vector<String> background){
    double score = 0;
    HashSet<String> words = new HashSet<String>();
    words.addAll(background);
    for (String token : words) {
      if (foreground.contains(token)) {
        score += 1;
      }
    }
    score /= words.size();
    return score;
  }

  private static HashMap<String, Double> buildDistribution(Vector<String> tokens) {
    HashMap<String, Double> dict = new HashMap<String, Double>();
    for (int i = 0; i < tokens.size(); ++i) {
      String token = tokens.get(i);
      if (dict.containsKey(token)) {
        dict.put(token, dict.get(token) + 1);
      } else {
        dict.put(token, new Double(1.0));
      }
    }

    // after we calculated the words counts, divide them all by the length of the document to get probabilities
    int length = tokens.size();
    for (Entry<String, Double> e : dict.entrySet()) {
      e.setValue(e.getValue() / length);
    }

    return dict;
  }

    // The method that prints all possible strings of length k.  It is
    //  mainly a wrapper over recursive function printAllKLengthRec()
    // static void printAllKLength(char set[], int k) {
    static void printAllKLength(Vector<String> set, int k) {
        int n = set.size();        
        printAllKLengthRec(set, "", n, k);
    }
 
    // The main recursive method to print all possible strings of length k
    static void printAllKLengthRec(Vector<String> set, String prefix, int n, int k) {
         
        // Base case: k is 0, print prefix
        if (k == 0) {
            System.out.println(prefix);
            return;
        }
 
        // One by one add all characters from set and recursively 
        // call for k equals to k-1
        for (int i = 0; i < n; ++i) {
          if (prefix.contains(set.get(i))) {
            // Next character of input added
            continue;
          }
          String newPrefix = prefix + set.get(i); 
             
            // k is decreased, because we have added a new character
            printAllKLengthRec(set, newPrefix, n, k - 1); 
        }
    }


    static  Vector<String> cc(Vector<String> tokens, int length) {
      Vector<String> res = new Vector<String> ();
      // res.addAll(withPrefix("", tokens));
      // for (int i = 0; i <  tokens.size(); ++i) {
      Vector<String> tokens_copy = new Vector<String> (tokens);
      for (String t : tokens) {

        // String t = tokens.get(i);
        res.add(t);
        Vector<String> l2 = withPrefix(t, tokens_copy);
        Vector<String> tokens_copy1 = new Vector<String> (l2);
        for (String tt : l2) {

          Vector<String> l3 = withPrefix(tt, tokens_copy);
          res.addAll(l3);
          tokens_copy1.remove(tt);
        }
        res.addAll(withPrefix(t, tokens_copy));
        tokens_copy.remove(t);
      }
      
      return res;
    }
    static Vector<String> withPrefix(String prefix, Vector<String> tokens) {
      Vector<String> res = new Vector<String>();

      for ( int i = 0; i < tokens.size(); ++i ) {
        String t = tokens.get(i);
        if (! prefix.contains(t)) {
          res.add(prefix + t);
        }
      }
      return res;
    }

    static void write_word_distrinbution2json(HashMap<String, HashMap<Double, Integer>> distr, String filename, 
      double max_value, double min_value) throws FileNotFoundException, JSONException {
      PrintWriter writer = new PrintWriter(new FileOutputStream(new File(filename),true));
      DecimalFormat decimalFormat = new DecimalFormat("#.#");

      for (Entry<String, HashMap<Double, Integer>> e : distr.entrySet()) {
        JSONObject record = new JSONObject();
        record.put("word", e.getKey());

        JSONArray bins = new JSONArray();
        HashMap<Double, Integer> existing_bins = e.getValue();
        for (double i = min_value; i < max_value + 0.1; i += 0.1) {
          i = Double.parseDouble(decimalFormat.format(i));
          JSONObject bin = new JSONObject();
          bin.put("bin", i);
          if (existing_bins.containsKey(i)) {
            bin.put("count", existing_bins.get(i));
          } else {
            bin.put("count", 0);
          }
          bins.put(bin);
        }
        record.put("distr", bins);
        writer.println(record.toString());

      }
      writer.close();
    }


    static Vector<Vector<String>> createNGrams(Vector<String> tokens, int n) {
      Vector<Vector<String>> all_ngrams = new Vector<Vector<String>>();

      for (int i = 0; i < tokens.size() - n + 1; ++i) {
        Vector<String> ngram = new Vector<String>();
        for (int j = 0; j < n; ++j) {
          ngram.add(tokens.get(i + j));
        }
        all_ngrams.add(ngram);
      }
      // System.out.println(all_ngrams);
      return all_ngrams;
    }

    static Vector<Vector<String>> ngramIntersection(Vector<Vector<String>> eval, Vector<Vector<String>> groundTruth) {
      Vector<Vector<String>> matchedNgrams = new Vector<Vector<String>>();
      for (Vector<String> ng : groundTruth) {
        if (eval.contains(ng)) {
          matchedNgrams.add(ng);
        }
      }
      return matchedNgrams;
    }

    static void wordFreqInTopNQueries(List<Entry<String, Double>> sortedQueries, int n) {
      // sortedQueries - sorted by increasing KLD score  (good queries are the first ones - with low KLD)
      Vector<String> topTokens = new Vector<String>();
      for (int i = 0; i < n; ++i) {
        topTokens.addAll(Arrays.asList(sortedQueries.get(i).getKey().split("\\s")));
      }

      List<Entry<String, Double>> sortedWords = entriesSortedByValues(buildDistribution(topTokens));
      for (Entry<String, Double> w : sortedWords) {
        System.out.println(w.getKey() + " - " + w.getValue());
      }
    }

    static void LocateNextMCover() {}

static public String FIELD_BODY = "contents"; // primary field name where all the text is stored
static public String FIELD_ID = "id";

  public static void main(String[] args) throws IOException, ParseException, JSONException, FileNotFoundException, ParseException {
      if (args.length < 2) {
          // Vector<String> a = new Vector<String> ();
          // a.add("a");
          // a.add("b");
          // a.add("c");
          // Vector<String> b = new Vector<String>();
          // b.add("d");
          // b.add("b");
          // b.add("c");

          // Vector<Vector<String>> ang = createNGrams(a, 1);
          // Vector<Vector<String>> bng = createNGrams(b, 1);
          // System.out.println(ngramIntersection(ang, bng));



          System.out.println("Input arguments: index_path, query string");
          return;
      }
      DecimalFormat decimalFormat = new DecimalFormat("#.#");

      String filename = "query_ranking.txt";
      String index_path = args[0].toString();
      String query = args[1].toString();
      int query_num = 100;

      loadStopWords();
      luc = new LuceneHelper(index_path);
      FileInputStream fstream = new FileInputStream("lq.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
      Scanner input = new Scanner(System.in);
      String strLine;

      // luc.testStuff();
      // input.next();


      while ((strLine = br.readLine()) != null)   {

        // Read next question in JSON format
        JSONObject jsonObj = new JSONObject(strLine);
        String title = jsonObj.get("title").toString();
        String body = jsonObj.get("body").toString();
        String best = jsonObj.get("best").toString().toLowerCase();
        String question = (title + " " + body).toLowerCase();

        JSONArray answers = jsonObj.getJSONArray("answers");
        String long_answer = "";
        for (int i = 0;  i < answers.length(); ++i) {
          String answer = answers.get(i).toString();
          long_answer += answer + " ";
        }

        System.out.println("QUESTION");
        System.out.println(question);

        System.out.println("COLLABORATIVE ANSWER");
        System.out.println(long_answer);

        PrintWriter writer1 = new PrintWriter(new FileOutputStream(new File(filename),true));
        writer1.println("QUESTION: " + question + "\n");
        writer1.println("ANSWER: " + long_answer + "\n");
        writer1.close();
        

// make queries
        // clean and tokenize question text
        Vector<String> qtokens = tokenizeAndClean(question, true, true, true, true, true);
        Vector<String> atokens = tokenizeAndClean(long_answer, true, true, true, true, true);

        // we want to compare to both question text and answer text
        atokens.addAll(qtokens);
        System.out.println("BACKGROUND MODEL: " + atokens.toString() + "\n");


        // Drop duplicate tokens while preserving the order of words
        Vector<String> no_dupl = removeDuplicateTokens(qtokens);
        System.out.println(no_dupl);

        // Compose queries
        // We first pick random queries of different lengths and them combine them all together
        Date startingTime = Calendar.getInstance().getTime();
        HashMap<Integer, Vector<String>> all_queries = composeQueries1(no_dupl, 5);
        Date now = Calendar.getInstance().getTime();
        long timeElapsed = now.getTime() - startingTime.getTime();
        System.out.println("Time to compose queries " + timeElapsed);
        

        Vector<String> queries = pickRandomSample(all_queries.get(2), query_num);
        queries.addAll(pickRandomSample(all_queries.get(3), query_num));
        queries.addAll(pickRandomSample(all_queries.get(4), query_num));
        queries.addAll(pickRandomSample(all_queries.get(5), query_num));

        // Now query ClueWeb
        System.out.println("\n *** \n Start searching");
        int max_doc = 5;

        HashMap<String, Double> query_ranking = new HashMap<String, Double>();
        HashMap<String, HashMap<Double, Integer>> word_distribution = new HashMap<String, HashMap<Double, Integer>>();

        // HashMap<String, WordRanking> wordRanks = new HashMap<String, WordRanking>();

        double min_kld = Double.POSITIVE_INFINITY;
        double max_kld = Double.NEGATIVE_INFINITY;

        // double ave_kld_for_question = 0;

        for (int i = queries.size() - 1 ; i >= 0; --i) {
            int q_length = queries.get(i).split("\\s").length;
            Vector<String> c_query = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));

            double ave_kld = 0;
            

            String current_query = queries.get(i);

          
            
            Vector<Document> docs = luc.performSearch_Doc(current_query, max_doc);
            for (int ii = 0; ii < docs.size(); ++ii) {
              Document doc = docs.get(ii);

              // System.out.println(doc);
              // input.next();

              String doc_id = doc.get(FIELD_ID);
              Vector<String> dtokens = tokenizeAndClean(doc.get(FIELD_BODY), true, true, true, true, true);



              
              // double kld = KLD_JelinekMercerSmoothing(dtokens, atokens, 0.9f);
              // double kld = similarityNoFreq(dtokens, atokens);
              // ave_kld += kld;
              
            }

            // search and calc similarity
            // Vector<String> docs = luc.performSearch(current_query, max_doc);
            // for (int ii = 0; ii < docs.size(); ++ii) {
            //   String doc = docs.get(ii);

            //   // System.out.println(doc);
            //   // input.next();
              
            //   Vector<String> dtokens = tokenizeAndClean(doc, true, true, true, true, true);


              
            //   double kld = KLD_JelinekMercerSmoothing(dtokens, atokens, 0.9f);
            //   // double kld = similarityNoFreq(dtokens, atokens);
            //   ave_kld += kld;
              
            // }
            
            ave_kld /= docs.size();
            ave_kld = Double.parseDouble(decimalFormat.format(ave_kld));
            System.out.println(i + ". " + current_query + " - " + ave_kld);

            if (ave_kld < min_kld) {
              min_kld = ave_kld;
            }
            if (ave_kld > max_kld) {
              max_kld = ave_kld;
            }
            
            
            


            Vector<String> words = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));
            for (String w : words) {
              if (! word_distribution.containsKey(w)) {
                word_distribution.put(w, new HashMap<Double, Integer>());
              }
              HashMap<Double, Integer> bins = word_distribution.get(w);
              Double kld_key = new Double(ave_kld);

              if (! bins.containsKey(kld_key)) {
                bins.put(kld_key, new Integer(1));
              } else {
                Integer count = bins.get(kld_key);
                bins.put(kld_key, new Integer(count + 1));
              }

              word_distribution.put(w, bins);

            }

            

            query_ranking.put(queries.get(i), ave_kld);
        }

        List<Entry<String, Double>> sorted = entriesSortedByValues(query_ranking);
        wordFreqInTopNQueries(sorted, 20);
        input.next();

        write_word_distrinbution2json(word_distribution, "word_distribution.txt", max_kld, min_kld);
        System.out.println(word_distribution);
        System.out.println("Min KLD = " + min_kld);
        System.out.println("Max KLD = " + max_kld);
        input.next();

        // ave_kld_for_question /= queries.size();

        // double mid_kld = min_kld + (max_kld - min_kld) / 2;

        // List<Entry<String, Double>> sorted = entriesSortedByValues(query_ranking);
        // int queries_length = Math.round((int)(sorted.size() * 0.2));
        // Vector<String> all_words = new Vector<String> ();

        // for ( int i = 0; i < queries_length; ++i) {
        //   String qqq = sorted.get(i).getKey();
        //   all_words.addAll(Arrays.asList(queries.get(i).split("\\s")));
        // }

        // System.out.println(all_words);
        // System.out.println(buildDistribution(all_words));

        // PrintWriter writer = new PrintWriter(new FileOutputStream(new File(filename),true));
        // writer.println("\nDistribution of words in first 40 queries");
        // writer.println(entriesSortedByValues(buildDistribution(all_words)).toString());
        // writer.close();

        // min_kld = Double.POSITIVE_INFINITY;
        // max_kld = Double.NEGATIVE_INFINITY;
        // ave_kld_for_question = 0;
        
      }

      br.close();
  }
}


