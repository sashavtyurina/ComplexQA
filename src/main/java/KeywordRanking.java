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
import java.util.Date;
import java.util.*;
import java.lang.Math.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.Math;
import java.util.Random;
import java.io.File;
import java.io.FileOutputStream;

import java.text.DecimalFormat;

import java.sql.*;

import org.apache.lucene.document.Document;
import java.util.logging.*;
import com.google.common.base.Joiner;
import org.jsoup.Jsoup; 



public class KeywordRanking {

  // private static Vector<String> stopwords;
  private static LuceneHelper luc; 
  private static int max_query_length = 5;
  private static Connection dbConnection = null;
  private static final Logger logger = Logger.getLogger(KeywordRanking.class.getName());
  private static Scanner input = new Scanner(System.in);
  private static String index_path = "";
  private static String query = "";
  private static DecimalFormat decimalFormat = null;
  private static FileInputStream fstream = null;
  private static BufferedReader br = null;
  private static Random rand = null;
  private static Similarity similarity = null;

  private static String OUTPUT_FOLDER = "results/";
  private static String INPUT_FOLDER = "data/";


  static public String FIELD_BODY = "contents"; // primary field name where all the text is stored
  static public String FIELD_ID = "id";

  //// SETUP METHODS
    public static void setupDBConnection() {

      try {
        Class.forName("org.sqlite.JDBC");
        dbConnection = DriverManager.getConnection("jdbc:sqlite:KeywordRankingDB.db");
        dbConnection.setAutoCommit(false);
        System.out.println("Opened database successfully");
      } catch ( Exception e ) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        System.exit(0);
      }    
    }

    public static void loggerSetup() {
      try {
        FileHandler fileHandler = new FileHandler("Logs/FullProbes.%u.%g.txt");
        fileHandler.setFormatter(new SimpleFormatter());
        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(fileHandler);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
      } catch (IOException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        System.exit(0);
      }
    }

    public static void setThingsUp() throws IOException, FileNotFoundException, ParseException {
      // index_path = args[0].toString();
      index_path = "/home/avtyurin/Anserini/lucene-index.cw09b_pos.cnt/";
      luc = new LuceneHelper(index_path); 
      loggerSetup();
      // query = args[1].toString();
      query = "query";
      decimalFormat = new DecimalFormat("#.###");
      setupDBConnection();
      fstream = new FileInputStream("data/gtQuestions.txt");
      br = new BufferedReader(new InputStreamReader(fstream));
      rand = new Random();
      similarity = new Similarity(luc, dbConnection);
    }


/* SIMILARITY METRIC THAT WORKS */
  public static void testSimilarityMeasures1() {
  /// for each question compare its answers with google snippets and rank passages accordingly
    try {
      // String sql = "select distinct questID from SnippetsFromKeywords;";
      String sql = "select distinct questID from FullProbes;";
      // String sql = "select distinct qid from questions;";
      
      Statement questIDStmt = dbConnection.createStatement();
      ResultSet questIDsRS = questIDStmt.executeQuery(sql);
      PrintWriter writer = new PrintWriter(new FileOutputStream(new File(OUTPUT_FOLDER + "ReproducinFullProbes.txt"), false));

      int equalWeightsWin = 0;
      int questionDominatesWin = 0;
      int answerDominatesWin = 0;
      int equalWeightAnswerDom = 0;

      while (questIDsRS.next()) {
          int curQuestID = questIDsRS.getInt("questID");
          // int curQuestID = questIDsRS.getInt("qid");


          Statement qidsStmt = dbConnection.createStatement();
          sql = "select qid, rawQuestion, gtquery, qtitle, qbody from questions where qid=" + curQuestID + ";";
          ResultSet qids = qidsStmt.executeQuery(sql);
          

          // int curQuestID = qids.getInt("qid");
          String rawQuestion = qids.getString("rawQuestion");
          String gtQuery = qids.getString("gtquery");
          String qtitle = qids.getString("qtitle");
          String qbody = qids.getString("qbody");

          // extract keywords from question
          /*Vector<Entry<String, Double>> keywordsWithCertainty = Keywords.extractKeywordsFromQuestionWithCertainty(rawQuestion, qtitle, qbody, similarity, 0.0);
          Vector<String> keywords = new Vector<String>();
          for (Entry<String, Double> e : keywordsWithCertainty) {
            keywords.add(e.getKey());
          }*/

          // get gt_query words
          Vector<String> gtQueryVect = new Vector<String>(Arrays.asList(gtQuery.toLowerCase().split("\\s"))); 
          /*gtQueryVect = Utils.s_stemmer(gtQueryVect);*/

          // if keywords contain all gt_query words, play with similarity score
/*          if (!keywords.containsAll(gtQueryVect)) {
            continue;
          } */  

          // for this question get all its answers 
          sql = "select answerText from answers where questID=" + curQuestID + ";";
          Statement answersStmt = dbConnection.createStatement();
          ResultSet answersRS = answersStmt.executeQuery(sql);
          Vector<String> answers = Utils.resultSet2Vect(answersRS, "answerText");
          answersStmt.close();
          answersRS.close();

          // for this question get all its snippets from the DB
          // sql = "select snippetID, questID, queryText, snippet, docURL from SnippetsFromKeywords where questID=" + curQuestID + ";";
          sql = "select snippetID, questID, queryText, snippet, docURL from FullProbes where questID=" + curQuestID + ";";
          // sql = "select snippetID, questID, queryText, snippet, docURL from NewSnippets where questID=" + curQuestID + ";";
          Statement snippetsStmt = dbConnection.createStatement();
          ResultSet snippetsRS = snippetsStmt.executeQuery(sql);
          Vector<Snippet> snippets = Snippet.rsToSnippetList(snippetsRS);
          snippetsStmt.close();
          snippetsRS.close();

          // remove snippets that were not produced by queries in our list from above
/*          Vector<Snippet> toRemove = new Vector<Snippet>();
          for (Snippet s : snippets) {
            Vector<String> queryWords = Utils.str2vect(s.queryText);
            if (!keywords.containsAll(queryWords)) {
              toRemove.add(s);
              System.out.println("removing snippet");
            }
          }
          snippets.removeAll(toRemove);*/

          

          // also select ground truth queries and their snippets
          sql = "select queryText, snippet, url from googlesearchdocs where questid=" + curQuestID + ";";
          Statement gtSnippetsStmt = dbConnection.createStatement();
          ResultSet gtSnippetsRS = gtSnippetsStmt.executeQuery(sql);
          Vector<Snippet> gtSnippets = new Vector<Snippet>();
          // filter out snippets that came from Yahoo answers
          while (gtSnippetsRS.next()) {
            String gtURL = gtSnippetsRS.getString("url");
            if (gtURL.contains("answers.yahoo.")) {
              continue;
            }

            String snippetText = gtSnippetsRS.getString("snippet");
            String queryText = gtQuery;  //gtSnippetsRS.getString("queryText");
            Snippet s = new Snippet(snippetText, queryText, -1, -1, curQuestID, true);
            gtSnippets.add(s);
          }
          gtSnippetsStmt.close();
          gtSnippetsRS.close();

          snippets.addAll(gtSnippets);

          
          writer.println("QUESTION :: " + curQuestID + "\n");
          writer.println(rawQuestion + "\n");
          // writer.println("ASNWERS :: " );
          // for (String a : answers) {
          //   writer.println(a + "\n########");  
          // }
          writer.println("\nQUERY ::");
          for (String s : gtQueryVect) {
            writer.print(s + " ");
          }
          writer.println("\n");


          // start playing with weights
            //// !!! EQUAL WEIGHTS
            // Vector<String> allSortedProbes = scoreProbes(rawQuestion, answers, snippets, writer, qtitle, qbody);
            // Vector<String> topProbes = new Vector<String>(Utils.sliceCollection(allSortedProbes, 0, 10));
            Vector<Entry<String, Double>> allSortedProbes = new Vector<Entry<String, Double>>(scoreProbes(rawQuestion, answers, snippets)); //, writer, qtitle, qbody)); //, 0.25,0.25,0.25,0.25));
            Vector<Entry<String, Double>> topProbes = new Vector<Entry<String, Double>>(Utils.sliceCollection(allSortedProbes, 0, 10));
            // now rank separate words
            writer.println("TOP SCORED PROBES::");
            Vector<String> topQueryWords = new Vector<String>();
            for (Entry<String, Double> e : topProbes) {

              writer.println(e.getKey() + " :: " + decimalFormat.format(e.getValue()));
              topQueryWords.addAll(Utils.str2vect(e.getKey()));
            }
            
            // writer.println("***EQUAL WEIGHTS***\n");
            // writer.println("Extracted top words :: ");  
            writer.println("WORDS RANKED BY FREQUENCY::");
            List<Entry<String, Double>> rankedWords = Utils.entriesSortedByValues(Utils.buildDistribution(topQueryWords), "dec");
            Vector<String> rankedWordsVect = new Vector<String>();
            for (Entry<String, Double> e : rankedWords) {
              writer.println(decimalFormat.format(e.getValue()) + " :: " + e.getKey());
              rankedWordsVect.add(e.getKey());
            }
            // we want to make both vectors have the same length - gtQueryVect and rankedWords
/*            if (rankedWordsVect.size() > gtQueryVect.size()) {
              gtQueryVect = Utils.extendVectorToLength(gtQueryVect, rankedWordsVect.size(), "");  
            } else {
              rankedWordsVect = Utils.extendVectorToLength(rankedWordsVect, gtQueryVect.size(), "");  
            }
            
            double rboEqWeights = Math.round(Utils.RBO(rankedWordsVect, gtQueryVect, 0.5, gtQueryVect.size()) * 100d) / 100d;
            writer.println("\nRBO score is :: " + rboEqWeights);*/
            

            writer.print("\n\n");
         

          writer.println("\n------------------------------------------------------------\n");
          writer.print("\n\n");

          qidsStmt.close();
          qids.close();



      }

      questIDStmt.close();
      questIDsRS.close();

      /*writer.println("Equal weights are best in " + equalWeightsWin + " questions");
      writer.println("Question domination is the best in " + questionDominatesWin + " questions");
      writer.println("Answers domination is the best in " + answerDominatesWin + " questions");
      writer.println("Equal weights and answer doimnation are the same for " + equalWeightAnswerDom + " questions");*/
      
      writer.close();
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    } catch (FileNotFoundException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
  }


  public static List<Entry<String, Double>> scoreProbes(String question, Vector<String> answers, Vector<Snippet> snippets) {
  //// This method uses a combination of overlap scores btw snippet and question/answer
    HashMap<String, Vector<Snippet>> snippetsSplitByProbe = Snippet.splitSnippetsToProbes(snippets);

    HashMap<String, Double> scoredProbesMap = new HashMap<String, Double>();

    Vector<String> topQuestionWords = similarity.getTopQuestionWordsNoScore(question, 10);
    Vector<String> topAnswersWords = similarity.getTopAnswerWords(answers, 10);

    for (Entry<String, Vector<Snippet>> e : snippetsSplitByProbe.entrySet()) {  
      Double score = new Double(similarity.scoreForSingleProbe(topQuestionWords, topAnswersWords, e.getValue(), 0.25, 0.25, 0.25, 0.25));
      scoredProbesMap.put(e.getKey(), score);
    }

    // Vector<String> topProbes = new Vector<String>();
    // writer.println("\nTOP SCORED PROBES::");
    // List<Entry<String, Double>> scoredProbes = Utils.sliceCollection(Utils.entriesSortedByValues(scoredProbesMap, "dec"), 0, 10);
    List<Entry<String, Double>> scoredProbes = Utils.entriesSortedByValues(scoredProbesMap, "dec");
    return scoredProbes;
    /*for (Entry<String, Double> e : scoredProbes) {
      topProbes.add(e.getKey());
    }
    
    return topProbes;*/
  }

  public static List<Entry<String, Double>> scoreProbes_(String question, Vector<String> answers, Vector<Snippet> snippets, 
    PrintWriter writer, String qtitle, String qbody, double aveQuestionIntersectionWeight, double aveAnswersIntersectionWeight, 
                                                     double questionIntersectionWeight, double answersIntersectionWeight) {
  //// This method uses a combination of overlap scores btw snippet and question/answer
    HashMap<String, Vector<Snippet>> snippetsSplitByProbe = Snippet.splitSnippetsToProbes(snippets);

    HashMap<String, Double> scoredProbesMap = new HashMap<String, Double>();

    Vector<String> topQuestionWords = similarity.getTopQuestionWordsNoScore(question, 10);
    Vector<String> topAnswersWords = similarity.getTopAnswerWords(answers, 10);

    for (Entry<String, Vector<Snippet>> e : snippetsSplitByProbe.entrySet()) {  
      Double score = new Double(similarity.scoreForSingleProbe(topQuestionWords, topAnswersWords, e.getValue(), aveQuestionIntersectionWeight,  aveAnswersIntersectionWeight,
                                                                                                                questionIntersectionWeight, answersIntersectionWeight));
      scoredProbesMap.put(e.getKey(), score);
    }

    Vector<String> topProbes = new Vector<String>();
    List<Entry<String, Double>> scoredProbes = Utils.entriesSortedByValues(scoredProbesMap, "dec");
    return scoredProbes;
  }
/* SIMILARITY METRIC THAT WORKS. END. */



/* TRY TO PROBE WITH ALL POSSIBLE COMBINATIONS OF WORDS */

  public static void probeWithQueries(Vector<String> queries, int questionID,  int nextSnippetID) {
    logger.log(Level.INFO, "Now probing for question :: " + questionID);

    int randomProbeNumber = 2;
    try {
      for (String query : queries) {     
        // will make a long sleep every 3-6 probes
        if (randomProbeNumber == 0) {

          int minLongSleep = 2 * 60 * 1000; // 4 mins - 250000 // reduced to 2 mins
          int maxLongSleep = 4 * 60 * 1000; // 8 mins - 500000 // reduced to 4 mins
          int randomLongSleep = rand.nextInt((maxLongSleep - minLongSleep) + 1) + minLongSleep;
          logger.log(Level.INFO, "Waiting for " + randomLongSleep + " seconds...");
          Thread.sleep(randomLongSleep);


          int minProbes = 2;
          int maxProbes = 7;
          randomProbeNumber = rand.nextInt((maxProbes - minProbes) + 1) + minProbes;
          logger.log(Level.INFO, "Will be sleeping after " + randomProbeNumber + " probes...");
        }
        randomProbeNumber--;


        // System.out.println(query);
        logger.log(Level.INFO, "Now probing with :: " + query);

        HashMap<String, String> snippetsMap = GoogleSearch.searchSnippetsBing(query);
        logger.log(Level.INFO, "after retrieving snippets");


        for (Entry<String, String> e : snippetsMap.entrySet()) {
          String docURL = e.getKey();
          String snippet = e.getValue();

          String sql = "insert into FullProbes (snippetID, snippet, docURL, questID, queryText)" +
                " values (" + nextSnippetID + ", ?, ?, " + questionID + ", ?);";

          PreparedStatement addToGoogleSearchStmt = dbConnection.prepareStatement(sql);
          addToGoogleSearchStmt.setString(1, snippet);
          addToGoogleSearchStmt.setString(2, docURL);
          addToGoogleSearchStmt.setString(3, query);

          addToGoogleSearchStmt.executeUpdate();
          addToGoogleSearchStmt.close();
          dbConnection.commit();

          nextSnippetID ++;
        }

        int min = 10000; // 15 sec // reduced to 10 sec
        int max = 15000; // 23 sec // reduced to 15 sec
        int randomWaitPeriod = rand.nextInt((max - min) + 1) + min;
        logger.log(Level.INFO, "Waiting for " + randomWaitPeriod + " seconds...");
        Thread.sleep(randomWaitPeriod);
      }

    } catch (InterruptedException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );  
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
  }

  public static void probeWithAllQueries() {
    Integer[] anArray = {1,2,3,6,9};
    Vector<Integer> questionsIDs = new Vector<Integer>(Arrays.asList(anArray));

    try {
      for (Integer questID : questionsIDs) {
        /// get question text
          String sql = "select rawQuestion from questions where qid=" + questID + ";";
          Statement rawQStmt = dbConnection.createStatement();
          ResultSet rawQRS = rawQStmt.executeQuery(sql);
          String rawQuestion = rawQRS.getString("rawQuestion");
          rawQStmt.close();
          rawQRS.close();

        /// clean question and split into tokens
          String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(rawQuestion.toLowerCase()));
          Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));
          qTokens = Utils.dropStopWords(qTokens);
          qTokens = Utils.removeShortTokens(qTokens, 2);
          qTokens = Utils.removeDuplicateTokens(qTokens);

        /// compose all possible queries
          logger.log(Level.INFO, "Words to compose probes from :: " + qTokens);
          Vector<String> probes = Utils.composeQueries1(qTokens, 3).get(3);



        /// select the biggest existing snippetid to avoid overwriting existing snippets 
          sql = "select max(snippetID) as maxSnippetID from FullProbes;";
          Statement snippetIDStmt = dbConnection.createStatement();
          ResultSet snippetIDRS = snippetIDStmt.executeQuery(sql);
          int nextSnippetID = snippetIDRS.getInt("maxSnippetID") + 1;

        /// select existing queries for this question
          sql = "select distinct queryText from FullProbes where questID=" + questID + ";";
          Statement queriesStmt = dbConnection.createStatement();
          ResultSet queriesRS = queriesStmt.executeQuery(sql);
          Vector<String> existingQueries = Utils.resultSet2Vect(queriesRS, "queryText");
          queriesStmt.close();
          queriesRS.close();


        logger.log(Level.INFO, "before iterating probes");
        Vector<String> filteredProbes = new Vector<String>();
        for (String p : probes) {
          if (existingQueries.contains(p)) {
            continue;
          }
          filteredProbes.add(p);
        }
        
          
        logger.log(Level.INFO, "after iterating probes");

        /// now actually probe with queries and write them in the DB
          probeWithQueries(filteredProbes, questID, nextSnippetID);
      }
    } catch (JSONException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch (Exception e) {
      logger.log(Level.INFO, e.getClass().getName() + ": " + e.getMessage());
    } 
  }

