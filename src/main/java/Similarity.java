import java.util.*;
import java.sql.*;
import java.util.Map.*;
import com.google.common.base.Joiner;
import java.io.*;
import org.apache.lucene.queryparser.classic.ParseException;

public class Similarity {
//// contains static methods for computing similarities btw snippets and QA pairs
  private LuceneHelper luc; 
  private Connection dbConnection = null;
  private Scanner input = new Scanner(System.in);
  
  public Similarity(LuceneHelper _luc, Connection _dbConnection) {
  	this.luc = _luc;
  	this.dbConnection = _dbConnection;
  }

  //// SIMILARITY BASED ON INTERSECTION OF SNIPPETS AND QUESTION AND ANSWERS
    public Vector<String> getTopQuestionWordsNoScore (String question, int topNumWords) {
      //// given a question returns top N words according to pointwise KLD score
      List<Entry<String, Double>> highKLDQuestion = this.getTopQuestionWordsWithScore(question, topNumWords);
      Vector<String> topQuestionWords = new Vector<String>();
      for (Entry<String, Double> e : highKLDQuestion) {
        topQuestionWords.add(e.getKey());
      }
      return topQuestionWords;
    }



    public List<Entry<String, Double>> getTopQuestionWordsWithScore (String question, int topNumWords) {
     String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
      Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));  
      qTokens = Utils.dropStopWords(qTokens);
      qTokens = Utils.removeShortTokens(qTokens, 2);

      /// we calculate KLD of all words in the question and we want to give to those in the title higher weight
      List<Entry<String, Double>> allKLDQuestion = Utils.pointwiseKLD(qTokens, luc);


      // now we sort words by their KLD score
      Collections.sort(allKLDQuestion, new Comparator<Entry<String, Double>>() {
        @Override
        public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
          return e2.getValue().compareTo(e1.getValue()); 
        }
      });

      /// if there's no topNumWords, we just return the entire list
      if (topNumWords == -1 ){
        topNumWords = allKLDQuestion.size();
      }

      List<Entry<String, Double>> highKLDQuestion = Utils.sliceCollection(allKLDQuestion, 0, topNumWords);
      return highKLDQuestion;
    }


    public Vector<String> getTopQuestionWordsReweightingNoScore(String question, String title, String body, int topNumWords) {//, Vector<String> importantTokens) {

      /// first get all the words with their KLD score and then reweight those in the titile, question setences and repeating bigrams
      /// we calculate KLD of all words in the question and we want to give to those in the title higher weight

      List<Entry<String, Double>> highKLDQuestion = this.getTopQuestionWordsReweightingWithScore(question, title, body, topNumWords);
      Vector<String> topQuestionWords = new Vector<String>();
      for (Entry<String, Double> e : highKLDQuestion) {
        topQuestionWords.add(e.getKey());
      }
      return topQuestionWords;
    }

    public List<Entry<String, Double>> getTopQuestionWordsReweightingWithScore(String question, String title, String body, int topNumWords) {//, Vector<String> importantTokens) {

      /// first get all the words with their KLD score and then reweight those in the titile, question setences and repeating bigrams
      /// we calculate KLD of all words in the question and we want to give to those in the title higher weight
      List<Entry<String, Double>> allKLDQuestion = this.getTopQuestionWordsWithScore(question, topNumWords);

      List<Entry<String, Double>> highKLDQuestionBeforeReweighting = allKLDQuestion;

      Vector<String> wordsFromQuestionSent = Keywords.wordsFromQuestionSentences(question);

      Vector<String> blocks = Keywords.splitQuestionIntoBlocks(question);
      Vector<String> wordsFromRepBigrams = Keywords.wordsFromRepeatedBigrams(blocks);

      for (Entry<String, Double> e : allKLDQuestion) {
        if (title.toLowerCase().contains(e.getKey())) {
          e.setValue(e.getValue() * 1.5);
        }

        if (wordsFromQuestionSent.contains(e.getKey())) {
          e.setValue(e.getValue() * 1.5); 
        }

        if (wordsFromRepBigrams.contains(e.getKey())) {
          e.setValue(e.getValue() * 1.5); 
        }
      }

      // now we sort reweighted words
      Collections.sort(allKLDQuestion, new Comparator<Entry<String, Double>>() {
        @Override
        public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
          return e2.getValue().compareTo(e1.getValue()); 
        }
      });

      if (topNumWords == -1 ){
        topNumWords = allKLDQuestion.size();
      }

      List<Entry<String, Double>> highKLDQuestion = Utils.sliceCollection(allKLDQuestion, 0, topNumWords);
      return highKLDQuestion;
    }


     public Vector<String> getTopAnswerWords(Vector<String> answers, int topNumWords) {
    //// given a set of questions return top N words 
      if (answers.size() < 3) { // if there're less than 3 answers, concat all answers and find words using KLD
        String collectiveAnswer = Utils.join_vector(answers, " ");
        return getTopQuestionWordsNoScore(collectiveAnswer, topNumWords);
      }

      // if there're more than 3 answers, find words that are repeated between answers 
      Vector<Vector<String>> cleanAnswers = new Vector<Vector<String>>();
      for (String a: answers) {
        String newAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(a.toLowerCase()));
        Vector<String> aTokens = new Vector<String>(Arrays.asList(newAnswer.split("\\s")));
        aTokens = Utils.dropStopWords(aTokens);
        aTokens = Utils.removeShortTokens(aTokens, 2);
        cleanAnswers.add(aTokens);
      }

      List<Entry<String, Double>> topScoredWords = Utils.getTopWordsFromAnswers(cleanAnswers, luc, topNumWords);
      Vector<String> topAnswersWords = new Vector<String>();
      for (Entry<String, Double> e : topScoredWords) {
        topAnswersWords.add(e.getKey());
      }
      return topAnswersWords;
    }



    public double averageIntersection(Vector<Snippet> snippets, Vector<String> tokens_) {
    //// given a list of snippets, find an average number of intersecting words with given list of tokens

      // the batch of snippets here is supposed to be produced by a single probe
      // we want to exclude the words in the probe from consideration
      
      double accumScore = 0.0;


      Vector<String> tokens = new Vector<String>();

      for (String s : tokens_) {
        String newStr = new String(s);
        tokens.add(newStr);
      }
      Vector<String> probeTokens = new Vector<String>(Arrays.asList(snippets.get(0).queryText.split("\\s")));
      // System.out.println("probe tokens :: " + probeTokens);
      // System.out.println("importatnt tokens  :: " + tokens);
      // tokens.removeAll(probeTokens);

      double length = tokens.size() * 1.0;

      for (Snippet s : snippets) {
        // System.out.println("snippet :: " + s.original);
        // System.out.println("snippet tokens :: " + s.tokens());
        HashSet<String> sTokens = new HashSet<String>(s.tokens());

        sTokens.removeAll(probeTokens);
        sTokens.retainAll(tokens);
        System.out.println(sTokens);
        accumScore += sTokens.size();

      }

      double aveIntersection = accumScore / (snippets.size() * 1.0);
      double aveScore = aveIntersection / length;

      
      if (Double.isNaN(aveScore)) {
        System.out.println("Tokens :: " + tokens);
        // System.out.println("Probe tokens :: " + probeTokens);
        
        input.next();
      }

      // System.out.println("Ave intersection :: " + aveScore);
      return aveScore;
    }

    public double allSnippetsIntersection(Vector<Snippet> snippets, Vector<String> tokens_) {
    //// Create one big snippet from all the given snippets, and find its intersection with the givem list of tokens
      HashSet<String> snippetsTokens = new HashSet<String>();
      Vector<String> probeTokens = new Vector<String>(Arrays.asList(snippets.get(0).queryText.split("\\s")));

      Vector<String> tokens = new Vector<String>();

      for (String s : tokens_) {
        String newStr = new String(s);
        tokens.add(newStr);
      }

      // tokens.removeAll(probeTokens);
      double length = tokens.size() * 1.0;

      for (Snippet s : snippets) {
        // System.out.println("snippet :: " + s.original);
        snippetsTokens.addAll(s.tokens());
      }
      snippetsTokens.removeAll(probeTokens);
      snippetsTokens.retainAll(tokens);
      System.out.println(snippetsTokens);

      double allIntersect = snippetsTokens.size()* 1.0 / length; 

      // System.out.println("All intersection :: " + allIntersect);

      return allIntersect;
    }

    public double scoreForSingleProbe(Vector<String> topQuestionWords, Vector<String> topAnswersWords, Vector<Snippet> snippets, 
      double aveQuestionIntersectionWeight, double aveAnswersIntersectionWeight, double questionIntersectionWeight, double answersIntersectionWeight) {
    //// given a list of snippets for a probe and quesion's and answers' top words, find a score for this probe
      //// average intersection with question across snippets
      // System.out.println("top question words :: " +  topQuestionWords);
      // System.out.println("top answers words :: " +  topAnswersWords);

      System.out.println("Average question intersection :: ");
      double aveQuestionIntersection = this.averageIntersection(snippets, topQuestionWords);      
      System.out.println(aveQuestionIntersection);
      System.out.println("Average answers intersection :: ");
      double aveAnswersIntersection = this.averageIntersection(snippets, topAnswersWords);
      System.out.println(aveAnswersIntersection);
      // System.out.println("aveQuestionIntersection :: " + aveQuestionIntersection);
      // System.out.println("aveAnswersIntersection :: " + aveAnswersIntersection);

      //// now intersection of all snippets with answer
      // System.out.println(topQuestionWords);
      // System.out.println(topAnswersWords);

      System.out.println("All question intersection :: ");
      double questionIntersection = this.allSnippetsIntersection(snippets, topQuestionWords);
      System.out.println(questionIntersection);
      System.out.println("All answers intersection :: ");
      double answersIntersection = this.allSnippetsIntersection(snippets, topAnswersWords);
      System.out.println(answersIntersection);

      // System.out.println("questionIntersection :: " + questionIntersection);
      // System.out.println("answersIntersection :: " + answersIntersection);


      // double probeScore = 0.125*aveQuestionIntersection + 0.375*aveAnswersIntersection + 0.125*questionIntersection + 0.375*answersIntersection;
      double probeScore = aveQuestionIntersectionWeight*aveQuestionIntersection 
                        + aveAnswersIntersectionWeight*aveAnswersIntersection
                        + questionIntersectionWeight*questionIntersection
                        + answersIntersectionWeight*answersIntersection;
      return probeScore;
    }

