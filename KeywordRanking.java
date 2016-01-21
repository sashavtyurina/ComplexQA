import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
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

          // Vector<Vector<String>> ang = Utils.createNGrams(a, 1);
          // Vector<Vector<String>> bng = Utils.createNGrams(b, 1);
          // System.out.println(Utils.ngramIntersection(ang, bng));



          System.out.println("Input arguments: index_path, query string");
          return;
      }
      DecimalFormat decimalFormat = new DecimalFormat("#.#");

      String filename = "query_ranking.txt";
      String index_path = args[0].toString();
      String query = args[1].toString();
      int query_num = 100;

      Utils.loadStopWords();
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

        // System.out.println("COLLABORATIVE ANSWER");
        // System.out.println(long_answer);

        // PrintWriter writer1 = new PrintWriter(new FileOutputStream(new File(filename),true));
        // writer1.println("QUESTION: " + question + "\n");
        // writer1.println("ANSWER: " + long_answer + "\n");
        // writer1.close();
        

// make queries
        // clean and tokenize question text
        Vector<String> qtokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(question, true, true, true, true, false));
        Vector<String> atokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(long_answer, true, true, true, true, false));
        

        // we want to compare to both question text and answer text
        atokens.addAll(qtokens);
        // System.out.println("BACKGROUND MODEL: " + atokens.toString() + "\n");


        // Drop duplicate tokens while preserving the order of words
        Vector<String> no_dupl = Utils.removeDuplicateTokens(qtokens);
        // System.out.println(no_dupl);

        // Compose queries
        // We first pick random queries of different lengths and them combine them all together
        Date startingTime = Calendar.getInstance().getTime();
        HashMap<Integer, Vector<String>> all_queries = Utils.composeQueries1(no_dupl, 5);
        Date now = Calendar.getInstance().getTime();
        long timeElapsed = now.getTime() - startingTime.getTime();
        // System.out.println("Time to compose queries " + timeElapsed);
        

        Vector<String> queries = Utils.pickRandomSample(all_queries.get(2), query_num);
        queries.addAll(Utils.pickRandomSample(all_queries.get(3), query_num));
        queries.addAll(Utils.pickRandomSample(all_queries.get(4), query_num));
        queries.addAll(Utils.pickRandomSample(all_queries.get(5), query_num));

        // Now query ClueWeb
        System.out.println("\n *** \n Start searching");
        int max_doc = 5;

        HashMap<String, Double> query_ranking = new HashMap<String, Double>();
        HashMap<String, HashMap<Double, Integer>> word_distribution = new HashMap<String, HashMap<Double, Integer>>();

        // HashMap<String, WordRanking> wordRanks = new HashMap<String, WordRanking>();

        double min_kld = Double.POSITIVE_INFINITY;
        double max_kld = Double.NEGATIVE_INFINITY;

        for (int i = queries.size() - 1 ; i >= 0; --i) {
            int q_length = queries.get(i).split("\\s").length;
            Vector<String> c_query = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));

            double ave_kld = 0;
            

            String current_query = queries.get(i);


            // search and calc similarity

            int passage_count = 0;
            Vector<String> docs = luc.performSearch(current_query, max_doc);
            for (int ii = 0; ii < docs.size(); ++ii) {
              String doc = docs.get(ii);

              // System.out.println("Doc #" + ii);
              // input.next();

              // select passages from the documents found
              Vector<String> passages = luc.performPassageSearch(current_query, 10, 250);
              for (String p : passages) {
                passage_count ++;
                Vector<String> ptokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(p, true, true, true, true, false));
                double kld = Utils.KLD_JelinekMercerSmoothing(ptokens, atokens, 0.9f, luc);
                // System.out.println("***");
                // System.out.println(kld);
                // System.out.println(p);
                ave_kld += kld;
                
              }
              // input.next();
              
              // Vector<String> dtokens = tokenizeAndClean(doc, true, true, true, true, true);
            // Vector<String> dtokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(doc, true, true, true, true, false));


              
              // double kld = Utils.KLD_JelinekMercerSmoothing(dtokens, atokens, 0.9f, luc);
              // ave_kld += kld;
            }
            ave_kld /= passage_count;
            System.out.println(i + ". " + queries.get(i) + " -- " + ave_kld);
            // ave_kld /= docs.size();
            // ave_kld = Double.parseDouble(decimalFormat.format(ave_kld));
            // System.out.println(i + ". " + current_query + " - " + ave_kld);

            // if (ave_kld < min_kld) {
            //   min_kld = ave_kld;
            // }
            // if (ave_kld > max_kld) {
            //   max_kld = ave_kld;
            // }
            
            
            


            // Vector<String> words = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));
            // for (String w : words) {
            //   if (! word_distribution.containsKey(w)) {
            //     word_distribution.put(w, new HashMap<Double, Integer>());
            //   }
            //   HashMap<Double, Integer> bins = word_distribution.get(w);
            //   Double kld_key = new Double(ave_kld);

            //   if (! bins.containsKey(kld_key)) {
            //     bins.put(kld_key, new Integer(1));
            //   } else {
            //     Integer count = bins.get(kld_key);
            //     bins.put(kld_key, new Integer(count + 1));
            //   }

            //   word_distribution.put(w, bins);

            // }

            

            query_ranking.put(queries.get(i), ave_kld);
        }

        List<Entry<String, Double>> sorted = Utils.entriesSortedByValues(query_ranking);
        for (Entry<String, Double> e : sorted) {
          System.out.println(e.getKey() + " -- " + e.getValue());
        }
        System.out.println("\n***\n");

        // wordFreqInTopNQueries(sorted, 20);
        // input.next();

        // Utils.write_word_distrinbution2json(word_distribution, "word_distribution.txt", max_kld, min_kld);
        // System.out.println(word_distribution);
        // System.out.println("Min KLD = " + min_kld);
        // System.out.println("Max KLD = " + max_kld);
        // input.next();

        // ave_kld_for_question /= queries.size();

        // double mid_kld = min_kld + (max_kld - min_kld) / 2;

        // List<Entry<String, Double>> sorted = Utils.entriesSortedByValues(query_ranking);
        // int queries_length = Math.round((int)(sorted.size() * 0.2));
        // Vector<String> all_words = new Vector<String> ();

        // for ( int i = 0; i < queries_length; ++i) {
        //   String qqq = sorted.get(i).getKey();
        //   all_words.addAll(Arrays.asList(queries.get(i).split("\\s")));
        // }

        // System.out.println(all_words);
        // System.out.println(Utils.buildDistribution(all_words));

        // PrintWriter writer = new PrintWriter(new FileOutputStream(new File(filename),true));
        // writer.println("\nDistribution of words in first 40 queries");
        // writer.println(Utils.entriesSortedByValues(Utils.buildDistribution(all_words)).toString());
        // writer.close();

        // min_kld = Double.POSITIVE_INFINITY;
        // max_kld = Double.NEGATIVE_INFINITY;
        // ave_kld_for_question = 0;
        
      }

      br.close();
  }
}