/* TRY TO PROBE WITH ALL POSSIBLE COMBINATIONS OF WORDS. END. */ 


/* EXTRACTING KEYWORDS FROM QUESTION */

public static void evalQuestionKeywords() {
  try {

    String sql = "select distinct qid from questions;";  
    Statement questIDStmt = dbConnection.createStatement();
    ResultSet questIDsRS = questIDStmt.executeQuery(sql);
    PrintWriter writer = new PrintWriter(new FileOutputStream(new File(OUTPUT_FOLDER + "EvaluateKeywords.txt"), false));

    while (questIDsRS.next()) {

      int curQuestID = questIDsRS.getInt("qid");
      Statement qidsStmt = dbConnection.createStatement();
      sql = "select qid, rawQuestion, gtquery, qtitle, qbody from questions where qid=" + curQuestID + ";";
      ResultSet qids = qidsStmt.executeQuery(sql);
      String rawQuestion = qids.getString("rawQuestion");
      String gtQuery = qids.getString("gtquery");
      String qtitle = qids.getString("qtitle");
      String qbody = qids.getString("qbody");      

      Vector<String> gtQueryWords = Utils.s_stemmer(Utils.str2vect(gtQuery.toLowerCase()));

      // get keywords from the question
      Vector<String> topQuestionWords = Keywords.extractKeywordsFromQuestion(rawQuestion, qtitle, qbody, similarity);

      // // how many words from gt query were not extracted
      // writer.println("QUESTION :: " + curQuestID);
      // writer.println("GT QUERY :: " + gtQueryWords);
      // writer.println("KEYWORDS :: " + topQuestionWords);

      // gtQueryWords.removeAll(topQuestionWords);
      // writer.println("MISSING :: " + gtQueryWords);
      // writer.println("\n___________________________\n");

      // compose all possible queries out of them
      // Vector<String> queries = Utils.composeQueries1(topQuestionWords, 3).get(3);
      // PrintWriter queryWriter = new PrintWriter(new FileOutputStream(new File("QueriesFromKeywordsQuestion" + curQuestID + ".txt"), false));
      // for (String q : queries) {
      //   queryWriter.println(q);
      // }
      // queryWriter.close();


      
      qidsStmt.close();
      qids.close();
    }

    questIDStmt.close();
    questIDsRS.close();
    writer.close();


  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  } catch (FileNotFoundException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  }
}

/* EXTRACTING KEYWORDS FROM QUESTION. END. */



/* TEST SHIT */

public static void test(){
  Vector<Integer> relDocs = new Vector<Integer>();
  relDocs.add(new Integer(1));
  relDocs.add(new Integer(2));
  relDocs.add(new Integer(6));
  relDocs.add(new Integer(11));
  relDocs.add(new Integer(17));

  Vector<Integer> rankedDocs = new Vector<Integer>();
  for (int i = 1; i < 21; ++i) {
    rankedDocs.add(new Integer(i));
  }
  System.out.println(Utils.RBP(relDocs, rankedDocs, 0.95));
}
/* TEST SHIT. END. */
  
public static Vector<Snippet> getSnippetsForQuestion(int questID) {
  try {
    // for this question get all its answers 
    String sql = "select answerText from answers where questID=" + questID + ";";
    Statement answersStmt = dbConnection.createStatement();
    ResultSet answersRS = answersStmt.executeQuery(sql);
    Vector<String> answers = Utils.resultSet2Vect(answersRS, "answerText");
    answersStmt.close();
    answersRS.close();

    // for this question get all its snippets from the DB
    // sql = "select snippetID, questID, queryText, snippet, docURL from SnippetsFromKeywords where questID=" + curQuestID + ";";
    sql = "select snippetID, questID, queryText, snippet, docURL from FullProbes where questID=" + questID + ";";
    // sql = "select snippetID, questID, queryText, snippet, docURL from NewSnippets where questID=" + curQuestID + ";";
    Statement snippetsStmt = dbConnection.createStatement();
    ResultSet snippetsRS = snippetsStmt.executeQuery(sql);
    Vector<Snippet> snippets = Snippet.rsToSnippetList(snippetsRS);
    snippetsStmt.close();
    snippetsRS.close();

    // also select ground truth queries and their snippets
    sql = "select queryText, snippet, url from googlesearchdocs where questid=" + questID + ";";
    Statement gtSnippetsStmt = dbConnection.createStatement();
    ResultSet gtSnippetsRS = gtSnippetsStmt.executeQuery(sql);
    Vector<Snippet> gtSnippets = new Vector<Snippet>();
    // filter out snippets that came from Yahoo answers
    while (gtSnippetsRS.next()) {
      String gtURL = gtSnippetsRS.getString("url");
      if (gtURL.contains("answers.yahoo.")) {
        continue;
      }

      String snippetText = gtSnippetsRS.getString("snippet");
      String queryText = gtSnippetsRS.getString("queryText");
      Snippet s = new Snippet(snippetText, queryText, -1, -1, questID, true);
      gtSnippets.add(s);
    }
    gtSnippetsStmt.close();
    gtSnippetsRS.close();

    snippets.addAll(gtSnippets);
    return snippets;
  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
  }
  return null;

}

