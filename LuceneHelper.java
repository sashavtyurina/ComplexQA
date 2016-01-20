import java.io.PrintWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import java.nio.file.Files;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.lucene.codecs.blocktree.FieldReader;
import org.apache.lucene.index.Term;
// import org.apache.lucene.index.TermFreqVector;
// import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.PlainTextDictionary;
// import org.apache.lucene.index.Terms;
import org.apache.lucene.index.MultiTerms;
// import org.apache.lucene.search.similarities.Distribution;
// import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.BasicStats;
import java.util.Arrays;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.DirectSpellChecker;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.analysis.util.CharArraySet;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.MultiFields;
import java.util.Scanner;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiTermsEnum;
import org.apache.lucene.index.Terms;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.lucene.analysis.TokenStream;
import java.io.StringReader;
// import org.apache.lucene.analysis.tokenattributes;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeSource;



public class LuceneHelper {
    public static class Range {
        public int left;
        public int right;
        public Range() {}
        public Range(int l, int r) {
            this.left = l;
            this.right = r;
        }
    }
	private IndexSearcher searcher;
    private QueryParser parser;
    public IndexReader reader;
    public IndexReader dictReader;

    public String dictIndexPath = "/home/avtyurin/ComplexQA/LuceneSpell/";
    public String FIELD_BODY = "contents"; // primary field name where all the text is stored
    public String FIELD_ID = "id";


	public LuceneHelper(String index_path) throws IOException, ParseException {
		

		Path indexPath = Paths.get(index_path);
		this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.searcher = new IndexSearcher(this.reader);
        this.searcher.setSimilarity(new BM25Similarity());
        this.parser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        this.parser.setDefaultOperator(QueryParser.Operator.OR);

        Path dictPath = Paths.get(dictIndexPath);
        this.dictReader = DirectoryReader.open(FSDirectory.open(dictPath));
        
	}

	public Vector<String> performSearch(String queryString, int n) throws IOException, ParseException {
        QueryParser queryParser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = queryParser.parse(queryString);
        // System.out.println("ANALYSED QUERY:");
        // System.out.println(query);

        // Query query = this.parser.parse(queryString);
        ScoreDoc[] scoreDocs = this.searcher.search(query, n).scoreDocs;
        Vector<String> docs = new Vector<String>();
        for (int i = 0; i < scoreDocs.length; ++i) {
        	int docID = scoreDocs[i].doc;
            Document d = reader.document(docID);
            docs.add(d.get(FIELD_BODY));
            // System.out.println(d.get("contents"));
        }
        return docs;
    }


    public Vector<Integer> createPostingList(String term, int docID) throws IOException {
        // int docID = 7768699;
        Document d = reader.document(docID);
        TermsEnum termsIt = this.reader.getTermVector(docID, FIELD_BODY).iterator();

        BytesRef w = new BytesRef(term.getBytes());    
        termsIt.seekExact(w);

        PostingsEnum postingsInDoc = termsIt.postings(null, PostingsEnum.POSITIONS);
        postingsInDoc.nextDoc();

        int totalFreq = postingsInDoc.freq();
        Vector<Integer> positions = new Vector<Integer>();

        for (int j = 0; j < totalFreq; j++) {
            int pos = postingsInDoc.nextPosition();
            positions.add(new Integer(pos));
        }
        return positions;
    }

