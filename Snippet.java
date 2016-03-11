import java.util.*;
import java.sql.*;

public class Snippet {
	public String original;
	public String queryText;
	public String processed;
	private Vector<String> tokens;


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
	}

	public static Vector<Snippet> rsToSnippetList (ResultSet snippetsRS) {
	//// the ResultSet should contain snippetText, query, queryID, quesrtionID, snippetID
		Vector<Snippet> snippetsList = new Vector<Snippet>();
		try {
			while (snippetsRS.next()) {
				String snippet = snippetsRS.getString("snippet");
				String query = snippetsRS.getString("queryText");
				int snipID = snippetsRS.getInt("snippetID");
				int querID = snippetsRS.getInt("queryID");
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

}