public static String getGTQueryForQuestion(int questID) {
  try {
    String sql = "select gtquery from questions where qid=" + questID + ";";
    Statement gtStmt = dbConnection.createStatement();
    ResultSet gtRS = gtStmt.executeQuery(sql);

    String queryText = gtRS.getString("gtquery");

    gtStmt.close();
    gtRS.close();

    return queryText;

  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
  }
  return "";
  
}

/// compares manual query, 
/// top words from probing with words from KLD
/// top words from full probing
  public static void compare_() {
    try {
      /// select all the questions for which we did full probes
        String sql = "select distinct questID from FullProbes;";
        Statement questIDs = dbConnection.createStatement();
        ResultSet questIDsRS = questIDs.executeQuery(sql);
        while (questIDsRS.next()) {
          int questID = questIDsRS.getInt("questID");

          /// for each such question rank words from full probes snippets
            Vector<Snippet> snippets = getSnippetsForQuestion(questID);

            sql = "select rawQuestion from questions where qid=" + questID + ";";
            Statement questTextStmt = dbConnection.createStatement();
            ResultSet questTextRS = questTextStmt.executeQuery(sql);
            String rawQuestion = questTextRS.getString("rawQuestion");
            questTextStmt.close();
            questTextRS.close();

            sql = "select answerText from answers where questID=" + questID + ";";
            Statement answersStmt = dbConnection.createStatement();
            ResultSet answersRS = answersStmt.executeQuery(sql);
            Vector<String> answers = Utils.resultSet2Vect(answersRS, "answerText");
            answersStmt.close();
            answersRS.close();

            Vector<String> topProbes = new Vector<String>();//Utils.sliceCollection(scoreProbes(rawQuestion, answers, snippets, new PrintWriter("test"), "", ""), 0, 10));

            Vector<String> fullProbesWords = new Vector<String>();
            System.out.println("Probes :: ");

            for (String p : topProbes) {
              System.out.println(p);
              fullProbesWords.addAll(Utils.str2vect(p));
            }

            System.out.println("\nWords :: ");
            List<Entry<String, Double>> rankedWords = Utils.entriesSortedByValues(Utils.buildDistribution(fullProbesWords), "dec");
            Vector<String> rankedWordsFullProbes = new Vector<String>();
            for (Entry<String, Double> e : rankedWords) {
              rankedWordsFullProbes.add(e.getKey());
              System.out.println(e.getKey());
            }

          /// for each such question rank words from kld snippets
            /*Vector<String> topKLDWords = similarity.getTopQuestionWordsNoReweighting(rawQuestion, "", "", 10);
            Vector<String> kldQueries = Utils.composeQueries1(topKLDWords, 3).get(3);

            // filter out snippets that wouldn't be produced by these queries
            Vector<Snippet> kldSnippets = new Vector<Snippet>();
            for (Snippet s : snippets) {
              Vector<String> queryTokens = Utils.str2vect(s.queryText);
              if (! topKLDWords.containsAll(queryTokens)) {
                continue;
              }
              kldSnippets.add(s);
            }

            Vector<String> topProbesKLD = new Vector<String>(Utils.sliceCollection(scoreProbes(rawQuestion, answers, kldSnippets, new PrintWriter("test"), "", ""), 0, 10));
            Vector<String> kldProbesWords = new Vector<String>();
            for (String p : topProbesKLD) {
              kldProbesWords.addAll(Utils.str2vect(p));
            }

            List<Entry<String, Double>> rankedWordsKLD = Utils.entriesSortedByValues(Utils.buildDistribution(kldProbesWords), "dec");
            Vector<String> rankedWordsProbesKLD = new Vector<String>();
            for (Entry<String, Double> e : rankedWordsKLD) {
              rankedWordsProbesKLD.add(e.getKey());
            }
            System.out.println("KLD probes :: " + rankedWordsProbesKLD);*/

          /// for each such question get their manual query
           /* Vector<String> gtQuery = Utils.str2vect(getGTQueryForQuestion(questID));
            System.out.println("Manual queries :: " + gtQuery);
    */
          /// compare the three rankings
            /*double RBPManualVSFullProbes = Utils.RBP(gtQuery, rankedWordsFullProbes, 0.5);
            double RBPManualVSKLDProbes = Utils.RBP(gtQuery, rankedWordsProbesKLD, 0.5);

            System.out.println("Full probes RBP :: " + RBPManualVSFullProbes);
            System.out.println("KLD probes RBP :: " + RBPManualVSKLDProbes);*/

            System.out.println("\n------------------------------------------------------------\n");          


        }
      } catch (SQLException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      } /*catch (FileNotFoundException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      }*/
  }