    public Range bestPassage(String queryString) throws IOException, ParseException {
        QueryParser queryParser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = queryParser.parse(queryString);

        // System.out.println(query.toString(FIELD_BODY));

        Scanner input = new Scanner(System.in);

        int max_docs = 1;
        
        ScoreDoc[] scoreDocs = this.searcher.search(query, max_docs).scoreDocs;
        
        Vector<String> query_words = new Vector<String>(Arrays.asList(queryString.split("\\s")));

        int docID = scoreDocs[0].doc;
        Document d = reader.document(docID);
        // System.out.println(d.get("tokenized"));
        // input.next();

        String doc_body = d.get(FIELD_BODY);
        System.out.println(doc_body);
        Index ind = new Index(doc_body);
        // ind.seeIndex();
        // input.next();
        // HashMap<String, Vector<Integer>> postings = new HashMap<String, Vector<Integer>>();
        Vector<String> parsed_words = Utils.lucene_tokenize(new EnglishAnalyzer(), queryString);
        System.out.println(parsed_words);

        // for (String s : query_words) {
        //     String parsed = queryParser.parse(s).toString(FIELD_BODY);
        //     System.out.println(parsed + " -- " + createPostingList(parsed, docID));
        //     postings.put(parsed, createPostingList(parsed, docID));
        //     parsed_words.add(parsed);
        // }


        // int position = 0;
        // int m = 3;
        // Range r = ind.nextCover(parsed_words, 0, 3);
        // System.out.println(ind.getPassage(r));
        ind.getPassages(parsed_words, 100);


        // next cover
        // Vector<Integer> V = new Vector<Integer>();
        // for (int i = 0; i < parsed_words.size(); ++i) {
        //     String t = parsed_words.get(i);
        //     V.add(next(postings.get(t), position));
        // }
        // Vector<Integer> V_sorted = new Vector<Integer>(V);
        // Collections.sort(V_sorted, null);
        // int v = V_sorted.get(m - 1);
        // System.out.println("V = " + V);
        // System.out.println("v = " + v);


        // if (v == Integer.MAX_VALUE) {
        //     System.out.println("Bad range");
        //     return null;
        // }

        // int u = v;
        // for (int i = 0; i < parsed_words.size(); ++i) {
        //     String t = parsed_words.get(i);
        //     int prev = prev(postings.get(t), v + 1);

        //     System.out.println(t + " -- " + prev);
        //     if ((V.get(i) < v) && ( prev < u )) {
        //         u = prev;
        //         System.out.println("u = " + prev);
        //     }
        // }

        // System.out.println(u + " -- " + v);
        // List<String> tokens = lucene_tokenize(new EnglishAnalyzer(), doc_body);
        // System.out.println(tokenizeds);

        // Vector<String> tokens = Utils.tokenizeAndClean(doc_body, false, false, false, false, false);
        // System.out.println(tokens);
        // for (int i = u-1; i <=v+1; ++i) {
        //     System.out.print(tokens.get(i) + " ");
        // }

        return null ; // new Range(u, v);
        // Vector<Document> docs = new Vector<Document>();
        // // for (int i = 0; i < scoreDocs.length; ++i) {
        //     // int docID = scoreDocs[i].doc;
        //     int docID = 7768699;
        //     Document d = reader.document(docID);


        //     String doc_body = d.get(FIELD_BODY);
        //     String doc_id = d.get(FIELD_ID);
        //     System.out.println(docID);
        //     System.out.println(doc_body);


        //     Vector<String> query_words = new Vector<String>(Arrays.asList(queryString.split("\\s")));
            
        //     int position = 0;
        //     int v = -1;
        //     int m = 3;

        //     Vector<Integer> V = new Vector<Integer>();
        //     TermsEnum termsIt = this.reader.getTermVector(docID, FIELD_BODY).iterator();
        //     for (String s : query_words) {
        //         BytesRef w = new BytesRef(s.getBytes());    
        //         termsIt.seekExact(w);
        //         PostingsEnum postingsInDoc = termsIt.postings(null, PostingsEnum.POSITIONS);
        //         postingsInDoc.nextDoc();
        //         V.add(new Integer(this.next(s, position, postingsInDoc)));
        //     }

        //     Collections.sort(V, null);
        //     System.out.println(V);

        //     v = V.get(m - 1);
        //     System.out.println(v);
        //     if (v == -1) {
        //         System.out.println("Infinity.");
        //         return new Range(Integer.MAX_VALUE, Integer.MAX_VALUE);
        //     }

        //     int u = v;

        //     TermsEnum termsIt1 = this.reader.getTermVector(docID, FIELD_BODY).iterator();
        //     for (int j = 0; j < query_words.size(); ++j) {
        //         String s = query_words.get(j);
        //         System.out.println(s + " -- " + V.get(j));

        //         BytesRef w = new BytesRef(s.getBytes());    
        //         termsIt1.seekExact(w);
        //         PostingsEnum postingsInDoc = termsIt1.postings(null, PostingsEnum.POSITIONS);
        //         postingsInDoc.nextDoc();

        //         int prev = this.prev(s, v + 1, postingsInDoc);

                
        //         System.out.println(prev);

        //         if ((V.get(j) < v) && (prev < u)) {
        //             u = prev;
        //         }
        //     }
        //     System.out.println(u + " -- " + v);
        //     return new Range(u, v);
        // // }
        // // return null;
    }

