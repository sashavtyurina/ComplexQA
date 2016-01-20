// util
import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.*;
import java.util.Collections;
import java.util.Scanner;

// lang
import java.lang.Math;
import java.lang.StringBuilder;

// io
import java.io.FileNotFoundException;

// lucene
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;



public class Index {


	private String corpus = "";
	private Vector<String> tokens = new Vector<String> ();
	private HashMap<String, Vector<Integer>> index = new HashMap<String, Vector<Integer>>();
	private Integer INFINITY = Integer.MAX_VALUE;

	// tokenization options
	private boolean lower = true;
	private boolean nopunct = true;
	private boolean shrink_rep = true;
	private boolean drop_stop = true;
	private boolean stem = true;

	private Scanner input = new Scanner(System.in);

	private void buildIndex() {
		for (int i = 0; i < this.tokens.size(); ++i){
			String t = this.tokens.get(i);
			if (index.containsKey(t)) {
				Vector<Integer> posting = index.get(t);
				posting.add(i);
				index.put(t, posting);
			} else {
				Vector<Integer> posting = new Vector<Integer>();
				posting.add(i);
				index.put(t, posting);
			}
		}
	}

	


	private int prev(String token, int pos) {
		if (! this.index.containsKey(token)) {
			return INFINITY;
		}
		if (pos > this.last(token)) {
			return this.last(token);
		}
		if (pos <= this.first(token)) {
			return INFINITY;
		}

		int l = this.index.get(token).size() - 1;
		int ind = this.binary_search_prev(token, 0, l, pos);
		return this.index.get(token).get(ind);
	}

	private int next(String token, int pos) {
		if (! this.index.containsKey(token)) {
			return INFINITY;
		}
		if (this.last(token) <= pos) {
			return INFINITY;
		}
		if (this.first(token) > pos) {
			return this.first(token);
		}
		int l = this.index.get(token).size() - 1;
		int ind = this.binary_search_next(token, 0, l, pos);
		return this.index.get(token).get(ind);
	}

	private int binary_search_prev(String token, int low, int high, int current) {
		if (! this.index.containsKey(token)) {
			return INFINITY;
		}
		while ((high - low) > 1) {
			int mid = (int)(Math.floor((high + low)/2));
			if (this.index.get(token).get(mid) >= current) {
				high = mid;
			} else {
				low = mid;
			}
		}
		return low;
	}

	private int binary_search_next(String token, int low, int high, int current) {
		if (! this.index.containsKey(token)) {
			return INFINITY;
		}

		while ((high - low) > 1) {
			int mid = (int)(Math.floor((high + low)/2));
			if (this.index.get(token).get(mid) <= current) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return high;
	}

	private int last(String token) {
		if (this.index.containsKey(token)) {
			int post_length = this.index.get(token).size();
			return this.index.get(token).get(post_length - 1);
		}
		return INFINITY;
	}

	private int first (String token) {
		if (this.index.containsKey(token)) {
			return this.index.get(token).get(0);	
		}
		return INFINITY;
	}

	public Index() {}
	public Index(String corp) throws FileNotFoundException {
		this.corpus = corp;
		// this.tokens = Utils.tokenizeAndClean(this.corpus, lower, nopunct, shrink_rep, drop_stop, stem);
		this.tokens = Utils.lucene_tokenize(new EnglishAnalyzer(), this.corpus);
		this.buildIndex();
	}

	public void setTokenizationOptions(boolean lower, boolean nopunct, boolean shrink_rep, boolean drop_stop, boolean stem){
		this.lower = lower;
		this.nopunct = nopunct;
		this.shrink_rep = shrink_rep;
		this.drop_stop = drop_stop;
		this.stem = stem;
	}

	public void seeIndex() {
		for (Entry<String, Vector<Integer>> e : this.index.entrySet()) {
			System.out.println(e.getKey() + " --- " + e.getValue().toString());
		}
	}

	public LuceneHelper.Range nextCover(Vector<String> tokens, int pos, int m) {
		Vector<Integer> V = new Vector<Integer>();

        for (int i = 0; i < tokens.size(); ++i) {
            String token = tokens.get(i);
            V.add(this.next(token, pos));
        }

        Vector<Integer> V_sorted = new Vector<Integer>(V);
        Collections.sort(V_sorted, null);
        int v = V_sorted.get(m - 1);

        if (v == INFINITY) {
            // System.out.println("Bad range");
            return new LuceneHelper.Range(INFINITY, INFINITY);
        }

        int u = v;
        for (int i = 0; i < tokens.size(); ++i) {
            String token = tokens.get(i);
            int prev = this.prev(token, v + 1);

            // System.out.println(token + " -- " + prev);
            if ((V.get(i) < v) && ( prev < u )) {
                u = prev;
                // System.out.println("u = " + prev);
            }
        }

        // System.out.println(u + " -- " + v);
        return new LuceneHelper.Range (u, v);
	}

	public void getPassages(Vector<String> tokens, int maxLength) {
		int n = tokens.size();
		for (int m = n; m >=2; m--) {
			System.out.println("M covers. M = " + m);
			int u = -1;
			while (u < INFINITY) {
				LuceneHelper.Range r = this.nextCover(tokens, u, m);
				// System.out.println("Range = " + r.left + " -- " + r.right);
				// input.next();

				u = r.left;
				
				if (u == INFINITY) {
					break;
				}

				int length = r.right - r.left;
				if (length > maxLength) {
					continue;
				}

				System.out.println(this.getPassage(r));
				System.out.println("***");

			}

		}
	}

	public String getPassage(LuceneHelper.Range r) {
		StringBuilder sb = new StringBuilder();
		for (int i = r.left; i <= r.right; ++i) {
			sb.append(this.tokens.get(i) + " ");
		}
		return sb.toString();
	}


} 