//// FOR THE WORKSHOP PAPER 
  public static void KLDvsProbing() {
    /*
      1. Given a question, get top-20(10?) words by kld only
      2. Compose queries out of them
      3. Probe with each query and evaluate each probe (comparing its snippets to question + answers) 
      4. Get words from the top probes - these are the same words as in 1. but reranked.
      5. Compare the two rank lists
    */

    try {
      /// get questions with manual queries - ok
      /// get question's answers - ok
      /// sort question's terms by kld and take the top 20 - ok
      /// compose all possible queries out of 20 words - ok
      /// get snippets for these probes - ok
      /// rank the probes - ok
      /// get the top words from the probes

      /// get questions with manual queries - ok
      String sql = "select qid, yahooqid, rawQuestion, gtquery, qtitle, qbody from questions;"; 
      Statement qStmt = dbConnection.createStatement();
      ResultSet qRS = qStmt.executeQuery(sql);
      int qNum = 1;

      while (qRS.next()) {
        int questID = qRS.getInt("qid");
        System.out.println("qid :: " + questID);
        System.out.println("qNum :: " + qNum);



        String yahooqid = qRS.getString("yahooqid").toLowerCase();
        System.out.println("yahooqid :: " + yahooqid);
        System.out.println("got yahooqid normally");
        String qtitle = qRS.getString("qtitle").toLowerCase();
        String qbody = qRS.getString("qbody").toLowerCase();
        String rawQuestion = qRS.getString("rawQuestion").toLowerCase();
        String gtquery = qRS.getString("gtquery").toLowerCase();
        Vector<String> gtTokens = Utils.str2vect(gtquery);
        System.out.println("\nGround truth query ::\n");
        for (String t : gtTokens) {
          System.out.println(t);
        }

        System.out.println("\n*****\n");
        

        /// get question's answers - ok
        sql = "select answerText from answers where questID=" + questID + ";";
        Statement answersStmt = dbConnection.createStatement();
        ResultSet answersRS = answersStmt.executeQuery(sql);
        Vector<String> answers = Utils.resultSet2Vect(answersRS, "answerText");
        answersStmt.close();
        answersRS.close();


        /// sort question's terms by kld and take the top 20 - ok
        Vector<String> topQuestionWordsByKLD = similarity.getTopQuestionWordsNoScore(rawQuestion, 10);
        
        System.out.println("\nWords by KL divergence\n");
        for (String w : topQuestionWordsByKLD) {
          System.out.println(w);
        }
        System.out.println("\n***\n");


        /// change normally ranked words by KLD to words ranked with reweighting 
        /*Vector<String> reweightedWords = similarity.getTopQuestionWordsReweightingNoScore(rawQuestion, qtitle, qbody, 10);
        System.out.println("words after reweighting :: " + reweightedWords);*/

        // use top-20 question words for intersection
        Vector<String> top20QuestionWords = new Vector<String>(Utils.sliceCollection(similarity.getTopQuestionWordsNoScore(rawQuestion, -1), 0, 10));
        System.out.println("top 20 question words :: " + top20QuestionWords);

        Vector<String> topAnswerWords = similarity.getTopAnswerWords(answers, 10);
        System.out.println("top 20 answer words :: " + topAnswerWords);


        /// compose all possible queries out of 10 top words - ok
        Vector<String> KLDProbes = Utils.composeQueries1(topQuestionWordsByKLD, 3).get(3); 

        /// also append ground truth queries
          sql = "select distinct queryText from GTQueries where questID=" + questID + ";";
          Statement gtSnippetsStmt = dbConnection.createStatement();
          ResultSet gtSnippetsRS = gtSnippetsStmt.executeQuery(sql);
          KLDProbes.add(gtSnippetsRS.getString("queryText"));
          gtSnippetsStmt.close();
          gtSnippetsRS.close();
        
        /// get snippets for these probes - ok
        Vector<Snippet> KLDSnippets = new Vector<Snippet>();
        HashMap<String, Double> probeScores = new HashMap<String, Double>();
        for (String p : KLDProbes) {
          /// for every probe find its corresponding snippets. 
          System.out.println("probe :: " + p);
          sql = "select snippet, queryText, sid, docURL, questID from AllSnippets where queryText=\"" + p + "\";"; 
          Statement snippetsStmt = dbConnection.createStatement();
          ResultSet snippetsRS = snippetsStmt.executeQuery(sql);

          Vector<Snippet> newSnippets = Snippet.rsToSnippetList(snippetsRS);
          if (newSnippets.size() == 0) {
            System.out.println(p + " was not found");
            continue;
          }

          Vector<Snippet> snippetsToDelete = new Vector<Snippet>();
          if (! yahooqid.equals("novalue")) {
            for (Snippet s : newSnippets) {
              System.out.println("docurl :: " + s.docURL);
              if (s.docURL.toLowerCase().contains("answers.yahoo.") && s.docURL.toLowerCase().contains(yahooqid)) {
                System.out.println("Removing snippet :: " + s.docURL);
                snippetsToDelete.add(s);
              }
            }
          }
          newSnippets.removeAll(snippetsToDelete);

          Double score = new Double(0.0);

          if (newSnippets.size() != 0) {
            score = new Double(similarity.scoreForSingleProbe(top20QuestionWords, topAnswerWords, newSnippets, 0.25, 0.25, 0.25, 0.25));
          }
          
          probeScores.put(p, score);
          System.out.println("probe :: " + p + " :: " + score);

          snippetsStmt.close();
          snippetsRS.close();
        }

        // / rank the probes - ok
        List<Entry<String, Double>> scoredProbes = Utils.sliceCollection(Utils.entriesSortedByValues(probeScores, "dec"), 0, 10);

        Vector<String> allWords = new Vector<String>();
        for (Entry<String, Double> scoredProbe : scoredProbes) {
          System.out.println(scoredProbe.getKey() + " :: " + decimalFormat.format(scoredProbe.getValue()));
          allWords.addAll(Utils.str2vect(scoredProbe.getKey()));
        }

        System.out.println("\nWords reranked after probing :: \n");
        Vector<String> rerankedWords = new Vector<String>();
        List<Entry<String, Double>> frequentWords = Utils.sliceCollection(Utils.entriesSortedByValues(Utils.buildDistribution(allWords), "dec"), 0, 10);
        for (Entry<String, Double> w : frequentWords) {
          System.out.println(w.getKey());  // + " :: " + decimalFormat.format(w.getValue()));
          rerankedWords.add(w.getKey());
        }
        System.out.println("\n***\n");

        double kld = Utils.RBP(gtTokens, topQuestionWordsByKLD, 0.5);
        double probing = Utils.RBP(gtTokens, rerankedWords, 0.5);
        System.out.println("RBP for KLD :: " + kld);
        System.out.println("RBP for probing :: " + probing);        

        System.out.println("\n------------------------------------------------------------\n");
        qNum ++;
      }
    
      qStmt.close();
      qRS.close();
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch (Exception e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } 
  }

  public static void exportSnippets() {
    try {
      String sql = "select questID, snippet, queryText, docURL from SnippetsFromKeywords;"; 
      Statement snippetsStmt = dbConnection.createStatement();
      ResultSet snippetsRS = snippetsStmt.executeQuery(sql);
      PrintWriter writer = new PrintWriter(new FileOutputStream("data/allSnippets.txt", true));

      while (snippetsRS.next()) {
        int questID = snippetsRS.getInt("questID");
        String snippet = snippetsRS.getString("snippet");
        String queryText = snippetsRS.getString("queryText");
        String docURL = snippetsRS.getString("docURL");

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("questID", questID);
        jsonObj.put("snippet", snippet);
        jsonObj.put("queryText", queryText);
        jsonObj.put("docURL", docURL);
        writer.println(jsonObj.toString());
      }

      writer.close();
      snippetsRS.close();
      snippetsStmt.close();
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch (FileNotFoundException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
  }

  public static void importSnippets() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("data/allSnippets-backup.txt")));

      String nextLine;
      int snippetID = 1;
      while ((nextLine = reader.readLine()) != null)   {
        JSONObject jsonObj = new JSONObject(nextLine.trim());

        int questID = jsonObj.getInt("questID");
        String sql = "insert into AllSnippets (snippetID, snippet, docURL, questID, queryText)" +
                      " values (" + snippetID + ", ?, ?, " + questID + ", ?);";
        PreparedStatement addSnippet = dbConnection.prepareStatement(sql);
        addSnippet.setString(1, jsonObj.getString("snippet"));
        addSnippet.setString(2, jsonObj.getString("docURL"));
        addSnippet.setString(3, jsonObj.getString("queryText"));

        addSnippet.executeUpdate();
        addSnippet.close();
        dbConnection.commit();
        snippetID ++;

      }
      reader.close();
    } catch (IOException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
  }

  public static void exportGTQueries() {
    try{
      String sql = "select gtquery, qid from questions";
      Statement stmt = dbConnection.createStatement();
      ResultSet rs = stmt.executeQuery(sql);

      while(rs.next()) {
        int questID = rs.getInt("qid");
        String gtQuery = rs.getString("gtquery");
        PrintWriter writer = new PrintWriter(new FileOutputStream("data/gtQueries/gtQuery" + questID + ".txt"));
        writer.println(gtQuery);
        writer.close();
      }
      stmt.close();
      rs.close();

    } catch(SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    } catch(FileNotFoundException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
  }

public static void main(String[] args) throws IOException, ParseException, JSONException, FileNotFoundException, ParseException {
  // if (args.length < 2) {
  //     System.out.println("Input arguments: index_path, query string");
  //     return;
  // }
  setThingsUp();
  // exportGTQueries();
  KLDvsProbing();
  
  // addGoogleSearchDocs();
  // addBingSearchResultsFromFile();
  // testSimilarityMeasures1();
  // populateDBWithRawQA();

  // test();
  // evalQuestionKeywords();


  
  // probeWithAllQueries();
  // compare1();
}

































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


public static Vector<String> getRandomQueriesForQuetsionID(int questID, int sampleSize) {
  //// Given question id, find all the queries constructed for it and then select a random sample,
  //// which hopefully should be uniform
    Vector<String> randomQueries = new Vector<String>();
    try {
      String sql = "select query from queries where qid in (select distinct queryid from qqp where questid=" + questID + ");";
      Statement queriesStmt = dbConnection.createStatement();
      ResultSet queriesRS = queriesStmt.executeQuery(sql);

      Vector<String> allQueries = Utils.resultSet2Vect(queriesRS, "query");
      randomQueries = Utils.pickRandomSample(allQueries, sampleSize);


      Vector<String> words = new Vector<String>();
      for (String q : randomQueries) {
        words.addAll(Utils.str2vect(q));
      }

      HashMap<String, Double> distr = Utils.buildDistribution(words);
      for (Entry<String, Double> e : distr.entrySet()) {
        System.out.println(e.getValue() + ":::" + e.getKey());
      }


      queriesStmt.close();
      queriesRS.close();

    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    return randomQueries;
  }


/// Different methods of ranking queries 
/// #1 - baseline. Just using KLD. 
public static Vector<String> baselineKLD(Vector<String> qtokens, LuceneHelper luc, int depth) {

  /// takes as input all question tokens and returns them in order of decreasing pointwise KLD score
  List<Entry<String, Double>> scoredWords = Utils.pointwiseKLD(qtokens, luc);

  Vector<String> topWords = new Vector<String>();
  int l = Math.min(depth, scoredWords.size());
  for (int i = 0;  i < l; ++i) {
    topWords.add(scoredWords.get(i).getKey());
  }
  return topWords;
}

public static Vector<String> composeQueries(Vector<String> qtokens, LuceneHelper luc) {
  // Calculate pointwise KLdivergence of every word in the question
  // Take top-20 outstanding words and use them to construcs queries
  List<Entry<String, Double>> kldScores = Utils.pointwiseKLD(qtokens, luc);
  Vector<String> top20 = new Vector<String>(20);
  int length = Math.min(kldScores.size(), 20);

  for (int i = kldScores.size() - 1; i >= kldScores.size() - length; i --) {
    top20.add(kldScores.get(i).getKey());
  }

  // compose all queries of length 3 (1140 for 20 words)  
  Date startingTime = Calendar.getInstance().getTime();
  int maxQueryLength = 3;
  HashMap<Integer, Vector<String>> all_queries = Utils.composeQueries1(top20, maxQueryLength);
  Date now = Calendar.getInstance().getTime();
  long timeElapsed = now.getTime() - startingTime.getTime();

  Vector<String> queries = new Vector<String>();
  queries = all_queries.get(maxQueryLength);

  return queries;
}

public static double comparePassagesKDL(Vector<String> passages, Vector<String> atokens, Vector<String> qtokens) 
throws FileNotFoundException, IOException, ParseException {

  for (String p : passages){
    Vector<String> ptokens = Utils.lucene_tokenize(Utils.tokenizeAndClean(p, true, true, true, true, false));
    double pscore_answer = Utils.KLD_JelinekMercerSmoothing(ptokens, atokens, (float)0.9, luc);  
    double pscore_question = Utils.KLD_JelinekMercerSmoothing(ptokens, atokens, (float)0.9, luc);  
  }
  return 0.0;
  
}

public static Vector<String> getTopWords(List<Entry<String, Double>> scoredQueries, int depth) {
  // take top "depth" queries and return a vector of words in those queries. Sorted from the most frequent to the least frequent.
  List<Entry<String, Double>> slice = Utils.sliceCollection(scoredQueries, 0, depth);
  Vector<String> words = new Vector<String>();
  for (Entry<String, Double> e : slice) {
    words.addAll(Arrays.asList(e.getKey().split("\\s")));
  }

  HashMap<String, Double> scoredWords = Utils.buildDistribution(words);
  List<Entry<String, Double>> sortedWords = Utils.entriesSortedByValues(scoredWords, "dec");

  words.clear();
  for (Entry<String, Double> e : sortedWords) {
    words.add(e.getKey());
  }
  return words;
}

public static Vector<String> passagesForQuestion(int questID) {
  if (dbConnection == null) {
    System.out.println("Setup db connection properly. Exiting...");
    return null;
  }
  return null;
}




  public static void testThings()  {/// Test RBO

          // Vector<String> a = new Vector<String> ();
          // a.add("a");
          // a.add("b");
          // a.add("c");
          // a.add("d");
          // a.add("e");
          // a.add("f");

          // System.out.println(Utils.sliceCollection(a, 2, 5));
          // Vector<String> b = new Vector<String>();
          // b.add("b");
          // b.add("c");
          // b.add("a");

          // System.out.println(Utils.RBO(a, b, 0.0, 3));
          // System.out.println(Utils.RBO(a, b, 0.3, 3));
          // System.out.println(Utils.RBO(a, b, 0.5, 3));
          // System.out.println(Utils.RBO(a, b, 0.7, 3));
          // System.out.println(Utils.RBO(a, b, 0.99, 3));

          // System.out.println(Utils.RBO(a, b, 2));
          // System.out.println(Utils.RBO(a, b, 3));

          // Vector<Vector<String>> ang = Utils.createNGrams(a, 1);
          // Vector<Vector<String>> bng = Utils.createNGrams(b, 1);
          // System.out.println(Utils.ngramIntersection(ang, bng));
}

public static void checkIfAnswersWereAddedToIndex() throws IOException {
      Vector<String> yahooDocs = luc.searchYahooDocs("Answer.0.14.yahoo.txt", 30); // "clueweb09-en0005-53-00116" 

      for (String s : yahooDocs) {
        System.out.println(s);
      }

      System.out.println("Done");
      input.next();
}

public static void addGTQueriesToDB() throws IOException, ParseException {
  //// For every question we have a manuallyconstructed (ideal / ground truth) query.
  //// We want to add every such query in the table queries. 
  try {
    String sql = "select max(qid) as max_qid from queries;";
    Statement maxQueryIDStmt = dbConnection.createStatement();
    int maxQueryID = maxQueryIDStmt.executeQuery(sql).getInt("max_qid");
    maxQueryIDStmt.close();

    sql = "select max(pid) as max_pid from passages;";
    Statement maxPIDStmt = dbConnection.createStatement();
    int maxPID = maxPIDStmt.executeQuery(sql).getInt("max_pid");
    maxPIDStmt.close();


    sql = "select qid, gtquery from questions;";
    Statement questIDstmt = dbConnection.createStatement();
    ResultSet questIDs = questIDstmt.executeQuery(sql);


    while (questIDs.next()) {
      try {
        int curQuestID = questIDs.getInt("qid");
        String curGTQuery = questIDs.getString("gtquery");

        System.out.println(curQuestID);
        System.out.println(curGTQuery);

        PrintWriter writer = new PrintWriter(new FileOutputStream(new File("checkRetrievedDocuments/DocumentsRetrivedFroQuestion" + curQuestID + ".txt"), false));

        //// insert a row into table queries
        /*maxQueryID += 1;
        sql = "insert into queries (qid, query) values (" + maxQueryID + ", " + curGTQuery + ");";
        Statement insertQueryStmt = dbConnection.createStatement();
        insertQueryStmt.executeUpdate(sql);
        insertQueryStmt.close();

        sql = "update questions set gtqueryID=" + maxQueryID + " where qid=" + curQuestID + ";";
        Statement updateQuestionsStmt = dbConnection.createStatement();
        updateQuestionsStmt.executeUpdate(sql);
        updateQuestionsStmt.close();*/

        //// now we want to perform search using this query and memorize all the passages retrieved with it
        Vector<String> passages = luc.performPassageSearch(curGTQuery, 30, 250);
        writer.println("For query [" + curGTQuery.toString() + "] could retrieve " + passages.size() + " documents.\n\n");

        for (String p : passages) {
          /*Vector<String> words = new Vector<String> (Arrays.asList(curGTQuery.split("\\s")));
          for (String w : words) {
            if (! p.toLowerCase().contains(w)) {
              System.out.println("No word " + w + " in document \n" + p);
            }
          }*/
          writer.println(p);
          writer.println("\n\n\n***\n\n\n");
         /* System.out.println(p);
          input.next();
          maxPID += 1;*/
          /*sql = "insert into passages (pid, passage) values (maxPID, p);";
          Statement insertPassageStmt = dbConnection.createStatement();
          insertPassageStmt.executeUpdate(sql);
          insertPassageStmt.close();


          //// finally we update qqp table
          sql = "insert into qqp (questid, queryid, pid) values (" + curQuestID + ", " + maxQueryID + ", " + maxPID + ");";
          Statement updateQQPStmt = dbConnection.createStatement();
          updateQQPStmt.executeUpdate(sql);
          updateQQPStmt.close();*/
        }

        writer.close();
      } catch (Exception e) {
        System.out.println("everything went wrong");
        continue;
      }
    }

    

    questIDs.close();
    questIDstmt.close();

  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  }

}

public static Vector<String> getTopWordsFromQueries(Vector<String> queries, int depth) {
  Vector<String> words = new Vector<String> ();

  for (String query : queries) {
    words.addAll(Arrays.asList(query.split("\\s"))); 
  }

  HashMap<String, Double> wordsDistribution = Utils.buildDistribution(words);
  logger.log(Level.INFO, "word distribution is" + wordsDistribution.toString());
  List<Entry<String, Double>> sortedWordsDistribution = Utils.entriesSortedByValues(wordsDistribution, "dec");

  logger.log(Level.INFO, "sorted word distribution is" + sortedWordsDistribution.toString());
  List<Entry<String, Double>> topWords = Utils.sliceCollection(sortedWordsDistribution, 0, depth);
  logger.log(Level.INFO, "top words are" + topWords.toString());

  words.clear();
  for (int i = 0; i < topWords.size(); ++i) {
    words.add(topWords.get(i).getKey());
  }

  logger.log(Level.INFO, "the final result is" + words.toString());
  return words;
}

public static String getQueryByID(int qid) {
  String queryText = "";
  try {
      String sql = "select queryText from NewQueries where queryID=" + qid + ";";
      Statement queryTextStmt = dbConnection.createStatement(); 
      ResultSet queryTextRS = queryTextStmt.executeQuery(sql);
      queryText = queryTextRS.getString("queryText");
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    }
  return queryText;
}

public static Vector<String> getQueriesTextByIDs(Vector<Integer> queryIDs) {
  Vector<String> queriesText = new Vector<String>();

  for (Integer qid : queryIDs) {
    try {
      String sql = "select queryText from NewQueries where queryID=" + qid + ";";
      Statement queryTextStmt = dbConnection.createStatement(); 

      ResultSet queryText = queryTextStmt.executeQuery(sql);
      queriesText.add(queryText.getString("queryText"));

    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      continue;
    }
  }

  return queriesText;
}


public static void averageSim(int questID, Vector<String> gtQuery, PrintWriter writer) {

  double rboCoefficient = 0.5;
  int topQueriesCount = 20;
  /// given question ID we should 
  //// 1. Find all the corresponding queries.
  //// 2. For each query find its average similarity score. 
  //// 3. write it down in a dictionary
  HashMap<Integer, Double> queryAveScoreAtokens = new HashMap<Integer, Double>();
  HashMap<Integer, Double> queryAveScoreQtokens = new HashMap<Integer, Double>();
  HashMap<Integer, Double> queryAveScoreAlltokens = new HashMap<Integer, Double>();

  HashMap<Integer, Double> queryAveScoreADomtokens = new HashMap<Integer, Double>();  // 0.7*answer_sim + 0.3*question_sim
  HashMap<Integer, Double> queryAveScoreQDomtokens = new HashMap<Integer, Double>();  // 0.3*answer_sim + 0.7*question_sim
  HashMap<Integer, Double> queryAveScoreEqualWeighttokens = new HashMap<Integer, Double>();  // 0.5*answer_sim + 0.5*question_sim
  // int testCounter = 10;
  
  try {

    String sql = "select distinct queryID as qid from qqp where questID=" + questID + ";";
    Statement queryIDsstmt = dbConnection.createStatement(); 
    ResultSet queryIDs = queryIDsstmt.executeQuery(sql);



    while(queryIDs.next()) {
      // testCounter --;
      // if (testCounter == 0) {break;}

      int queryID = queryIDs.getInt("qid");
      logger.log(Level.INFO, "Working with query " + queryID + "...");

      //// find average sim scores for every queryID
      sql = "select avg(atokensSim) as atokensSimAve, avg(qtokensSim) as qtokensSimAve, avg(alltokensSim) as allTokensSimAve " + 
                  "from KLDSim where pid in (select pid from qqp where queryid=" + queryID + ");";

      Statement aveScoresStmt = dbConnection.createStatement(); 
      ResultSet aveScoresResults = aveScoresStmt.executeQuery(sql);

      double atokensAveSim = aveScoresResults.getDouble("atokensSimAve");
      double qtokensAveSim = aveScoresResults.getDouble("qtokensSimAve");
      double alltokensAveSim = aveScoresResults.getDouble("allTokensSimAve");

      queryAveScoreAtokens.put(new Integer(queryID), new Double(atokensAveSim));
      logger.log(Level.INFO, "ATokens ave similarity is " + atokensAveSim);

      queryAveScoreQtokens.put(new Integer(queryID), new Double(qtokensAveSim));
      logger.log(Level.INFO, "QTokens ave similarity is " + qtokensAveSim);

      queryAveScoreAlltokens.put(new Integer(queryID), new Double(alltokensAveSim));
      logger.log(Level.INFO, "AllTokens ave similarity is " + alltokensAveSim);


      queryAveScoreADomtokens.put(new Integer(queryID), new Double(0.7*atokensAveSim + 0.3*qtokensAveSim));
      logger.log(Level.INFO, "ADomTokens ave similarity is " + (0.7*atokensAveSim + 0.3*qtokensAveSim));

      queryAveScoreQDomtokens.put(new Integer(queryID), new Double(0.3*atokensAveSim + 0.7*qtokensAveSim));
      logger.log(Level.INFO, "QDomTokens ave similarity is " + (0.3*atokensAveSim + 0.7*qtokensAveSim));

      queryAveScoreEqualWeighttokens.put(new Integer(queryID), new Double(0.5*atokensAveSim + 0.5*qtokensAveSim));
      logger.log(Level.INFO, "EqualWeightTokens ave similarity is " + (0.5*atokensAveSim + 0.5*qtokensAveSim));

      aveScoresStmt.close();
      aveScoresResults.close();
    }
    queryIDs.close();
    queryIDsstmt.close();

  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  }

  //// at this point we have a hashmap query - score
  //// we want to sort the queries according to the score
  //// will be sorting in increasing order, as low KLD values are good and we want to see them first
  List<Entry<Integer, Double>> sortedQueryIDsAtokens = Utils.entriesSortedByValues(queryAveScoreAtokens, "inc"); 
  List<Entry<Integer, Double>> sortedQueryIDsQtokens = Utils.entriesSortedByValues(queryAveScoreQtokens, "inc"); 
  List<Entry<Integer, Double>> sortedQueryIDsAlltokens = Utils.entriesSortedByValues(queryAveScoreAlltokens, "inc"); 

  List<Entry<Integer, Double>> sortedQueryIDsADomtokens = Utils.entriesSortedByValues(queryAveScoreADomtokens, "inc"); 
  List<Entry<Integer, Double>> sortedQueryIDsQDomtokens = Utils.entriesSortedByValues(queryAveScoreQDomtokens, "inc"); 
  List<Entry<Integer, Double>> sortedQueryIDsEqualWeighttokens = Utils.entriesSortedByValues(queryAveScoreEqualWeighttokens, "inc"); 


  // logger.log(Level.INFO, "query sim scores are " + queryAveScoreAtokens.toString());
  // logger.log(Level.INFO, "sorted query sim scores are " + sortedQueryIDsAtokens.toString());


  //// take top 20 (topQueriesCount) queries
  List<Entry<Integer, Double>> topQueryScoresAtokens = Utils.sliceCollection(sortedQueryIDsAtokens, 0, topQueriesCount);
  List<Entry<Integer, Double>> topQueryScoresQtokens = Utils.sliceCollection(sortedQueryIDsQtokens, 0, topQueriesCount);
  List<Entry<Integer, Double>> topQueryScoresAlltokens = Utils.sliceCollection(sortedQueryIDsAlltokens, 0, topQueriesCount);

  List<Entry<Integer, Double>> topQueryScoresADomtokens = Utils.sliceCollection(sortedQueryIDsADomtokens, 0, topQueriesCount);
  List<Entry<Integer, Double>> topQueryScoresQDomtokens = Utils.sliceCollection(sortedQueryIDsQDomtokens, 0, topQueriesCount);
  List<Entry<Integer, Double>> topQueryScoresEqualWeighttokens = Utils.sliceCollection(sortedQueryIDsEqualWeighttokens, 0, topQueriesCount);

  // logger.log(Level.INFO, "top " + topQueriesCount + " sim scores and queries are " + topQueryScoresAtokens.toString());


  //// from the top 20 queries only take out their ids
  Vector<Integer> topQueryIDsAtokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresAtokens.size(); ++i) {
    topQueryIDsAtokens.add(topQueryScoresAtokens.get(i).getKey());
  }

  Vector<Integer> topQueryIDsQtokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresQtokens.size(); ++i) {
    topQueryIDsQtokens.add(topQueryScoresQtokens.get(i).getKey());
  }

  Vector<Integer> topQueryIDsAlltokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresAlltokens.size(); ++i) {
    topQueryIDsAlltokens.add(topQueryScoresAlltokens.get(i).getKey());
  }

  Vector<Integer> topQueryIDsADomtokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresADomtokens.size(); ++i) {
    topQueryIDsADomtokens.add(topQueryScoresADomtokens.get(i).getKey());
  }

  Vector<Integer> topQueryIDsQDomtokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresQDomtokens.size(); ++i) {
    topQueryIDsQDomtokens.add(topQueryScoresQDomtokens.get(i).getKey());
  }

  Vector<Integer> topQueryIDsEqualWeighttokens = new Vector<Integer>();
  for (int i = 0; i < topQueryScoresEqualWeighttokens.size(); ++i) {
    topQueryIDsEqualWeighttokens.add(topQueryScoresEqualWeighttokens.get(i).getKey());
  }

  // logger.log(Level.INFO, "top query ids are " + topQueryIDsAtokens.toString());  

  //// get queries text based on their IDs 
  Vector<String> queriesTextAtokens = getQueriesTextByIDs(topQueryIDsAtokens);
  Vector<String> queriesTextQtokens = getQueriesTextByIDs(topQueryIDsQtokens);
  Vector<String> queriesTextAlltokens = getQueriesTextByIDs(topQueryIDsAlltokens);

  Vector<String> queriesTextADomtokens = getQueriesTextByIDs(topQueryIDsADomtokens);
  Vector<String> queriesTextQDomtokens = getQueriesTextByIDs(topQueryIDsQDomtokens);
  Vector<String> queriesTextEqualWeighttokens = getQueriesTextByIDs(topQueryIDsEqualWeighttokens);

  // logger.log(Level.INFO, "queries text is " + queriesTextAtokens.toString());  

  //// from the top 20 queries get top 4(gtQuery.size()) based on their frequency
  Vector<String> wordsAtokens = getTopWordsFromQueries(queriesTextAtokens, gtQuery.size());
  logger.log(Level.INFO, "Atokens result query is " + wordsAtokens.toString());  

  Vector<String> wordsQtokens = getTopWordsFromQueries(queriesTextQtokens, gtQuery.size());
  logger.log(Level.INFO, "Qtokens result query is " + wordsQtokens.toString());  

  Vector<String> wordsAlltokens = getTopWordsFromQueries(queriesTextAlltokens, gtQuery.size());
  logger.log(Level.INFO, "Alltokens result query is " + wordsAlltokens.toString());  

  Vector<String> wordsADomtokens = getTopWordsFromQueries(queriesTextADomtokens, gtQuery.size());
  logger.log(Level.INFO, "ADom result query is " + wordsADomtokens.toString());  

  Vector<String> wordsQDomtokens = getTopWordsFromQueries(queriesTextQDomtokens, gtQuery.size());
  logger.log(Level.INFO, "QDom result query is " + wordsQDomtokens.toString());

  Vector<String> wordsEqualWeighttokens = getTopWordsFromQueries(queriesTextEqualWeighttokens, gtQuery.size());
  logger.log(Level.INFO, "EqualWeight result query is " + wordsEqualWeighttokens.toString());


  logger.log(Level.INFO, "The ground truth query is " + gtQuery.toString());  


  //// now we want to compare ground truth query with the one we think is the best and find its RBO score
  double rboScoreAtokens = Utils.RBO(wordsAtokens, gtQuery, rboCoefficient, gtQuery.size());
  double rboScoreQtokens = Utils.RBO(wordsQtokens, gtQuery, rboCoefficient, gtQuery.size());
  double rboScoreAlltokens = Utils.RBO(wordsAlltokens, gtQuery, rboCoefficient, gtQuery.size());

  double rboScoreADomtokens = Utils.RBO(wordsADomtokens, gtQuery, rboCoefficient, gtQuery.size());
  double rboScoreQDomtokens = Utils.RBO(wordsQDomtokens, gtQuery, rboCoefficient, gtQuery.size());
  double rboScoreEqualWeighttokens = Utils.RBO(wordsEqualWeighttokens, gtQuery, rboCoefficient, gtQuery.size());

  logger.log(Level.INFO, "Atokens RBO score is " + rboScoreAtokens);
  logger.log(Level.INFO, "Qtokens RBO score is " + rboScoreQtokens);
  logger.log(Level.INFO, "Alltokens RBO score is " + rboScoreAlltokens);

  logger.log(Level.INFO, "ADom RBO score is " + rboScoreADomtokens);
  logger.log(Level.INFO, "QDom RBO score is " + rboScoreQDomtokens);
  logger.log(Level.INFO, "EqualWeight RBO score is " + rboScoreEqualWeighttokens);

  writer.println(questID + "\t" + gtQuery.toString() + "\t" + rboScoreAtokens   + "\t" + wordsAtokens.toString()   + "\t" +
                                                              rboScoreQtokens   + "\t" + wordsQtokens.toString()   + "\t" + 
                                                              rboScoreAlltokens + "\t" + wordsAlltokens.toString() + "\t" + 
                                                              rboScoreADomtokens + "\t" + wordsADomtokens.toString() + "\t" + 
                                                              rboScoreQDomtokens + "\t" + wordsQDomtokens.toString() + "\t" +
                                                              rboScoreEqualWeighttokens + "\t" + wordsEqualWeighttokens.toString()); 

}