    private List<String> lucene_tokenize(Analyzer analyzer, String text) {
        List<String> result = new ArrayList<String>();
        try {
          TokenStream stream  = analyzer.tokenStream(null, new StringReader(text));
          stream.reset();
          while (stream.incrementToken()) {
            result.add(stream.getAttribute(CharTermAttribute.class).toString());
          }
        } catch (IOException e) {
          // not thrown b/c we're using a string reader...
          throw new RuntimeException(e);
        }
        return result;

    }

    private int next(Vector<Integer> posting, int position) {
        if (posting == null) {
            return Integer.MAX_VALUE;
        }

        if (posting.get(0) > position) {
            return posting.get(0);
        }

        for (int i = 0; i < posting.size(); ++i) {
            if (posting.get(i) > position) {
                return posting.get(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    private int prev(Vector<Integer> posting, int position) {
        if (posting == null) {
            return Integer.MAX_VALUE;
        }

        if (posting.get(0) > position) {
            return Integer.MAX_VALUE;
        }

        for (int i = 1; i < posting.size(); ++i) {
            if (position < posting.get(i)) {
                return posting.get(i - 1);
            }
        }
        return posting.get(posting.size() - 1);
    }

    // private int next1(String term, int position, PostingsEnum posting) throws IOException {
    //     int totalFreq = posting.freq();
    //     for (int j = 0; j < totalFreq; j++) {
    //         int pos = posting.nextPosition();
    //         if (pos > position) {
    //             return pos;
    //         }
    //     }
    //     return Integer.MAX_VALUE;
    // }

    // private int prev1(String term, int position, PostingsEnum posting) throws IOException {
    //     int totalFreq = posting.freq();
    //     Vector<Integer> positions = new Vector<Integer>();

    //     int prev_pos = -1;

    //     for (int j = 0; j < totalFreq; j++) {
    //         int pos = posting.nextPosition();
    //         positions.add(pos);

    //         if ((j == 0) && (pos > position)) {
    //             return Integer.MAX_VALUE;
    //         }

    //         // prev_pos = pos;
    //         if ( pos > position ) {
    //             return positions.get(j-1);
    //         }
    //     }
    //     return Integer.MAX_VALUE;
    // }

    private String nextMCover(Vector<String> query, int position, int m) {
        return "";
    }


        public Vector<Document> performSearch1(String queryString, int n) throws IOException, ParseException {
        QueryParser queryParser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = queryParser.parse(queryString);
        // System.out.println("ANALYSED QUERY:");
        // System.out.println(query);

        // Query query = this.parser.parse(queryString);
        ScoreDoc[] scoreDocs = this.searcher.search(query, n).scoreDocs;
        Vector<Document> docs = new Vector<Document>();
        for (int i = 0; i < scoreDocs.length; ++i) {
            int docID = scoreDocs[i].doc;
            Document d = reader.document(docID);
            docs.add(d);
            // System.out.println(d.get("contents"));
        }
        return docs;
    }

    public long totalTermFreq(String term) throws IOException, ParseException {
        QueryParser queryParser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        Query query = queryParser.parse(term);
        
        String analysed_query = query.toString(FIELD_BODY);
        // System.out.println(analysed_query);


        Term t = new Term(FIELD_BODY, analysed_query);   
        return this.reader.totalTermFreq(t);
    }

    public long totalTerms() throws IOException {
        return this.reader.getSumDocFreq(FIELD_BODY);
    }



    public void testStuff() throws IOException, ParseException {
        Scanner input = new Scanner(System.in);
        System.out.println(this.reader.document(1));


        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).hasPositions());
        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).hasOffsets());
        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).size());

        TermsEnum it = this.reader.getTermVector(1, FIELD_BODY).iterator();
        it.seekExact(new BytesRef("name".getBytes()));
        PostingsEnum postings = it.postings(null, PostingsEnum.POSITIONS);
        System.out.println(postings);
        input.next();

        // Term t = new Term(FIELD_BODY, new String("test"));
        // PostingsEnum docEnum = MultiFields.getTermPositionsEnum(this.reader, FIELD_BODY, t.bytes());

        // // System.out.println(docEnum.freq());
        // int termFreq = 0;

        // int doc = PostingsEnum.NO_MORE_DOCS;
        // while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
        //     termFreq += docEnum.freq();
        //     System.out.println(docEnum.attributes().toString());
        //     System.out.println(docEnum. nextPosition());
            
        // }
        // TermFreqVector tfv = this.reader.getTermFreqVector(1, FIELD_BODY);

        
        // // Path dirPath = Paths.get("/home/avtyurin/ComplexQA/spellIndexDirectory");
        // // FSDirectory spellIndexDirectory = FSDirectory.open(dirPath);

        // // Path dictPath = Paths.get("/home/avtyurin/ComplexQA/spellIndexDirectory/fulldictionary00.txt");
        // // SpellChecker spellchecker = new SpellChecker(spellIndexDirectory);

        // // IndexWriterConfig conf = new IndexWriterConfig(new EnglishAnalyzer());
        // // spellchecker.indexDictionary(new PlainTextDictionary(dictPath), conf, false);


