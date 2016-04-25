import java.util.*;
import java.sql.*;

public class Snippet {
	public String original;
	public String queryText;
	public String processed;
	private Vector<String> tokens;
	private Vector<String> bigrams;


	public int snippetID;
	public int queryID;
	public int questionID;
	public boolean producedByTrueQuery = false;


	private double simScore; // this will change depending on what measure is used 

	public Snippet(String _original, String _queryText, int _snippetID, int _queryID, int _questionID, boolean _producedByTrueQuery) {
		original = _original;
		snippetID = _snippetID;
		queryID = _queryID;
		questionID = _questionID;
		queryText = _queryText;

		processed = Utils.shrinkRepeatedChars(Utils.removePunct(original.toLowerCase()));
		tokens = Utils.removeShortTokens(new Vector<String>(Arrays.asList(processed.split("\\s"))), 2);
		simScore = -1.0;
		producedByTrueQuery = _producedByTrueQuery;
		this.bigrams = new Vector<String>();
	}

	public static Vector<Snippet> rsToSnippetList (ResultSet snippetsRS) {
	//// the ResultSet should contain snippetText, query, queryID, quesrtionID, snippetID
		Vector<Snippet> snippetsList = new Vector<Snippet>();
		try {
			while (snippetsRS.next()) {
				String docURL = snippetsRS.getString("docURL");
				if (docURL.contains("answers.yahoo.")) {
					continue;
				}
				String snippet = snippetsRS.getString("snippet");
				String query = snippetsRS.getString("queryText");
				int snipID = snippetsRS.getInt("snippetID");
				// int querID = snippetsRS.getInt("queryID");
				int querID = -1;
				int questID = snippetsRS.getInt("questID");
				snippetsList.add(new Snippet(snippet, query, snipID, querID, questID, false));

			}
		} catch (SQLException e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      		System.exit(0);
		}

		return snippetsList;
	}

	public static Vector<Snippet> rsToSnippetListGoogleSearchDocs (ResultSet snippetsRS) {
	//// the ResultSet should contain snippetText, query, queryID, quesrtionID, snippetID
		Vector<Snippet> snippetsList = new Vector<Snippet>();
		try {
			while (snippetsRS.next()) {
				String docURL = snippetsRS.getString("url");
				if (docURL.contains("answers.yahoo.")) {
					continue;
				}
				String snippet = snippetsRS.getString("snippet");
				String query = snippetsRS.getString("queryText");
				int snipID = snippetsRS.getInt("docID");
				// int querID = snippetsRS.getInt("queryID");
				int querID = -1;
				int questID = snippetsRS.getInt("questID");
				snippetsList.add(new Snippet(snippet, query, snipID, querID, questID, false));

			}
		} catch (SQLException e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
      		System.exit(0);
		}
		return snippetsList;
	}

	public String toString() {
		return simScore + "::" + original;
	}

	public Vector<String> tokens() {
		return this.tokens;
	}

	public void setSimScore(double newScore) {
		this.simScore = newScore;
	}

	public double simScore() {
		return simScore;
	}

	public Vector<String> getBigrams() {
		return this.bigrams;
	}

	public void generateBigrams() {
		
		Vector<String> stoppedTokens = Utils.dropStopWords(this.tokens);
		int length = stoppedTokens.size();

		for (int i = 0; i < length - 1; ++i) {
			String bigram = stoppedTokens.get(i) + " " + stoppedTokens.get(i + 1);
			this.bigrams.add(bigram);
		}
	}


	public static Vector<Snippet> sortSnippetsByScore(Vector<Snippet> snippets, String direction) {

    /// direction can be either "inc" or "dec"

	    if (direction == "inc") {
		    Collections.sort(snippets, 
		            new Comparator<Snippet>() {
		                @Override
		                public int compare(Snippet s1, Snippet s2) {
		                	Double score1 = new Double(s1.simScore());
		                	Double score2 = new Double(s2.simScore());
		                    return score1.compareTo(score2);
		                }
		            }
		    );
	  	} else {
	    	Collections.sort(snippets, 
	            new Comparator<Snippet>() {
	                @Override
	                public int compare(Snippet s1, Snippet s2) {
	                    Double score1 = new Double(s1.simScore());
	                	Double score2 = new Double(s2.simScore());
	                    return score2.compareTo(score1);
	                }
	            }
	    	);
		}
		return snippets;
	}

	public static HashMap<String, Vector<Snippet>> splitSnippetsToProbes(Vector<Snippet> snippets) {

		HashMap<String, Vector<Snippet>> result = new HashMap<String, Vector<Snippet>>();

		for (Snippet s : snippets) {
			String probe = s.queryText;
			if (!result.containsKey(probe)) {
				Vector<Snippet> ss = new Vector<Snippet>();
				ss.add(s);
				result.put(probe, ss);
			} else {
				Vector<Snippet> curSnippets = result.get(probe);
				curSnippets.add(s);
				result.put(probe, curSnippets);
			}
		}

		return result;
	}




}