public static Vector<String> getQueriesByPids(Vector<Integer> pids) {
  Vector<String> queries = new Vector<String>();
  Joiner joiner = Joiner.on(",");
  String pidsStr = joiner.join(pids);

  String sql = "select query from queries where qid in (select queryid from qqp where pid in (" + pidsStr + "));";

  try {
    Statement queriesStmt = dbConnection.createStatement();
    ResultSet queryText = queriesStmt.executeQuery(sql);

    while (queryText.next()) {
      queries.add(queryText.getString("query"));
    }
  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage());
    System.exit(0);
  }

  return queries;
}

public static void passageWiseSim (int questID, Vector<String> gtQuery, PrintWriter writer) {
  //// Take all the passages corresponding to questID and their scores
  //// Sort them by their score in increasing order, since lower KLD score is better for us
  //// Take top 20 of such passages and find their corresponding queries
  //// Build words distribution from the 20 queries
  //// Take the top 4-5 words from the distribution and use them to compute RBO score (against ground truth query)
  //// Write results to the writer

  double rboCoefficient = 0.5;
  int topPassagesCount = 20;

  HashMap<Integer, Double> aTokensSim = new HashMap<Integer, Double>();
  HashMap<Integer, Double> qTokensSim = new HashMap<Integer, Double>();
  HashMap<Integer, Double> allTokensSim = new HashMap<Integer, Double>();
  HashMap<Integer, Double> aDomTokensSim = new HashMap<Integer, Double>();
  HashMap<Integer, Double> qDomTokensSim = new HashMap<Integer, Double>();
  HashMap<Integer, Double> equalWeightTokensSim = new HashMap<Integer, Double>();

  try {
    String sql = "select pid, atokensSim, qtokensSim, alltokensSim from KLDSim where questid=" + questID + ";";
    Statement pidsStmt = dbConnection.createStatement(); 
    ResultSet pids = pidsStmt.executeQuery(sql);


    //// construct 6 vectors with different similarities
    while(pids.next()) {
      int pid = pids.getInt("pid");
      double aTokensScore = pids.getDouble("atokensSim");
      double qTokensScore = pids.getDouble("qtokensSim");
      double allTokensScore = pids.getDouble("alltokensSim");

      double aDomScore = 0.7*aTokensScore + 0.3*qTokensScore;
      double qDomScore = 0.3*aTokensScore + 0.7*qTokensScore;
      double equalWeightScore = 0.5*aTokensScore + 0.5*qTokensScore;

      aTokensSim.put(new Integer(pid), new Double(aTokensScore));
      qTokensSim.put(new Integer(pid), new Double(qTokensScore));
      allTokensSim.put(new Integer(pid), new Double(allTokensScore));
      aDomTokensSim.put(new Integer(pid), new Double(aDomScore));
      qDomTokensSim.put(new Integer(pid), new Double(qDomScore));
      equalWeightTokensSim.put(new Integer(pid), new Double(equalWeightScore));
    }
    pidsStmt.close();
    pids.close();

    //// now we have all kinds of similarities in these 6 hashmaps. Time to sort them.
    List<Entry<Integer, Double>> sortedATokensSim = Utils.entriesSortedByValues(aTokensSim, "inc");
    List<Entry<Integer, Double>> sortedQTokensSim = Utils.entriesSortedByValues(qTokensSim, "inc");
    List<Entry<Integer, Double>> sortedAllTokensSim = Utils.entriesSortedByValues(allTokensSim, "inc");
    List<Entry<Integer, Double>> sortedADomSim = Utils.entriesSortedByValues(aDomTokensSim, "inc");
    List<Entry<Integer, Double>> sortedQDomSim = Utils.entriesSortedByValues(qDomTokensSim, "inc");
    List<Entry<Integer, Double>> sortedEqualWeightSim = Utils.entriesSortedByValues(equalWeightTokensSim, "inc");

    //// take only top 20 passages into consideration and discard the rest
    List<Entry<Integer, Double>> topPsgScoresAtokens = Utils.sliceCollection(sortedATokensSim, 0, topPassagesCount);
    List<Entry<Integer, Double>> topPsgScoresQtokens = Utils.sliceCollection(sortedQTokensSim, 0, topPassagesCount);
    List<Entry<Integer, Double>> topPsgScoresAlltokens = Utils.sliceCollection(sortedAllTokensSim, 0, topPassagesCount);
    List<Entry<Integer, Double>> topPsgScoresADom = Utils.sliceCollection(sortedADomSim, 0, topPassagesCount);
    List<Entry<Integer, Double>> topPsgScoresQDom = Utils.sliceCollection(sortedQDomSim, 0, topPassagesCount);
    List<Entry<Integer, Double>> topPsgScoresEqualWeight = Utils.sliceCollection(sortedEqualWeightSim, 0, topPassagesCount);

    //// now only leave pids without the scores

    Vector<Integer> pidsAtokens = new Vector<Integer>();
    Vector<Integer> pidsQtokens = new Vector<Integer>(); 
    Vector<Integer> pidsAlltokens = new Vector<Integer>();
    Vector<Integer> pidsADom = new Vector<Integer>();
    Vector<Integer> pidsQDom = new Vector<Integer>();
    Vector<Integer> pidsEqualWeights = new Vector<Integer>();

    for (int i = 0; i < topPassagesCount; ++i) {
      pidsAtokens.add(topPsgScoresAtokens.get(i).getKey());
      pidsQtokens.add(topPsgScoresQtokens.get(i).getKey());
      pidsAlltokens.add(topPsgScoresAlltokens.get(i).getKey());
      pidsADom.add(topPsgScoresADom.get(i).getKey());
      pidsQDom.add(topPsgScoresQDom.get(i).getKey());
      pidsEqualWeights.add(topPsgScoresEqualWeight.get(i).getKey());
    }

    logger.log(Level.INFO, "ATokens pids are " + pidsAtokens.toString());
    logger.log(Level.INFO, "QTokens pids are " + pidsQtokens.toString());
    logger.log(Level.INFO, "AllTokens pids are " + pidsAlltokens.toString());
    logger.log(Level.INFO, "ADom pids are " + pidsADom.toString());
    logger.log(Level.INFO, "QDom pids are " + pidsQDom.toString());
    logger.log(Level.INFO, "EqualWeight pids are " + pidsEqualWeights.toString());

    Vector<String> atokensQueries = getQueriesByPids(pidsAtokens);
    Vector<String> qtokensQueries = getQueriesByPids(pidsQtokens);
    Vector<String> alltokensQueries = getQueriesByPids(pidsAlltokens);
    Vector<String> aDomQueries = getQueriesByPids(pidsADom);
    Vector<String> qDomQueries = getQueriesByPids(pidsQDom);
    Vector<String> equalWeightQueries = getQueriesByPids(pidsEqualWeights);

    Vector<String> wordsATokens = getTopWordsFromQueries(atokensQueries, gtQuery.size());
    logger.log(Level.INFO, "Atokens result query is " + wordsATokens.toString()); 

    Vector<String> wordsQTokens = getTopWordsFromQueries(qtokensQueries, gtQuery.size());
    logger.log(Level.INFO, "Qtokens result query is " + wordsQTokens.toString()); 

    Vector<String> wordsAllTokens = getTopWordsFromQueries(alltokensQueries, gtQuery.size());
    logger.log(Level.INFO, "Alltokens result query is " + wordsAllTokens.toString()); 

    Vector<String> wordsADom = getTopWordsFromQueries(aDomQueries, gtQuery.size());
    logger.log(Level.INFO, "ADom result query is " + wordsADom.toString()); 

    Vector<String> wordsQDom = getTopWordsFromQueries(qDomQueries, gtQuery.size());
    logger.log(Level.INFO, "QDom result query is " + wordsQDom.toString()); 

    Vector<String> wordsEqualWeight = getTopWordsFromQueries(equalWeightQueries, gtQuery.size());
    logger.log(Level.INFO, "EqualWeight result query is " + wordsEqualWeight.toString()); 

    double rboScoreAtokens = Utils.RBO(wordsATokens, gtQuery, rboCoefficient, gtQuery.size());
    double rboScoreQtokens = Utils.RBO(wordsQTokens, gtQuery, rboCoefficient, gtQuery.size());
    double rboScoreAlltokens = Utils.RBO(wordsAllTokens, gtQuery, rboCoefficient, gtQuery.size());
    double rboScoreADom = Utils.RBO(wordsADom, gtQuery, rboCoefficient, gtQuery.size());
    double rboScoreQDom = Utils.RBO(wordsQDom, gtQuery, rboCoefficient, gtQuery.size());
    double rboScoreEqualWeight = Utils.RBO(wordsEqualWeight, gtQuery, rboCoefficient, gtQuery.size());

    logger.log(Level.INFO, "Atokens RBO score is " + rboScoreAtokens);
    logger.log(Level.INFO, "Qtokens RBO score is " + rboScoreQtokens);
    logger.log(Level.INFO, "Alltokens RBO score is " + rboScoreAlltokens);

    logger.log(Level.INFO, "ADom RBO score is " + rboScoreADom);
    logger.log(Level.INFO, "QDom RBO score is " + rboScoreQDom);
    logger.log(Level.INFO, "EqualWeight RBO score is " + rboScoreEqualWeight);

    writer.println(questID + "\t" + gtQuery.toString() + "\t" + rboScoreAtokens     + "\t" + wordsATokens.toString()   + "\t" +
                                                                rboScoreQtokens     + "\t" + wordsQTokens.toString()   + "\t" + 
                                                                rboScoreAlltokens   + "\t" + wordsAllTokens.toString() + "\t" + 
                                                                rboScoreADom        + "\t" + wordsADom.toString()      + "\t" + 
                                                                rboScoreQDom        + "\t" + wordsQDom.toString()      + "\t" +
                                                                rboScoreEqualWeight + "\t" + wordsEqualWeight.toString());

  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  }
}

