import java.util.*;
import java.util.Map.*;
import java.util.regex.*;


public class Keywords {
	public Keywords() {}

	public static Vector<String> splitQuestionIntoBlocks(String question) {
		String processedQuestion = Utils.shrinkRepeatedChars(question.toLowerCase());

		Vector<String> blocks = new Vector<String>(Arrays.asList(processedQuestion.split("[,.?!]")));
    Vector<String> cleanBlocks = new Vector<String>();
    for (String b : blocks) {
      String cleanBlock = Utils.removePunct(b);
      Vector<String> tokens = Utils.str2vect(cleanBlock);
      tokens = Utils.dropStopWords(tokens);
      tokens = Utils.removeShortTokens(tokens, 2);
      tokens = Utils.s_stemmer(tokens);
      cleanBlock = Utils.join_vector(tokens, " ");

      if (cleanBlock.trim().isEmpty()) {
        continue;
      }
      cleanBlocks.add(cleanBlock);  
      
    }

		// System.out.println("****");
		// for (String b : cleanBlocks) {
		// 	System.out.println(b);	
		// }
		// System.out.println("****");
		return cleanBlocks;
	}


  public static Vector<String> wordsFromRepeatedBigrams(Vector<String> blocks) {
  /// split the question into bigrams (within a block - btw two punctuation marks)
  /// build a list of bigrams and see which ones are repeated two or more times. 
  /// The words from these bigrams go to the list

    Vector<String> repWords = new Vector<String>();
    Vector<String> shingles = new Vector<String>();

    

    for (String b : blocks) {
      Vector<String> tokens = Utils.str2vect(b);
      int length = tokens.size();
      if (length == 1) {
        continue;
      }

      for (int i = 0; i < length - 1; ++i) {
        String bigram = tokens.get(i) + " " + tokens.get(i + 1);
        shingles.add(bigram);
      }
    }
    List<Entry<String, Integer>> sortedShingles = Utils.entriesSortedByValues(Utils.buildFrequency(shingles), "dec");
    // System.out.println("IMPORTANT BIGRAMS");
    for (Entry<String, Integer> e : sortedShingles) {
      if (e.getValue() > 1) {
        // System.out.println(e.getKey() + "  ::  " + e.getValue());    
        Vector<String> words = Utils.str2vect(e.getKey());
        repWords.addAll(words);
      } else {
        break;
      }
      
    }
    
    return repWords;
  }


  public static Vector<String> wordsFromQuestionSentences(String question) {
  /// returns a list of words from question sentences. 

    // extract pieces of text that end with "?". These will be our question sentences
    Vector<String> resWords = new Vector<String>();

    Pattern re = Pattern.compile("([^.!?\\s][^.!?]*[?])");
    Matcher reMatcher = re.matcher(question);
    while (reMatcher.find()) {
      String singleSentence = reMatcher.group();
      singleSentence = Utils.removePunct(singleSentence);

      Vector<String> tokens = Utils.str2vect(singleSentence);
      tokens = Utils.dropStopWords(tokens);
      tokens = Utils.removeShortTokens(tokens, 2);
      tokens = Utils.s_stemmer(tokens);
      resWords.addAll(tokens);
    }
    
    return resWords;
  }

  public static Vector<String> extractKeywordsFromQuestion(String rawQuestion, String qtitle, String qbody, Similarity similarity) {
    // keywords from question sentences
    String processedQBody = Utils.shrinkRepeatedChars(qbody.toLowerCase());
    Vector<String> qSentWords = Keywords.wordsFromQuestionSentences(processedQBody);

    // keywords from repeated bigrams
    Vector<String> blocks = Keywords.splitQuestionIntoBlocks(rawQuestion);
    Vector<String> repBigramWords = Keywords.wordsFromRepeatedBigrams(blocks);

    Vector<String> importantWords = new Vector<String>(repBigramWords);
    importantWords.addAll(qSentWords);

    Vector<String> topQuestionWords = similarity.getTopQuestionWords(rawQuestion, qtitle, qbody, 10, importantWords);
    return topQuestionWords;
  }

  public static Vector<Entry<String, Double>> extractKeywordsFromQuestionWithCertainty(String rawQuestion, String qtitle, String qbody, Similarity similarity, double cutoff) {
    // keywords from question sentences
    String processedQBody = Utils.shrinkRepeatedChars(qbody.toLowerCase());
    Vector<String> qSentWords = Keywords.wordsFromQuestionSentences(processedQBody);

    // keywords from repeated bigrams
    Vector<String> blocks = Keywords.splitQuestionIntoBlocks(rawQuestion);
    Vector<String> repBigramWords = Keywords.wordsFromRepeatedBigrams(blocks);

    Vector<String> importantWords = new Vector<String>(repBigramWords);
    importantWords.addAll(qSentWords);

    Vector<Entry<String, Double>> topQuestionWords = similarity.getTopQuestionWordsWithCertainty(rawQuestion, qtitle, qbody, 10, importantWords, cutoff);
    return topQuestionWords;
  }


}