///Spell checker ///
        DirectSpellChecker checker = new DirectSpellChecker();

        Vector<String> words = new Vector<String>();
        words.add("misspelt");
        words.add("peple");
        words.add("syphoms");
        words.add("wabbit");
        words.add("oftened");
        words.add("good");
        words.add("these");
        words.add("are");
        words.add("correct");
        words.add("words");

        for (String w : words) {
            Term t = new Term(FIELD_BODY, w);
            SuggestWord[] suggestions = checker.suggestSimilar(t, 10, this.dictReader, SuggestMode.valueOf("SUGGEST_MORE_POPULAR")); //, "contents", , 0.0f);
            System.out.println("WORD: " + w);
            if ((suggestions != null) && (suggestions.length != 0)) {
                for (int i = 0; i < suggestions.length; ++i) {
                    System.out.println(suggestions[i].string + " " + suggestions[i].score);
                }
                // System.out.println(Arrays.toString(suggestions));
            } else {
                System.out.println("No suggestions");
            }
        }
///Spell checker ///     

    }

    public void indexDictionary() throws IOException {
        Path dictPath = Paths.get("/home/avtyurin/ComplexQA/LuceneSpell");
        Directory dir = FSDirectory.open(dictPath);
        CharArraySet stop_words = new CharArraySet(new Vector<String>(), true);
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(new StandardAnalyzer(stop_words)));

        FileInputStream fstream = new FileInputStream("/home/avtyurin/ComplexQA/LuceneSpell/fulldictionary00.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;
        int id = 0;
        while ((strLine = br.readLine()) != null) {
            String w = strLine.trim().toLowerCase();
            id ++;
            // make a new, empty document
            Document document = new Document();

            // document ID
            document.add(new StringField(FIELD_ID, String.valueOf(id), Field.Store.YES));
            document.add(new TextField(FIELD_BODY, w, Field.Store.YES));

            
            writer.addDocument(document);
        }
        writer.commit();
        writer.close();
    }
}