public static void recalculateRBO() throws IOException, FileNotFoundException, ParseException {
    // setThingsUp();
    // luc = new LuceneHelper(index_path); 
    FileInputStream gt = new FileInputStream("gtQueries.txt");
    BufferedReader gt_br = new BufferedReader(new InputStreamReader(gt));

    FileInputStream eval = new FileInputStream("CalculatedQueries.txt");
    BufferedReader eval_br = new BufferedReader(new InputStreamReader(eval));

    PrintWriter writer = new PrintWriter(new FileOutputStream(new File("RBOScoresRevisited.txt"), true));



    String gtLine;
    String evalLine;

    while ((evalLine = eval_br.readLine()) != null)   {
      evalLine = evalLine.replace("[", "");
      evalLine = evalLine.replace("]", "");
      evalLine = evalLine.replace(",", "");
      Vector<String> evalQuery = new Vector<String>(Arrays.asList(evalLine.split("\\s")));

      gtLine = gt_br.readLine();
      gtLine = gtLine.replace("[", "");
      gtLine = gtLine.replace("]", "");
      gtLine = gtLine.replace(",", "");
      Vector<String> gtQuery = new Vector<String>(luc.lucene_tokenize(new EnglishAnalyzer(), gtLine));
      // Vector<String> gtQuery = new Vector<String>(Arrays.asList(gtLine.split(",\\s")));

      System.out.println(evalQuery);
      System.out.println(gtQuery);

      double rboScore = Utils.RBO(evalQuery, gtQuery, 0.5, gtQuery.size());
      System.out.println(rboScore);  
      writer.println(rboScore);    
    }

    writer.close();
}

