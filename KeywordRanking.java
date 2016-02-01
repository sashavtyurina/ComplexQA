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

  // private static Vector<String> stopwords;
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

      // Utils.loadStopWords();
      luc = new LuceneHelper(index_path);
      FileInputStream fstream = new FileInputStream("lq.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
      Scanner input = new Scanner(System.in);
      String strLine;

      // luc.testStuff();
      // input.next();
      PrintWriter writer = new PrintWriter(new FileOutputStream(new File("test.html"), true));

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

        if (answers.length() == 1) {
          continue;
        }

        /// ***** Answer intersection start
        // writer.println("<b>QUESTION:</b><br> " + question + "<br>");

        Vector<Entry<String, Double>> answersIntersection = Utils.answersIntersectionJSON(jsonObj.getJSONArray("answers"), 0.4, luc);
        HashSet<String> answersIntersectionWords = new HashSet<String>();
        // writer.println("\n<b>INETERSECTING WORDS:</b><br> ");
        for (Entry<String, Double> e : answersIntersection) {
          // System.out.println (e.getKey() + " --- " + e.getValue());
          answersIntersectionWords.add(e.getKey());
        }

        // input.next();

        // writer.println("<b>ANSWERS: </b><br>");
        // for (int i = 0;  i < answers.length(); ++i) {
          // String answer = answers.get(i).toString();
          // writer.println(answer + "<br>\n***<br>");
        // }

        // writer.println("<br>\n*********************\n<br>");
        /// ***** Answer intersection end


        /// ***** Cleaning and tokenizing start
       Vector<String> qtokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(question, true, true, true, true, false));
        Vector<String> atokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(long_answer, true, true, true, true, false));

        // System.out.println("QUESTION");
        // System.out.println(question);
        // System.out.println(qtokens);

        // System.out.println(Utils.pointwiseKLD(qtokens, luc));

        // int str = input.nextInt();
        // if (str == 1) {
        //   continue;
        // }

        // we want to compare to both question text and answer text
        atokens.addAll(qtokens);

        // Drop duplicate tokens while preserving the order of words
        Vector<String> no_dupl = Utils.removeDuplicateTokens(qtokens);
        /// ***** Cleaning and tokenizing end


        /// ***** Query composing start
        // Compose queries
        // We first pick random queries of different lengths and them combine them all together
        Date startingTime = Calendar.getInstance().getTime();
        int maxQueryLength = 5;
        HashMap<Integer, Vector<String>> all_queries = Utils.composeQueries1(no_dupl, maxQueryLength);
        Date now = Calendar.getInstance().getTime();
        long timeElapsed = now.getTime() - startingTime.getTime();

        // System.out.println("Question length = " + qtokens.size() + "; Queries length = " + all_queries)
        // System.out.println("Time to compose queries " + timeElapsed);


        // fixed number of queries might not be the best strategy, given that the quetstion length varies significantly
        // will pick 5% of existing queries
        startingTime = Calendar.getInstance().getTime();
        int query_num = 100; 
        Vector<String> queries = new Vector<String>();
        int low = 3;
        int high = maxQueryLength;
        for (int i = low; i <= high; ++i) {
          // query_num = Math.round((float)(all_queries.get(i).size() * 0.05));
          query_num = qtokens.size() * Math.round((float)(Math.log((double)(all_queries.get(i).size()))));
          queries.addAll(Utils.pickRandomSample(all_queries.get(i), query_num));
          // System.out.println("Out of " + all_queries.get(i).size() + "; pick " + query_num);
        }
        now = Calendar.getInstance().getTime();
        timeElapsed = now.getTime() - startingTime.getTime();
        // System.out.println("Time to random sample " + timeElapsed);
        /// ***** Query composing end




        /// ***** Passage selection start
        // Now query ClueWeb

        System.out.println("\n *** \n Start searching");
        int max_doc = 5;

        // we want to rank queries according to their "passage intersection" score.
        HashMap<String, Double> queryRanking = new HashMap<String, Double>();
        double maxScore = -1;

        
        for (int i = queries.size() - 1 ; i >= 0; --i) {
          String current_query = queries.get(i);
          // System.out.println("QUERY:" + current_query);
          Vector<String> passages = luc.performPassageSearch(current_query, 10, 250);

          // Vector<Entry<String, Double>> passagesIntersection = Utils.answersIntersection(passages, 0.5, luc);

          // // get high repetition words
          // Vector<String> passagesIntersectionWords = new Vector<String>();
          // for (Entry<String, Double> e : passagesIntersection) {
          //   // System.out.println (e.getKey() + " --- " + e.getValue());
          //   passagesIntersectionWords.add(e.getKey());
          // }

          // // find how many of them were in the answers
          // passagesIntersectionWords.retainAll(answersIntersectionWords);

          // System.out.println(i + ". " + current_query);
          // System.out.println("Frequent words in passages");
          // for (Entry<String, Double> e : passagesIntersection) {
          //   System.out.println (e.getKey() + " --- " + e.getValue());
          // }

          // System.out.println("Frequent words in answers");
          // System.out.println(answersIntersectionWords);

          // System.out.println("These words intersect");
          // System.out.println(passagesIntersectionWords);
          
          // double score = (double)((double)passagesIntersectionWords.size() / (double)answersIntersectionWords.size());

          // if (score > maxScore) {
          //   maxScore = score;
          // }

          // System.out.println(score);
          // queryRanking.put(current_query, new Double(score));
          // // input.next();
        }
        
        /// ***** Passage selection end

        /// **** Printing out results
        List<Entry<String, Double>> sortedQueries = Utils.entriesSortedByValues(queryRanking);
        Vector<String> highlyRankedWords = new Vector<String>();

        for (Entry<String, Double> e : sortedQueries) {
          System.out.println(e.getKey() + " -- " + e.getValue());

          if (e.getValue() > maxScore / 2.0) {
            highlyRankedWords.addAll(Arrays.asList(e.getKey().split("\\s")));
          }
        }
        HashMap<String, Double> distr = Utils.buildDistribution(highlyRankedWords);
        // Utils.printMap(distr);
        Utils.printCollection(Utils.entriesSortedByValues(distr));


        /// **** Printing out results end


        //  System.out.println("QUESTION");
        // System.out.println(question);

        // System.out.println("COLLABORATIVE ANSWER");
        // System.out.println(long_answer);        

