import java.util.*;
import java.util.Map.*;

public class QueryScoring {
	public String queryText;
	public double atokensSim;
	public double qtokensSim;
	public double alltokensSim;
	public double aSimDom; // similarity with answer dominates. 0.7 answer_sim + 0.3 question_sim
	public double qSimDom; // same as above. the coefficients are swapped 
	public double equalWeightSim; // 0.5 answer_sim + 0.5 question_sim

	public QueryScoring(String _query, double _atokensSim, double _qtokensSim, double _alltokensSim) {
		this.queryText = _query;
		this.atokensSim = _atokensSim;
		this.qtokensSim = _qtokensSim;
		this.alltokensSim = _alltokensSim;

		this.aSimDom = 0.7 * _atokensSim + 0.3 * _qtokensSim;
		this.qSimDom = 0.3 * _atokensSim + 0.7 * _qtokensSim;
		this.equalWeightSim = 0.5 * _atokensSim + 0.5 * qtokensSim;
	}

	public static Vector<String> getTopWordsFromScoredQueries(Vector<QueryScoring> scoredQueries, int n, String fieldName) {
		List<QueryScoring> topNQueries = Utils.sliceCollection(Utils.sortByFiledName(fieldName, scoredQueries), 0, n);
		System.out.println(Utils.sortByFiledName(fieldName, scoredQueries));

		Vector<String> wordsRanking = new Vector<String> ();

		for (QueryScoring q : topNQueries) {
			System.out.println(q.queryText);
			wordsRanking.addAll(Utils.str2vect(q.queryText));
		}

		System.out.println(Utils.buildDistribution(wordsRanking));
		List<Entry<String, Double>> sortedWords = Utils.entriesSortedByValues(Utils.buildDistribution(wordsRanking), "dec");
		System.out.println("\n" + sortedWords + "\n");
		wordsRanking.clear();

		for (Entry<String, Double> e : sortedWords) {
			wordsRanking.add(e.getKey());
		}

		return wordsRanking;
	}

	public String toString() {
		return "queryText: " + queryText + "; atokensSim: " + atokensSim 
			 + "; qtokensSim: " + qtokensSim + "; alltokensSim: " + alltokensSim
			 + "; aSimDom: " + aSimDom + "; qSimDom: " + qSimDom + "; equalWeightSim: " + equalWeightSim;
	}
}