public static void addGoogleSearchDocs() {
    int numDocs = 20;
    int docIDCount = 1;
    int queryID = 1;
    int snippetID = 1;


    try {
      Statement qidsStmt = dbConnection.createStatement();
      String sql = "select qid, gtqueryID, gtquery, rawQuestion from questions";
      ResultSet qids = qidsStmt.executeQuery(sql);
      int testCounter = 40;

      while (qids.next()) {

        int questID = qids.getInt("qid");
        int gtqueryID = qids.getInt("gtqueryID");
        String gtQuery = qids.getString("gtquery");
        String rawQuestion = qids.getString("rawQuestion");

        logger.log(Level.INFO, "Working with question " + questID + "...");

        rawQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(rawQuestion.toLowerCase()));
        Vector<String> questTokens = Utils.str2vect(rawQuestion);
        questTokens = Utils.removeShortTokens(Utils.dropStopWords(questTokens), 2);
        HashSet<String> distinctTokens = new HashSet<String>(questTokens);
        logger.log(Level.INFO, "Question tokens: " + distinctTokens);

        /// kld is not used. Compose queries out of all the question words
        Vector<String> allQueries = Utils.composeQueries1(new Vector<String>(distinctTokens), 3).get(3);  
        logger.log(Level.INFO, "all queries constructed are " + allQueries.size());
        logger.log(Level.INFO, "Start inserting all queries in the database...");

        for (String q : allQueries) {
          
          // sql = "insert into newqueries (queryID, queryText, questID) values (" + queryID + ", ?, " + questID + ");";

          // PreparedStatement insertQueryStmt = dbConnection.prepareStatement(sql);
          // insertQueryStmt.setString(1, q);
          // insertQueryStmt.executeUpdate();
          queryID ++;
          // insertQueryStmt.close();
          // dbConnection.commit();
        }
        logger.log(Level.INFO, "Inserted query #" + queryID + "...");

        Vector<String> randomQueries = Utils.pickRandomSample(allQueries, 150);

        //// build word distribution to make sure all words are equally represented
          /*Vector<String> words = new Vector<String>();
          for (String q : randomQueries) {
            words.addAll(Utils.str2vect(q));
          }

          HashMap<String, Double> distr = Utils.buildDistribution(words);
          for (Entry<String, Double> e : distr.entrySet()) {
            System.out.println(e.getValue() + ":::" + e.getKey());
          }*/


        logger.log(Level.INFO, "Sart stealing from Google...");
        PrintWriter writer = new PrintWriter(new FileOutputStream(new File("QueriesToGoogle.txt"), true));
        for (String curQuery : randomQueries) {

          writer.println(curQuery);

          //// for the time being
            boolean fetchSucceeded = false;
            HashMap<String, String> googleSearchResults = new HashMap<String, String>();
            while (!fetchSucceeded) {
              try {
                googleSearchResults = GoogleSearch.searchTermsSnippets(curQuery, numDocs);
                fetchSucceeded = true;
              }
              catch (Exception e) {
                /// if something went wrong, wait for 5 minutes and try again
                logger.log(Level.INFO, "Something went wrong with fetching snippets. Wait for 5 mins and try again");
                logger.log(Level.INFO, e.getClass().getName() + ": " + e.getMessage() );
                // Thread.sleep(1000*60*5);
              }
            }

            Vector<String> urls = new Vector<String>();
            Vector<String> snippets = new Vector<String>();

            urls.addAll(googleSearchResults.keySet());
            snippets.addAll(googleSearchResults.values());

            logger.log(Level.INFO, "Start inserting stolen snippets in the database...");
            for (int i = 0; i < urls.size(); ++i) {
              String curURL = urls.get(i);
              String curSnippet = snippets.get(i);

              sql = "insert into NewSnippets (snippetID, snippet, docURL, questID, queryText)" +
                      " values (" + snippetID + ", ?, ?, " + questID + ", ?);";
              snippetID ++;

              PreparedStatement addToGoogleSearchStmt = dbConnection.prepareStatement(sql);
              addToGoogleSearchStmt.setString(1, curSnippet);
              addToGoogleSearchStmt.setString(2, curURL);
              addToGoogleSearchStmt.setString(3, curQuery);

              addToGoogleSearchStmt.executeUpdate();
              addToGoogleSearchStmt.close();
              dbConnection.commit();

          } 
        
          // logger.log(Level.INFO, "Inserted snippet #" + snippetID);
          

// also for the time being
  /*          int min = 7000;
            int max = 10000;
            int randomWaitPeriod = rand.nextInt((max - min) + 1) + min;
            logger.log(Level.INFO, "Waiting for " + randomWaitPeriod + " m seconds...");
            Thread.sleep(randomWaitPeriod);
  */


          // testCounter -= 1;
          // if (testCounter == 0) {
          //   break;
          // }
        }
        writer.println("\n");
        writer.close();
        
      }
      qidsStmt.close();
      qids.close();
      dbConnection.commit();
      dbConnection.close();
    } 

    catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    } catch (FileNotFoundException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    // catch (InterruptedException e) {
    //   System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    //   System.exit(0);
    // }
}

public static void addBingSearchResultsFromFile() {  
  try {

    // find the max snippetID to avoid duplicate values in the table
    // String sql = "select max(snippetID) as snippetID from FullProbes;";

    String sql = "select max(snippetID) as snippetID from SnippetsFromKeywords;";
    Statement snippetIDStmt = dbConnection.createStatement();
    ResultSet snippetIDRS = snippetIDStmt.executeQuery(sql);
    int snippetID = snippetIDRS.getInt("snippetID") + 1;
    snippetIDRS.close();
    snippetIDStmt.close();


    // BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("FullProbes/FullProbesSnippets.txt")));
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/home/avtyurin/ComplexQA/data/allGtQueries.txt")));
    
    String strLine;
    while ((strLine = reader.readLine()) != null) {

    /// Read next question in JSON format and initialize. Start.
      try {
        JSONObject jsonObj = new JSONObject(strLine);
        String curQuery = jsonObj.getString("query");
        int questID = jsonObj.getInt("questID");
        System.out.println(curQuery);
        JSONArray snippets = jsonObj.getJSONArray("snippets");
        Vector<String> snippetsStr = Utils.JSONArrayToVect(snippets);
        for (String s : snippetsStr) {
          JSONObject snippetJSON = new JSONObject(s);
          String curSnippet = snippetJSON.getString("snippet");
          String curURL = snippetJSON.getString("url");
          // System.out.println(snippet);
          // System.out.println(url);

/*          String sql = "select queryID, questID from newqueries where queryText=?;";
          PreparedStatement queryIDStmt = dbConnection.prepareStatement(sql);
          queryIDStmt.setString(1, curQuery);
          ResultSet queryIDRS = queryIDStmt.executeQuery();
          int queryID = queryIDRS.getInt("queryID");
          int questID = queryIDRS.getInt("questID");
          queryIDRS.close();
          queryIDStmt.close();*/
          // System.out.println("Query = " + curQuery + "; queryID = " + queryID + "; questID = " + questID);
          // if (input.next().equals(".")){
          //   continue;
          // }



          // sql = "insert into SnippetsFromKeywords (snippetID, questID, snippet, docURL, queryText)" +
          //         " values (" + snippetID + ", " + questID + ", ?, ?, ?);";

          sql = "insert into GTQueries (questID, snippet, docURL, queryText)" +
                " values (" + questID + ", ?, ?, ?);";
          snippetID ++;

          PreparedStatement addToGoogleSearchStmt = dbConnection.prepareStatement(sql);
          addToGoogleSearchStmt.setString(1, curSnippet);
          addToGoogleSearchStmt.setString(2, curURL);
          addToGoogleSearchStmt.setString(3, curQuery);

          addToGoogleSearchStmt.executeUpdate();
          addToGoogleSearchStmt.close();
          dbConnection.commit();
        }
      } catch (JSONException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        System.out.println("\n\n****\n\n");
        continue;
      }

    }
  } catch (FileNotFoundException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  } catch (IOException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  } catch (SQLException e) {
    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    System.exit(0);
  }
}


