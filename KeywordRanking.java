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

public static Vector<String> passagesForQuestion(int questID) {
	if (dbConnection == null) {
		System.out.println("Setup db connection properly. Exiting...");
		return null;
	}
	return null;
}

public static void loggerSetup() {
  try {
    FileHandler fileHandler = new FileHandler("ComparingPassagesLog.%u.%g.txt");
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

public static void setThingsUp(String[] args) throws IOException, FileNotFoundException, ParseException {
  index_path = args[0].toString();
  luc = new LuceneHelper(index_path); 
  loggerSetup();
  query = args[1].toString();
  decimalFormat = new DecimalFormat("#.#");
  setupDBConnection();
  fstream = new FileInputStream("gtQuestions.txt");
  br = new BufferedReader(new InputStreamReader(fstream));
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
      Vector<String> yahooDocs = luc.searchYahooDocs("clueweb09-en0005-53-00116", 30); // "Answer.0.14.yahoo.txt"

      for (String s : yahooDocs) {
        System.out.println(s);
        input.next();
      }

      System.out.println("Done");
      input.next();
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

public static Vector<String> getQueriesTextByIDs(Vector<Integer> queryIDs) {
  Vector<String> queriesText = new Vector<String>();

  for (Integer qid : queryIDs) {
    try {
      String sql = "select query from queries where qid=" + qid + ";";
      Statement queryTextStmt = dbConnection.createStatement(); 

      ResultSet queryText = queryTextStmt.executeQuery(sql);
      queriesText.add(queryText.getString("query"));

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

  Vector<String> wordsEqualWeighttokens= getTopWordsFromQueries(queriesTextEqualWeighttokens, gtQuery.size());
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

  public static void main(String[] args) throws IOException, ParseException, JSONException, FileNotFoundException, ParseException {
      if (args.length < 2) {
          System.out.println("Input arguments: index_path, query string");
          return;
      }
      setThingsUp(args);

      String sql = "";
      Statement stmt = null;
      try {
      	stmt = dbConnection.createStatement();
      } catch (SQLException e) {
      	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
  	  	System.exit(0);
      }
      
      String strLine;

      PrintWriter writer = new PrintWriter(new FileOutputStream(new File("AveSimMeasures.txt"), true));

      /// working with questions 1-30
      int minQID = 1;
      int maxQID = 30;
      while ((strLine = br.readLine()) != null)   {

        /// Read next question in JSON format and initialize. Start.
          JSONObject jsonObj = new JSONObject(strLine);
          int questID = jsonObj.getInt("id");

          Vector<String> gtQuery = Utils.JSONArrayToVect(jsonObj.getJSONArray("gt_query"));

          if (! (questID >= minQID) && (questID <= maxQID) ) {
            continue;
          }
          logger.log(Level.INFO, "Working with question " + questID + "...");
          averageSim(questID, gtQuery, writer);
          writer.flush();
          

          
/// Calculating similarity and writing to the db
/*          try {
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
          }*/


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
    writer.close();
    br.close();
  }
}


