//opennlp
import opennlp.tools.tokenize.SimpleTokenizer;

//util
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;
import java.util.Map;
import java.util.Map.*;
import java.util.List;
import java.util.*;


// io
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;

//lucene
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.queryparser.classic.ParseException;

// misc
import java.text.DecimalFormat;
import org.json.*;
import java.sql.*;



public class Utils {
  private static Vector<String> stopwords;
  private static Scanner input = new Scanner(System.in);

  public static Vector<String> resultSet2Vect(ResultSet rs, String fieldName) {
    Vector<String> result = new Vector<String>();
    try {
      while(rs.next()) {
        result.add(rs.getString(fieldName));
      }
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    return result;
  }

  public static Vector<String> str2vect(String str) {
    // given a space separated string, convert it to vector<String>

    Vector<String> tokens = new Vector<String>();
    for (String s :Arrays.asList(str.split("\\s"))) {
      if (!s.trim().equals("")) {
        tokens.add(s);
      }
    }
    return tokens;
  }

public static Vector<String> JSONArrayToVect(JSONArray jarr) throws JSONException {
  Vector<String> resVect = new Vector<String>();
  if (jarr != null) { 
    int len = jarr.length();
    for (int i = 0; i < len; i++) { 
      resVect.add(jarr.get(i).toString());
    } 
  } 
  return resVect;
}

public static Vector<QueryScoring> sortByFiledName(final String fieldName, Vector<QueryScoring> queryScoringList) {
  Collections.sort(queryScoringList, new Comparator<QueryScoring>() {
        @Override
        public int compare(QueryScoring q1, QueryScoring q2) {
          if (fieldName.equals("atokensSim")) {
            return q1.atokensSim >= q2.atokensSim ? 1 : 0;
          } else if (fieldName.equals("qtokensSim")) {
            return q1.qtokensSim >= q2.qtokensSim ? 1 : 0;
          } else if (fieldName.equals("alltokensSim")) {  
            return q1.alltokensSim >= q2.alltokensSim ? 1 : 0;  
          } else if (fieldName.equals("aSimDom")) {
            return q1.aSimDom >= q2.aSimDom ? 1 : 0;  
          } else if (fieldName.equals("qSimDom")) {
            return q1.qSimDom >= q2.qSimDom ? 1 : 0;
          } else {
            return q1.equalWeightSim >= q2.equalWeightSim ? 1 : 0;
          }
        }
      });
  return queryScoringList;
}

public static String fromJSONGet(String jsonStr, String fieldName) {
  try {
    JSONObject jsonObj = new JSONObject(jsonStr);
    return jsonObj.get(fieldName).toString();  
  } catch (JSONException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
    return null;
  }
  
}

public static void addtoDB () {
            ////**** Adding the question to SQLite db
        //   title = title.replaceAll("\n", "").replaceAll("'", "''");
        //   body = body.replaceAll("\n", "").replaceAll("'", "''");
        //   String sqlAnswers = answers.toString().replaceAll("\n", "").replaceAll("'", "''");

        //     sql = "INSERT INTO QUESTIONS (QID, QTOKENS, ATOKENS, GTQUERY, QTITLE, QBODY, ANSWERS) VALUES (" //1, 'test''s', 'test', 'test', 'test', 'test', 'test');";
        //      + questID + ", '" + Utils.vector2String(qtokens) + "', '" + Utils.vector2String(atokens) + "', '" + Utils.vector2String(gt_query) + "', '" + title + "', '" +
        //     body + "', '" + sqlAnswers + "');";
        //     System.out.println(sql);
        //     try {
        //       stmt.executeUpdate(sql);
        //     } catch (SQLException e) {
        //   System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        //   System.exit(0);
        // }
          ////**** Adding the question to SQLite db. End.
}

  public static void answerINtersection () {
    // /// ***** Answer intersection start
        // // writer.println("<b>QUESTION:</b><br> " + question + "<br>");

        // Vector<Entry<String, Double>> answersIntersection = Utils.answersIntersectionJSON(jsonObj.getJSONArray("answers"), 0.4, luc);
        // HashSet<String> answersIntersectionWords = new HashSet<String>();
        // // writer.println("\n<b>INETERSECTING WORDS:</b><br> ");
        // for (Entry<String, Double> e : answersIntersection) {
        //   // System.out.println (e.getKey() + " --- " + e.getValue());
        //   answersIntersectionWords.add(e.getKey());
        // }

        // // input.next();

        // // writer.println("<b>ANSWERS: </b><br>");
        // // for (int i = 0;  i < answers.length(); ++i) {
        //   // String answer = answers.get(i).toString();
        //   // writer.println(answer + "<br>\n***<br>");
        // // }

        // // writer.println("<br>\n*********************\n<br>");
        // /// ***** Answer intersection end

  }

  public static String vector2String(Vector<String> vector) {
    StringBuilder sb = new StringBuilder();
    for (String s : vector) {
      sb.append(s + " ");
    }
    return sb.toString();
  }

  public static <T> List<T> sliceCollection(List<T> collection, int from, int to) {
    
    if (!((from >= 0) && (to <= collection.size()))) {
      System.out.println("Slice vector. Bad indicies. Returning null. ");
      return null;
    } 

    List<T> result = new ArrayList<T>();

    for (int i = from; i < to; ++i) {
      result.add(collection.get(i));
    }
    return result;
  }
 
  public static <T> double RBO(Vector<T> a, Vector<T> b, double p, int evalDepth) {
    /// calculates Rank Biased Overlap with parameter 0 < p < 1

    double rbo = 0.0;
    for (int d = 1; d <= evalDepth; ++d) {
      rbo += Math.pow(p, d-1) * Utils.agreementAtD(a, b, d);
    }

    rbo *= (1 - p);
    return rbo;

  }

  // public static <T> double averageOverlap(Vector<T> a, Vector<T> b, int k) {
  //   /// caluculates an average overlap at depth k
  //   if ((a.size() < k) || (b.size() < k)) {
  //     System.out.println("The list are shorter than the depth. Returning 0.0 and exiting.");
  //     return 0.0;
  //   }    

  //   double accOverlap = 0.0;
  //   for (int d = 1; d <= k; ++d) {
  //     accOverlap += agreementAtD(a, b, d);
  //   }

  //   return accOverlap / k;
  // }

  public static <T> double agreementAtD(Vector<T> a, Vector<T> b, int depth) {
    /// calculates the agreement between two ranked lists at depth

    if ((a.size() < depth) || (b.size() < depth)) {
      System.out.println("The list are shorter than the depth. Returning 0.0 and exiting.");
      return 0.0;
    }

    // intersection
    Vector<T> bSlice = new Vector<T>();
    for (int i = 0; i < depth; ++i) {
      bSlice.add(b.get(i));
    }

    Vector<T> intersection = new Vector<T>();
    for (int i = 0; i < depth; ++i) {
      T el = a.get(i);
      if (bSlice.contains(el)) {
        intersection.add(el);
      }
    }

    // overlap
    int X = intersection.size();

    // agreement
    double agreement = (double)X / (double) depth; 
    
    // System.out.println("Agreement at " + depth + " is " + agreement);
    return agreement;
  }



  public static Vector<String> tokenizeAndClean(String text, boolean lower, boolean nopunct, boolean shrink_rep, 
    boolean drop_stop, boolean stem) throws FileNotFoundException {
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

  public static String removePunct(String str) {
    return str.replaceAll("[^A-Za-z'\\s\\d]", " ");
  }

  public static Vector<String> removeShortTokens(Vector<String> vect, int minLength) {
    for (int i = 0; i < vect.size(); ++i) {
      if (vect.get(i).length() < minLength) {
        vect.remove(i);
      }
    }
    return vect;
  }

  public static Vector<String> removePunctVect(Vector<String> vect) {
    for (int i = 0;  i < vect.size(); ++i) {
      vect.set(i, removePunct(vect.get(i)));
    } 

    return vect;
  }

  public static String shrinkRepeatedChars(String str) {
    Pattern p = Pattern.compile("(.)\\1{2,}");
    Matcher m = p.matcher(str);
    String res = m.replaceAll("$1"); 
    return res;
  }

  public static Vector<String> dropStopWords(Vector<String> tokens) {
    if (stopwords == null) {
      Utils.loadStopWords();
    }
    tokens.removeAll(stopwords);
    return tokens;
  }

  private static void loadStopWords() {
      Utils.stopwords = new Vector<String>();
      try {
        FileInputStream fstream = new FileInputStream("stop_words.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        
        while ((strLine = br.readLine()) != null)   {
            Utils.stopwords.add(strLine.trim().toLowerCase());
        }
        br.close();  
      } catch (FileNotFoundException e) {
        System.out.println("No stop_words.txt in the current folder");
      } catch (IOException e) {
        System.out.println("IOException. Something went wrong. " + e.getMessage());
      }
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
  public static Vector<String> lucene_tokenize(Vector<String> tokens) {
    StringBuilder sb = new StringBuilder();
    for (String s : tokens) {
      sb.append(s + " ");
    }
    return Utils.lucene_tokenize(sb.toString());
  }
  public static Vector<String> lucene_tokenize(String text) {
    Analyzer analyzer = new EnglishAnalyzer();
    Vector<String> result = new Vector<String>();
    try {
      TokenStream stream  = analyzer.tokenStream(null, new StringReader(text));
      stream.reset();
      while (stream.incrementToken()) {
        result.add(stream.getAttribute(CharTermAttribute.class).toString());
      }
    } catch (IOException e) {
      // not thrown b/c we're using a string reader...
      throw new RuntimeException(e);
    }
    return result;
  }

  static <K,V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K,V> map, String direction) {

    /// direction can be either "inc" or "dec"
    List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());

    if (direction == "inc") {
    Collections.sort(sortedEntries, 
            new Comparator<Entry<K,V>>() {
                @Override
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            }
    );
  } else {
    Collections.sort(sortedEntries, 
            new Comparator<Entry<K,V>>() {
                @Override
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e2.getValue().compareTo(e1.getValue());
                }
            }
    );
  }

    return sortedEntries;
  }

  static <K,V extends Comparable<? super V>> Vector<Entry<K, V>> entriesSortedByValues(Vector<Entry<K,V>> entries) {
    Collections.sort(entries, 
            new Comparator<Entry<K,V>>() {
                @Override
                public int compare(Entry<K,V> e1, Entry<K,V> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            }
    );
    return entries;
  }


  public static <K,V> void printMap(Map<K,V> map) {
    for (Entry<K,V> e : map.entrySet()) {
      System.out.println(e.getKey().toString() + " -- " + e.getValue().toString());
    }
  }

  public static <T> void printCollection(Collection<T> c) {
    for (T e : c) {
      System.out.println(e.toString()); 
    }
  }

    public static Set <String> create_vocabulary(String text1, String text2) {
      SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
      String[] tokens1 = tokenizer.tokenize(text1);
      String[] tokens2 = tokenizer.tokenize(text2);
      Set<String> vocab1 = new HashSet<String>(Arrays.asList(tokens1));
      Set<String> vocab2 = new HashSet<String>(Arrays.asList(tokens2));
      Set<String> vocabulary = new HashSet<String>(vocab1);
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
                    if (! Utils.containsQuery (newCombinations, new_query)) {
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

  public static void write_query_ranking(HashMap<String, Double> queries, boolean toFile, String filename) throws FileNotFoundException{
    List<Entry<String, Double>> sorted = Utils.entriesSortedByValues(queries, "inc");

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

  public static HashMap<Integer, Vector<String>> composeQueries1(Vector<String> tokens, int maxQueryLength) {
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

  public static Vector<String> pickRandomSample(Vector<String> queries, int sampleSize){
  //// Given a vector of queries pick random sample of size sampleSize
    Random rand = new Random();
    // int max_sample_size = 100;
    int min = 0;
    int max = queries.size();
    Vector<String> randomSample = new Vector<String>();

    if (max <= sampleSize) {
      return queries;
    }

    int count = 0;
    while (count < sampleSize) {
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


  public static List<Entry<String, Double>> pointwiseKLD(Vector<String> foreground, LuceneHelper luc) {
    // returns KLD value with the background of the entire index
    HashMap<String, Double> fore_distr = buildDistribution(foreground);
    HashMap<String, Double> kldValues = new HashMap<String, Double>();
    double kld_value = 0;

    for (Entry<String, Double> e : fore_distr.entrySet()) {
      try {
        double p_i = e.getValue();
        double q_i = (double)(luc.totalTermFreq(e.getKey())) / (double)(luc.totalTerms());
        if (q_i == 0) {
          continue;
        } 

        double kldScore = p_i * Math.log(p_i / q_i);
        kldValues.put(e.getKey(), new Double(kldScore));
      } catch (IOException exc) {
        System.out.println(exc.getMessage());
        continue;
      } catch (ParseException exc) {
        System.out.println(exc.getMessage());
        continue;
      }

    }

    // static <K,V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {
    return entriesSortedByValues(kldValues, "dec");
  }

   public static double KLD_JelinekMercerSmoothing(Vector<String> foreground, Vector<String> background, double lambda, LuceneHelper luc) 
  throws IOException, ParseException {

    HashMap<String, Double> fore_distr = buildDistribution(foreground);
    HashMap<String, Double> back_distr = buildDistribution(background);

    HashMap<String, Double> all_distr = new HashMap<String, Double> (fore_distr);
    all_distr.putAll(back_distr);

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
      
      double add = smoothed_pi * Math.log10(smoothed_pi / smoothed_qi);
      
      if (Double.isInfinite(add)) {

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

    public static HashMap<String, Double> buildDistribution(Vector<String> tokens) {
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



    public static void write_word_distrinbution2json(HashMap<String, HashMap<Double, Integer>> distr, String filename, 
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

      public  static Vector<Vector<String>> createNGrams(Vector<String> tokens, int n) {
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

    public static Vector<Vector<String>> ngramIntersection(Vector<Vector<String>> eval, Vector<Vector<String>> groundTruth) {
      Vector<Vector<String>> matchedNgrams = new Vector<Vector<String>>();
      for (Vector<String> ng : groundTruth) {
        if (eval.contains(ng)) {
          matchedNgrams.add(ng);
        }
      }
      return matchedNgrams;
    }

      public  static void wordFreqInTopNQueries(List<Entry<String, Double>> sortedQueries, int n) {
      // sortedQueries - sorted by increasing KLD score  (good queries are the first ones - with low KLD)
      Vector<String> topTokens = new Vector<String>();
      for (int i = 0; i < n; ++i) {
        topTokens.addAll(Arrays.asList(sortedQueries.get(i).getKey().split("\\s")));
      }

      List<Entry<String, Double>> sortedWords = Utils.entriesSortedByValues(Utils.buildDistribution(topTokens), "inc");
      for (Entry<String, Double> w : sortedWords) {
        System.out.println(w.getKey() + " - " + w.getValue());
      }
    }

      public static Vector<String> removeDuplicateTokens(Vector<String> tokens) {
    Vector<String> result = new Vector<String>();
    for (int i = 0; i < tokens.size(); ++i) {
      if (!(result.contains(tokens.get(i)))) {
        result.add(tokens.get(i));
      }
    }
    return result;
  } 

  public static Vector<Entry<String, Double>> answersIntersectionJSON(JSONArray answers, double minRepFraction, LuceneHelper luc) {
    int answersCount = answers.length();
    Vector<String> answersVect = new Vector<String>();
    for (int i = 0;  i < answersCount; ++i) {
      try {
        String answer = answers.get(i).toString();
        answersVect.add(answer);
      } catch (JSONException e) {
        System.out.println("JSONException in Utils.answersIntersection" + e.getMessage());
        continue;
      }
    }

    return answersIntersection(answersVect, minRepFraction, luc);

  }

  public static  Vector<Entry<String, Double>> answersIntersection(Vector<String> answers, double minRepFraction, LuceneHelper luc) {
    /* Given a set of answers find which words (except stop words) are repeated throughout all of them.
    For every repetition (in another answer) give that word +1 point.  
    Sort words by the number of points - high to low.
    Words with high repetition score should be indicative of what the topic of the discussion is. */

    HashMap<String, Integer> wordsRep = new HashMap<String, Integer>();
    HashSet<String> allWords = new HashSet<String>();
    int answersCount = answers.size();
    int maxReps = 1;
      for (int i = 0;  i < answersCount; ++i) {
          String answer = answers.get(i); // .toString();

          // clean and tokenize with lucene heavy tokenizer
          Vector<String> atokens = Utils.dropStopWords(Utils.lucene_tokenize(Utils.shrinkRepeatedChars(Utils.removePunct(answer))));

          allWords.addAll(atokens);
          HashSet<String> setAtokens = new HashSet<String>();
          setAtokens.addAll(atokens);

          setAtokens.retainAll(allWords); // setAtokens contains only tokens occuring in bother sets
          for (String s : setAtokens) {
            if (wordsRep.containsKey(s)) {
              int reps = wordsRep.get(s) + 1;
              wordsRep.put(s, reps);
              if (reps > maxReps) {
                maxReps = reps;
              }
            } else {
              wordsRep.put(s, 1);
            }
          }

          // System.out.println(atokens);
          // input.next();
        

      }

      /// Ranking v1. returning terms that are repeated at least 1/2 times that the max repeated word
      // Vector<Entry<String, Double>> topRepWords = new Vector<Entry<String, Double>>();
      // DecimalFormat decimalFormat = new DecimalFormat("#.#");
      // for (Entry<String, Integer> e : wordsRep.entrySet()) {
      //   double repFraction = e.getValue().doubleValue() / maxReps;

      //   if (repFraction > minRepFraction) {
      //     String form = decimalFormat.format(repFraction);
      //     repFraction = Double.parseDouble(form);
      //     Map.Entry<String, Double> newEntry = new AbstractMap.SimpleEntry<String, Double>(e.getKey(), new Double(repFraction));
      //     topRepWords.add(newEntry);
      //     // System.out.println(e.getKey() + " -- " + e.getValue() + " -- " + repFraction);
      //   }
      // }
      /// Ranking v1 end

      /// Ranking v2. Normalization with neg. log of term's probability * their repetition score
      Vector<Entry<String, Double>> scoredWords = new Vector<Entry<String, Double>>();
      double maxScore = 0.0;
        for (Entry<String, Integer> e : wordsRep.entrySet()) {
          try {
            double termReps = e.getValue().doubleValue();
            long termCount = luc.totalTermFreq(e.getKey());
            long totalTerms = luc.totalTerms();
            double score = - Math.log((double)termCount / (double)totalTerms) * (termReps / answersCount);
            if (Double.isInfinite(score)) {
              continue;
            }
            if (score > maxScore) {
              maxScore = score;
            }

            Map.Entry<String, Double> newEntry = new AbstractMap.SimpleEntry<String, Double>(e.getKey(), new Double(score));
            scoredWords.add(newEntry);
          }

          catch (IOException exc) {
            System.out.println(exc.getMessage());
            continue;
          }
          catch (ParseException exc) {
            System.out.println(exc.getMessage());
            continue;
          }
        }

        Vector<Entry<String, Double>> topRepWords = new Vector<Entry<String, Double>>();
        for (Entry<String, Double> e : scoredWords) {
          double score = e.getValue().doubleValue();
          if (score > (maxScore / 2.0) ) {
            topRepWords.add(e);
          }
        }



      // sort entries by value
      // topRepWords = entriesSortedByValues(topRepWords);
      // for (int i = 0; i < (int)(topRepWords.size()/2); i++) {
      //   topRepWords.removeElementAt(i);  
      // }

      /// Ranking v2 end
      return topRepWords;
  }


    // private static void loadStopWords() throws FileNotFoundException, IOException {
  //     stopwords = new Vector<String>();
  //     FileInputStream fstream = new FileInputStream("stop_words.txt");
  //     BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
  //     String strLine;
      
  //     while ((strLine = br.readLine()) != null)   {
  //         stopwords.add(strLine.trim().toLowerCase());
  //     }
  //     br.close();
  // }

  // private static Vector<String> dropStopWords(Vector<String> tokens) {
  //   tokens.removeAll(stopwords);
  //   return tokens;
  // }

  // private static Vector<String> s_stemmer(Vector<String> tokens) {
  //   Vector<String> stemmed = new Vector<String>();
  //   for (int i = 0; i < tokens.size(); ++i) {
  //     String word = tokens.get(i);
  //     StringBuilder b = new StringBuilder(word);

  //     if ((word.endsWith("ies")) && (!word.endsWith("eies")) && (!word.endsWith("aies"))) {
  //       b.replace(word.lastIndexOf("ies"), word.length(), "y");
  //     } else if ((word.endsWith("es")) && (!word.endsWith("aes")) && (!word.endsWith("ees")) && (!word.endsWith("oes"))) {
  //       b.replace(word.lastIndexOf("es"), word.length(), "e");
  //     } else if ((word.endsWith("s")) && (!word.endsWith("us")) && (!word.endsWith("ss"))) {
  //       b.replace(word.lastIndexOf("s"), word.length(), "");
  //     }

  //     // get rid of empty tokens and single character tokens
  //     if (!(b.toString() == null || b.toString().trim().equals("") || b.toString().trim().length() == 1)) {
  //       stemmed.add(b.toString());  
  //     }
  //   }
  //   return stemmed;
  // }

  // private static String shrinkRepeatedChars(String str) {
  //   Pattern p = Pattern.compile("(.)\\1{2,}");
  //   Matcher m = p.matcher(str);
  //   String res = m.replaceAll("$1"); 
  //   return res;
  // }

  // private static String removePunct(String str) {
  //   return str.replaceAll("[^A-Za-z\\s\\d]", "");
  // }



  // private static Vector<String> tokenizeAndClean(String text, boolean lower, boolean nopunct, boolean shrink_rep, 
  //   boolean drop_stop, boolean stem) {
  //   if (lower) {
  //     text = text.toLowerCase();
  //   }

  //   if (nopunct) {
  //     //Remove all punctuation marks 
  //     text = removePunct(text);
  //   }

  //   // Replace chars repeating 3 or more times with the same char repeated once
  //   if (shrink_rep){
  //     text = shrinkRepeatedChars(text);
  //   }

  //   //tokenize question
  //   SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
  //   Vector<String> tokens = new Vector<String>(Arrays.asList(tokenizer.tokenize(text)));

  //     if (drop_stop) {
  //       // drop stop words
  //       dropStopWords(tokens);
  //     }

  //     Vector<String> stemmed = new Vector<String>(tokens);
  //     if (stem) {
  //      // Light stemming to get rid of plurals
  //       stemmed = s_stemmer(tokens);
  //     }


  //     return stemmed;
  // }

 





    // // The method that prints all possible strings of length k.  It is
    // //  mainly a wrapper over recursive function printAllKLengthRec()
    // // static void printAllKLength(char set[], int k) {
    // static void printAllKLength(Vector<String> set, int k) {
    //     int n = set.size();        
    //     printAllKLengthRec(set, "", n, k);
    // }
 
    // // The main recursive method to print all possible strings of length k
    // static void printAllKLengthRec(Vector<String> set, String prefix, int n, int k) {
         
    //     // Base case: k is 0, print prefix
    //     if (k == 0) {
    //         System.out.println(prefix);
    //         return;
    //     }
 
    //     // One by one add all characters from set and recursively 
    //     // call for k equals to k-1
    //     for (int i = 0; i < n; ++i) {
    //       if (prefix.contains(set.get(i))) {
    //         // Next character of input added
    //         continue;
    //       }
    //       String newPrefix = prefix + set.get(i); 
             
    //         // k is decreased, because we have added a new character
    //         printAllKLengthRec(set, newPrefix, n, k - 1); 
    //     }
    // }


    // static  Vector<String> cc(Vector<String> tokens, int length) {
    //   Vector<String> res = new Vector<String> ();
    //   // res.addAll(withPrefix("", tokens));
    //   // for (int i = 0; i <  tokens.size(); ++i) {
    //   Vector<String> tokens_copy = new Vector<String> (tokens);
    //   for (String t : tokens) {

    //     // String t = tokens.get(i);
    //     res.add(t);
    //     Vector<String> l2 = withPrefix(t, tokens_copy);
    //     Vector<String> tokens_copy1 = new Vector<String> (l2);
    //     for (String tt : l2) {

    //       Vector<String> l3 = withPrefix(tt, tokens_copy);
    //       res.addAll(l3);
    //       tokens_copy1.remove(tt);
    //     }
    //     res.addAll(withPrefix(t, tokens_copy));
    //     tokens_copy.remove(t);
    //   }
      
    //   return res;
    // }
    // static Vector<String> withPrefix(String prefix, Vector<String> tokens) {
    //   Vector<String> res = new Vector<String>();

    //   for ( int i = 0; i < tokens.size(); ++i ) {
    //     String t = tokens.get(i);
    //     if (! prefix.contains(t)) {
    //       res.add(prefix + t);
    //     }
    //   }
    //   return res;
    // }
}