//// OTHER SIMILARITY MEASURES THAT DID NOT REALLY WORK
  public Vector<Snippet> simpleIntersection (String question, Vector<String> answers, Vector<Snippet> snippets, String query) {
  /// only do slight preprocessing - remove punctuation (no stopping or stemming)
  /// compare words intersection - (question + collective answer) and (snippet)

    String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
    Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));

    Joiner joiner = Joiner.on(" ");
    String collectiveAnswer = Utils.shrinkRepeatedChars(joiner.join(answers));
    collectiveAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(collectiveAnswer.toLowerCase()));
    Vector<String> aTokens = new Vector<String>(Arrays.asList(collectiveAnswer.split("\\s")));

    HashSet<String> qaWords = new HashSet<String>(qTokens);
    qaWords.addAll(aTokens);

    qaWords.removeAll(Arrays.asList(query.split("\\s")));


    for (Snippet snippet : snippets) {
      HashSet<String> sWords = new HashSet<String>(snippet.tokens());
      double sLength = 1.0 * sWords.size();
      sWords.retainAll(qaWords);
      double score = sWords.size() / sLength;
      snippet.setSimScore(score);
    }
    return snippets;
  }

  public Vector<Snippet> bigramsIntersection(String question, Vector<String> answers, Vector<Snippet> snippets) {
    //// count the number of intersecting bigrams in the snippet and question/answer
    HashSet<String> questionBigrams = new HashSet<String>(Utils.bigramsFromSentence(question));

    Vector<String> answersBigrams = new Vector<String>();

    for (String a : answers) {
      answersBigrams.addAll(Utils.bigramsFromSentence(a));
    }
    System.out.println(Utils.entriesSortedByValues(Utils.buildDistribution(answersBigrams), "dec"));

    HashSet<String> allBigrams = new HashSet<String>(questionBigrams);
    allBigrams.addAll(answersBigrams);

    for (Snippet s : snippets) {
      s.generateBigrams();
      // System.out.println("SNIPPET: " + s.original);
      // System.out.println("BIGRAMS: " + s.getBigrams());
      // input.next();
      HashSet<String> intersectionBigrams = new HashSet<String>(s.getBigrams());
      double total = s.getBigrams().size();

      intersectionBigrams.retainAll(allBigrams);
      double intersect = intersectionBigrams.size();

      
      if (intersectionBigrams.size() != 0) {
        // System.out.println(intersectionBigrams);
        // System.out.println(s.original);
        // System.out.println(s.queryText + "\n\n");
      }

      s.setSimScore(intersect);

    }
    return snippets;
  }

  public Vector<Snippet> kldSimilarity(String question, Vector<String> answers, Vector<Snippet> snippets) {
  //// Concat together question, collective answer and use it to compute KLD btw that and each snippet   
    String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
    Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));

    Joiner joiner = Joiner.on(" ");
    String collectiveAnswer = Utils.shrinkRepeatedChars(joiner.join(answers));
    collectiveAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(collectiveAnswer.toLowerCase()));
    Vector<String> aTokens = new Vector<String>(Arrays.asList(collectiveAnswer.split("\\s")));

    aTokens.addAll(qTokens);
    Vector<String> background = new Vector<String>(aTokens);
    int minShortTokenLength = 2;
    background = Utils.removeShortTokens(background, minShortTokenLength);

    for (Snippet s : snippets) {
      try {
        // input.next();
        Vector<String> foreground = Utils.removeShortTokens(s.tokens(), minShortTokenLength);
        // System.out.println("foreground = " + foreground);
        // System.out.println("background = " + background);
        double kldSimScore = Utils.KLD_JelinekMercerSmoothing(foreground, background, 0.9, luc);
        s.setSimScore(kldSimScore);

        System.out.println(s.original);
        System.out.println(kldSimScore);
        System.out.println("\n***");
      } catch (IOException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        continue;
      } catch (ParseException e) {
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        continue;
      }
    }

    return snippets;
  }

  public Vector<Snippet> simpleIntersectionTopWords(String question, Vector<String> answers, Vector<Snippet> snippets, double questSimWeight, double answerSimWeight, PrintWriter writer) {
  /// The score is the percentage of snippet words intersecting with top words from question and top words from answer
  /// Top words from question are the words with the highest pointwise KLD score (or it can be only title words)
  /// Top words from the answers are the words repeating throughout the answers (redundancy)

    // get top words from the question
    String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
    Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));
    qTokens = Utils.removeShortTokens(qTokens, 2);

    List<Entry<String, Double>> highKLDQuestion = Utils.sliceCollection(Utils.pointwiseKLD(qTokens, luc),0, 5);
    // System.out.println("quest::: " + highKLDQuestion);

    Vector<String> topQuestionWords = new Vector<String>();
    for (Entry<String, Double> e : highKLDQuestion) {
      topQuestionWords.add(e.getKey());
    }

    Vector<String> cleanAnswers = new Vector<String>();
    for (String a: answers) {
      String newAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(a.toLowerCase()));
      Vector<String> aTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));
      aTokens = Utils.removeShortTokens(aTokens, 2);
      newAnswer = Utils.join_vector(aTokens, " ");

      cleanAnswers.add(newAnswer);
    }

    Vector<Entry<String, Double>> repAnswers = Utils.answersIntersection(cleanAnswers, 0.4, luc);
    List<Entry<String, Double>> sortedRepAnswers = Utils.entriesSortedByValues(repAnswers, "dec");

    Vector<Entry<String, Double>> copySortedRepAnswers = new Vector<Entry<String, Double>>();
    copySortedRepAnswers.addAll(sortedRepAnswers);

    List<Entry<String, Double>> topEntriesAnswers = Utils.sliceCollection(copySortedRepAnswers ,0, 5);
    // System.out.println("asw::: " + topEntriesAnswers);
    Vector<String> topAnswersWords = new Vector<String>();
    for (Entry<String, Double> e : topEntriesAnswers) {
      topAnswersWords.add(e.getKey());
    }

    HashSet<String> topWords = new HashSet<String>();
    topWords.addAll(topAnswersWords);
    topWords.addAll(topQuestionWords);
    // System.out.println("TOTAL:: " + topWords); 

    writer.println("::TOP WORDS FROM QUESTION:: " + topQuestionWords);
    writer.println("::TOP WORDS FROM ANSWERS:: " + topAnswersWords);



    for (Snippet snippet : snippets) {
      HashSet<String> sWords = new HashSet<String>(snippet.tokens());

      Vector<String> queryTokens = new Vector<String>(Arrays.asList(snippet.queryText.split("\\s")));
      sWords.removeAll(queryTokens);

      Vector<String> copySWords = new Vector<String> (sWords);



      double sLength = 1.0 * sWords.size();
      sWords.retainAll(topQuestionWords);
      double questionScore = sWords.size()*1.0 / sLength;

      copySWords.retainAll(topAnswersWords);
      double answersScore = copySWords.size()*1.0 / sLength;


      snippet.setSimScore(questSimWeight*questionScore + answerSimWeight*answersScore);
    }
    return snippets;
  
  }

    public Vector<Snippet> simpleIntersectionNoQuery (String question, Vector<String> answers, Vector<Snippet> snippets) {
  /// only do slight preprocessing - remove punctuation (no stopping or stemming)
  /// % of intersectiong words that were not in the query
  /// ((question + answer) \ (query) ) and (snippet)

    String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
    Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));

    Joiner joiner = Joiner.on(" ");
    String collectiveAnswer = Utils.shrinkRepeatedChars(joiner.join(answers));
    collectiveAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(collectiveAnswer.toLowerCase()));
    Vector<String> aTokens = new Vector<String>(Arrays.asList(collectiveAnswer.split("\\s")));

    HashSet<String> qaWords = new HashSet<String>(qTokens);
    qaWords.addAll(aTokens);
    // qaWords.removeAll(Arrays.asList(query.split("\\s")));


    for (Snippet snippet : snippets) {
      Vector<String> queryTokens = new Vector<String>(Arrays.asList(snippet.queryText.split("\\s")));
      HashSet<String> sWords = new HashSet<String>(snippet.tokens());
      // System.out.println(sWords);
      sWords.removeAll(queryTokens);

      HashSet<String> qaWordsNoQuery = new HashSet<String>(qaWords);
      qaWordsNoQuery.removeAll(queryTokens);

      
      // System.out.println(snippet.queryText);
      // System.out.println(sWords);
      // System.out.println(qaWordsNoQuery);

      double sLength = 1.0 * sWords.size();
      sWords.retainAll(qaWordsNoQuery);
      double score = sWords.size() / sLength;
      snippet.setSimScore(score);


      // input.next();
    }
    return snippets;
  }

  public Vector<Snippet> simpleIntersectionWeighted(String question, Vector<String> answers, Vector<Snippet> snippets, double questSimWeight, double answerSimWeight) {
  /// Do simple intersection between question and snippet, answer and snippet 
  /// The final score is composed of  questSimWeight * SimWithQuestion + answerSimWeight * SimWithAnswer
    String processedQuestion = Utils.shrinkRepeatedChars(Utils.removePunct(question.toLowerCase()));
    Vector<String> qTokens = new Vector<String>(Arrays.asList(processedQuestion.split("\\s")));

    Joiner joiner = Joiner.on(" ");
    String collectiveAnswer = Utils.shrinkRepeatedChars(joiner.join(answers));
    collectiveAnswer = Utils.shrinkRepeatedChars(Utils.removePunct(collectiveAnswer.toLowerCase()));
    Vector<String> aTokens = new Vector<String>(Arrays.asList(collectiveAnswer.split("\\s")));

    for (Snippet snippet : snippets) {
      Vector<String> queryTokens = new Vector<String>(Arrays.asList(snippet.queryText.split("\\s")));
      HashSet<String> sWords = new HashSet<String>(snippet.tokens());
      sWords.removeAll(queryTokens);
      double sLength = 1.0 * sWords.size();

      // sim with question
      qTokens.removeAll(queryTokens);
      sWords.retainAll(qTokens);
      double questScore = sWords.size() / sLength;

      // sim with answers
      sWords = new HashSet<String>(snippet.tokens());
      sWords.removeAll(queryTokens);
      aTokens.removeAll(queryTokens);
      sWords.retainAll(aTokens);
      double answerScore = sWords.size() / sLength;

      double finalScore = questSimWeight * questScore + answerSimWeight * answerScore;
      snippet.setSimScore(finalScore);
    }
    return snippets;

  }


}