// make queries
// clean and tokenize question text
        


         
        

        


        

        


        
        

        // HashMap<String, Double> query_ranking = new HashMap<String, Double>();
        // HashMap<String, HashMap<Double, Integer>> word_distribution = new HashMap<String, HashMap<Double, Integer>>();

//         // HashMap<String, WordRanking> wordRanks = new HashMap<String, WordRanking>();

//         double min_kld = Double.POSITIVE_INFINITY;
//         double max_kld = Double.NEGATIVE_INFINITY;

        // for (int i = queries.size() - 1 ; i >= 0; --i) {
            // int q_length = queries.get(i).split("\\s").length;
//             Vector<String> c_query = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));

//             double ave_kld = 0;
            

            // String current_query = queries.get(i);


//             // search and calc similarity

//             int passage_count = 0;
//             Vector<String> docs = luc.performSearch(current_query, max_doc);
//             for (int ii = 0; ii < docs.size(); ++ii) {
//               String doc = docs.get(ii);

//               // System.out.println("Doc #" + ii);
//               // input.next();

//               // select passages from the documents found
              // Vector<String> passages = luc.performPassageSearch(current_query, 10, 250);
              // for (String p : passages) {
                // passage_count ++;
                // Vector<String> ptokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(p, true, true, true, true, false));
                // double kld = Utils.KLD_JelinekMercerSmoothing(ptokens, atokens, 0.9f, luc);
                // System.out.println("***");
//                 // System.out.println(kld);
                // System.out.println(p);
//                 ave_kld += kld;
                
              // }
//               // input.next();
              
//               // Vector<String> dtokens = tokenizeAndClean(doc, true, true, true, true, true);
//             // Vector<String> dtokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(doc, true, true, true, true, false));


              
//               // double kld = Utils.KLD_JelinekMercerSmoothing(dtokens, atokens, 0.9f, luc);
//               // ave_kld += kld;
            // }
            // input.next();
//             ave_kld /= passage_count;
//             System.out.println(i + ". " + queries.get(i) + " -- " + ave_kld);
//             // ave_kld /= docs.size();
//             // ave_kld = Double.parseDouble(decimalFormat.format(ave_kld));
//             // System.out.println(i + ". " + current_query + " - " + ave_kld);

//             // if (ave_kld < min_kld) {
//             //   min_kld = ave_kld;
//             // }
//             // if (ave_kld > max_kld) {
//             //   max_kld = ave_kld;
//             // }
            
            
            


//             // Vector<String> words = new Vector<String>(Arrays.asList(queries.get(i).split("\\s")));
//             // for (String w : words) {
//             //   if (! word_distribution.containsKey(w)) {
//             //     word_distribution.put(w, new HashMap<Double, Integer>());
//             //   }
//             //   HashMap<Double, Integer> bins = word_distribution.get(w);
//             //   Double kld_key = new Double(ave_kld);

//             //   if (! bins.containsKey(kld_key)) {
//             //     bins.put(kld_key, new Integer(1));
//             //   } else {
//             //     Integer count = bins.get(kld_key);
//             //     bins.put(kld_key, new Integer(count + 1));
//             //   }

//             //   word_distribution.put(w, bins);

//             // }

            

//             query_ranking.put(queries.get(i), ave_kld);
//         }

//         List<Entry<String, Double>> sorted = Utils.entriesSortedByValues(query_ranking);
//         for (Entry<String, Double> e : sorted) {
//           System.out.println(e.getKey() + " -- " + e.getValue());
//         }
        // System.out.println("\n***\n");

//         // wordFreqInTopNQueries(sorted, 20);
//         // input.next();

//         // Utils.write_word_distrinbution2json(word_distribution, "word_distribution.txt", max_kld, min_kld);
//         // System.out.println(word_distribution);
//         // System.out.println("Min KLD = " + min_kld);
//         // System.out.println("Max KLD = " + max_kld);
//         // input.next();

//         // ave_kld_for_question /= queries.size();

//         // double mid_kld = min_kld + (max_kld - min_kld) / 2;

//         // List<Entry<String, Double>> sorted = Utils.entriesSortedByValues(query_ranking);
//         // int queries_length = Math.round((int)(sorted.size() * 0.2));
//         // Vector<String> all_words = new Vector<String> ();

//         // for ( int i = 0; i < queries_length; ++i) {
//         //   String qqq = sorted.get(i).getKey();
//         //   all_words.addAll(Arrays.asList(queries.get(i).split("\\s")));
//         // }

//         // System.out.println(all_words);
//         // System.out.println(Utils.buildDistribution(all_words));

//         // PrintWriter writer = new PrintWriter(new FileOutputStream(new File(filename),true));
//         // writer.println("\nDistribution of words in first 40 queries");
//         // writer.println(Utils.entriesSortedByValues(Utils.buildDistribution(all_words)).toString());
//         // writer.close();

//         // min_kld = Double.POSITIVE_INFINITY;
//         // max_kld = Double.NEGATIVE_INFINITY;
//         // ave_kld_for_question = 0;
        
    }

    br.close();
  }
}