public static void TestSimMeasures () {
    try {
      Statement qidsStmt = dbConnection.createStatement();
      String sql = "select qid, gtquery, qtokens, atokens from questions;";
      ResultSet qids = qidsStmt.executeQuery(sql);

      while (qids.next()) {
        try {

          int curQuestID = qids.getInt("qid");
          String curGTQuery = qids.getString("gtquery");
          String qtokens = qids.getString("qtokens");
          String atokens = qids.getString("atokens");

          System.out.println("Working with question " + curQuestID + "...");

          // System.out.println(qtokens);
          // System.out.println("\n\n***\n\n");
          // System.out.println(atokens);


          Statement googleDocsStmt = dbConnection.createStatement();
          sql = "select rawHTML from GoogleSearchDocs where questID=" + curQuestID + ";";
          ResultSet googleDocs = googleDocsStmt.executeQuery(sql);

          Vector<String> allPassages = new Vector<String>();

          while(googleDocs.next()) {
            String curDoc = googleDocs.getString("rawHTML");
            curDoc = Jsoup.parse(curDoc).text();
            Vector<String> passages = luc.passageSearchFromDocument(curGTQuery, curDoc, 50);
            // passageRanking(passages, qtokens, atokens);
            allPassages.addAll(passages);
          }

          passageRanking(allPassages, qtokens, atokens);

          googleDocsStmt.close();
          googleDocs.close();

        } catch (SQLException e) {
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          continue;
        } catch (IOException e) {
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          continue;
        } catch (ParseException e) {
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          continue;
        }
        input.next();
      }
      qidsStmt.close();
      qids.close();

    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
}

public static void passageRanking(Vector<String> passages, String question, String answer) {
    //// rank passages based on the number of words intersecting with the question text and the answer text.
    HashMap<String, Integer> pRanking = new HashMap<String, Integer> ();

    for (String p : passages) {
      Set<String> pWordsSet = new HashSet<String>(Arrays.asList(p.split("\\s")));
      Set<String> qWordsSet = new HashSet<String>(Arrays.asList(question.split("\\s")));
      Set<String> aWordsSet = new HashSet<String>(Arrays.asList(answer.split("\\s")));
      
      /*s1.retainAll(s2) — transforms s1 into the intersection of s1 and s2. 
      (The intersection of two sets is the set containing only the elements common to both sets.)*/

      Set<String> aIntersection = new HashSet<String>(pWordsSet);
      Set<String> qIntersection = new HashSet<String>(pWordsSet);

      aIntersection.retainAll(pWordsSet);
      qIntersection.retainAll(pWordsSet);

      int answerIntersectionScore = aIntersection.size();
      int questionIntersectionScore = qIntersection.size();
      
      pRanking.put(p, new Integer(answerIntersectionScore + questionIntersectionScore));
    }

    List<Entry<String, Integer>> rankedP = Utils.entriesSortedByValues(pRanking, "dec");
    System.out.println("Passages size = " + passages.size());
    System.out.println("RankedP size = " + rankedP.size());

    int topN = 10;
    int pCounter = 0;
    // // for (int i = 0; i < topN; ++i) {
    // //   System.out.println(rankedP.get(i).getValue() + ":::" + rankedP.get(i).getKey() + "\n\n******\n");
    // // }
    for (Entry<String, Integer> e : rankedP) {
      System.out.println(e.getValue() + ":::" + e.getKey() + "\n******\n");

      pCounter ++;
      if (pCounter == topN) {
        break;
      }
    }
}


public static void populateDBWithRawQA() {
  Vector<Integer> validQids = new Vector<Integer>();
  String sql = "";

  /// get all the qids for questions existing in the database
    try {
      Statement qidsStmt = dbConnection.createStatement();
      sql = "select qid from questions";
      ResultSet qidsRS = qidsStmt.executeQuery(sql);

      while (qidsRS.next()) {
        validQids.add(new Integer(qidsRS.getInt("qid")));
      }

      qidsRS.close();
      qidsStmt.close();
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }

  /// get raw text from json file
  /// and insert all answers into our database

    String strLine;
    int answerID = 1;
    try {
      while ((strLine = br.readLine()) != null)   {
        /// Read next question in JSON format and initialize. Start.
        JSONObject jsonObj = new JSONObject(strLine);
        int questID = jsonObj.getInt("id");
        if (!(validQids.contains(questID))) {

          continue;
        }

        Vector<String> answers = Utils.JSONArrayToVect(jsonObj.getJSONArray("answers"));
        String bestAnswer = "";
        String qtitle = "";
        String qbody = "";

        String qtQuery = "";
        try {
          bestAnswer = jsonObj.getString("best");
          qtitle = jsonObj.getString("title");
          qbody = jsonObj.getString("body");

          Joiner joiner = Joiner.on(" ");
          qtQuery = joiner.join(Utils.JSONArrayToVect(jsonObj.getJSONArray("gt_query")));

        } catch (JSONException e) {
          System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }

        String rawQuestion = qtitle + " " + qbody;

        /// now insert raw question text to Questions table

          // sql = "update questions set rawQuestion=? where qid=" + questID + ";";
          sql = "update questions set gtquery=? where qid=" + questID + ";";

          PreparedStatement addRawQTextStmt = dbConnection.prepareStatement(sql);
          // addRawQTextStmt.setString(1, rawQuestion);
          addRawQTextStmt.setString(1, qtQuery);
          addRawQTextStmt.executeUpdate();
          addRawQTextStmt.close();

          
  /*          int isBestAnswer = 0;
          for (String a : answers) {
            if (a.equals(bestAnswer)) {
              isBestAnswer = 1;
            }

            sql = "insert into answers (answerID, questID, answerText, isBest) values (" + 
                   answerID + ", " + questID + ", ?, " + isBestAnswer + ");";
            PreparedStatement insertAnswerStmt = dbConnection.prepareStatement(sql);
            insertAnswerStmt.setString(1, a);
            insertAnswerStmt.executeUpdate();
            insertAnswerStmt.close();

            answerID += 1;
            isBestAnswer = 0;
          }*/
          dbConnection.commit();

      }
    } catch (IOException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    } catch (JSONException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
}











}

//// USEFUL CODE SNIPPETS

  //// 1. Loop through the questions from DB
    /*
    try {
      Statement qidsStmt = dbConnection.createStatement();
      String sql = "select qid, gtquery, qtokens, atokens from questions;";
      ResultSet qids = qidsStmt.executeQuery(sql);

      while (qids.next()) {
          int curQuestID = qids.getInt("qid");
          // do something here
      }
      qidsStmt.close();
      qids.close();

    } catch (SQLException e) {
      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      System.exit(0);
    }
    */


///// SOME CODE THAT MIGHT BE USEFUL

























































        // populateDBWithRawQA();
        // TestSimMeasures();
        // addGoogleSearchDocs();
        // GoogleSearch.searchTerms("dogs eat rocks", 16);
        // addGTQueriesToDB();
        // checkIfAnswersWereAddedToIndex();
        // recalculateRBO();


        // System.out.println("DONE");
        // input.next();
        

        /*String sql = "";
        Statement stmt = null;
        try {

        	stmt = dbConnection.createStatement();
        } catch (SQLException e) {
        	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
    	  	System.exit(0);
        }
        
        String strLine;

        // PrintWriter writer = new PrintWriter(new FileOutputStream(new File("PassageWiseSimMeasures.txt"), true));

        /// working with questions 1-30
        int minQID = 1;
        int maxQID = 30;
        while ((strLine = br.readLine()) != null)   {

          /// Read next question in JSON format and initialize. Start.
            JSONObject jsonObj = new JSONObject(strLine);
            int questID = jsonObj.getInt("id");

            Vector<String> gtQuery = Utils.JSONArrayToVect(jsonObj.getJSONArray("gt_query"));
            Joiner joiner = Joiner.on(" ");
            Vector<String> analyzedGtQuery = new Vector<String>(luc.lucene_tokenize(new EnglishAnalyzer(), joiner.join(gtQuery)));
            System.out.println(analyzedGtQuery.toString());

            if (! (questID >= minQID) && (questID <= maxQID) ) {
              continue;
            }
            // logger.log(Level.INFO, "Working with question " + questID + "...");
            // averageSim(questID, analyzedGtQuery, writer);
            // passageWiseSim(questID, analyzedGtQuery, writer);

            // writer.flush();
            


            
  /// Calculating similarity and writing to the db
            try {
            	sql = "select atokens, qtokens from questions where qid=" + questID + ";";

            	ResultSet qaTokens = stmt.executeQuery(sql);
            	Vector<String> qtokens = Utils.str2vect(qaTokens.getString("qtokens"));
            	Vector<String> atokens = Utils.str2vect(qaTokens.getString("atokens"));
            	Vector<String> allTokens = new Vector<String>(qtokens);
            	allTokens.addAll(atokens);

            	sql = "select pid from qqp where questid=" + questID + ";";
            	ResultSet pids = stmt.executeQuery(sql);	
            	Statement passageStmt = dbConnection.createStatement();

            	Statement addScoreStmt = dbConnection.createStatement();

            	while (pids.next()) {
            		int pid = pids.getInt("pid");
            		sql = "select passage from passages where pid=" + pid + ";";
            		ResultSet curPassageResultSet = passageStmt.executeQuery(sql);
            		String curPassage = curPassageResultSet.getString("passage");

            		Vector<String> curPassageTokens = Utils.str2vect(curPassage);

            		// /// SIMILARITY TIME BABY!
            		double aSim = Utils.KLD_JelinekMercerSmoothing(atokens, curPassageTokens, 0.9, luc);
            		double qSim = Utils.KLD_JelinekMercerSmoothing(qtokens, curPassageTokens, 0.9, luc);
            		double allSim = Utils.KLD_JelinekMercerSmoothing(allTokens, curPassageTokens, 0.9, luc);
            		sql = "insert into KLDSim (pid, questid, atokensSim, qtokensSim, alltokensSim) values (" + pid + 
            		", " + questID + ", " + aSim + ", " + qSim + ", " + allSim + ");";
            		addScoreStmt.executeUpdate(sql);
                curPassageResultSet.close();
            	}
              passageStmt.close();
              addScoreStmt.close();
              
              pids.close();

            } catch (SQLException e) {
            	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            	continue;
            } 


  //           /// ***** Compare passage and answers using repeated words start 

  //           // Vector<Entry<String, Double>> passagesIntersection = Utils.answersIntersection(passages, 0.5, luc);

  //           // // get high repetition words
  //           // Vector<String> passagesIntersectionWords = new Vector<String>();
  //           // for (Entry<String, Double> e : passagesIntersection) {
  //           //   // System.out.println (e.getKey() + " --- " + e.getValue());
  //           //   passagesIntersectionWords.add(e.getKey());
  //           // }

  //           // // find how many of them were in the answers
  //           // passagesIntersectionWords.retainAll(answersIntersectionWords);

  //           // System.out.println(i + ". " + current_query);
  //           // System.out.println("Frequent words in passages");
  //           // for (Entry<String, Double> e : passagesIntersection) {
  //           //   System.out.println (e.getKey() + " --- " + e.getValue());
  //           // }

  //           // System.out.println("Frequent words in answers");
  //           // System.out.println(answersIntersectionWords);

  //           // System.out.println("These words intersect");
  //           // System.out.println(passagesIntersectionWords);
            
  //           // double score = (double)((double)passagesIntersectionWords.size() / (double)answersIntersectionWords.size());

  //           // if (score > maxScore) {
  //           //   maxScore = score;
  //           // }

  //           // System.out.println(score);
  //           // queryRanking.put(current_query, new Double(score));
  //           // // input.next();

  //           /// ***** Compare passage and answers using repeated words end        
      
      }

      try {
        
  	    stmt.close();
  	    dbConnection.commit();
  	    dbConnection.close();
      } catch (SQLException e) {
  	  	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
  	  	System.exit(0);
      }
      // writer.close();
      br.close();*/
  


