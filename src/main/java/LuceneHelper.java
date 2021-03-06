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
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.search.similarities.BasicStats;
import java.util.Arrays;
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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeSource;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import java.io.Reader;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
// import org.apache.lucene.analysis.tokenattributes;
import org.apache.lucene.search.PhraseQuery;

import com.google.common.base.Joiner;


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

    // public String dictIndexPath = "/home/avtyurin/ComplexQA/LuceneSpell/";
    public String FIELD_BODY = "contents"; // primary field name where all the text is stored
    public String FIELD_ID = "id";

    public int numDocs() {
        return this.reader.numDocs();
    }

    public Document docByID(int id) throws IOException {
        return this.reader.document(id);
    } 

    public Vector<String> searchYahooDocs(String docIDPhrase, int numDocs) throws IOException {
        PhraseQuery pq = new PhraseQuery(FIELD_ID, docIDPhrase); //"clueweb09-en0005-53-00116");
        ScoreDoc[] scoreDocs = this.searcher.search(pq, numDocs).scoreDocs;



        Vector<String> docs = new Vector<String>();
        for (int i = 0; i < scoreDocs.length; ++i) {
            int docID = scoreDocs[i].doc;
            Document d = reader.document(docID);
            docs.add(d.get(FIELD_BODY));

            System.out.println(d.toString());
        }
        return docs;
    }


	public LuceneHelper(String index_path) throws IOException, ParseException {
		

		Path indexPath = Paths.get(index_path);
		this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.searcher = new IndexSearcher(this.reader);
        this.searcher.setSimilarity(new BM25Similarity());
        this.parser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
        this.parser.setDefaultOperator(QueryParser.Operator.OR);

        // Path dictPath = Paths.get(dictIndexPath);
        // this.dictReader = DirectoryReader.open(FSDirectory.open(dictPath));
        
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
            System.out.println(docID);
            Document d = reader.document(docID);
            docs.add(d.get(FIELD_BODY));
            // System.out.println(d.get("contents"));
        }
        return docs;
    }



    // public Vector<Integer> createPostingList(String term, int docID) throws IOException {
    //     // int docID = 7768699;
    //     Document d = reader.document(docID);
    //     TermsEnum termsIt = this.reader.getTermVector(docID, FIELD_BODY).iterator();

    //     BytesRef w = new BytesRef(term.getBytes());    
    //     termsIt.seekExact(w);

    //     PostingsEnum postingsInDoc = termsIt.postings(null, PostingsEnum.POSITIONS);
    //     postingsInDoc.nextDoc();

    //     int totalFreq = postingsInDoc.freq();
    //     Vector<Integer> positions = new Vector<Integer>();

    //     for (int j = 0; j < totalFreq; j++) {
    //         int pos = postingsInDoc.nextPosition();
    //         positions.add(new Integer(pos));
    //     }
    //     return positions;
    // }

    public Vector<String> performPassageSearch(String queryString, int max_docs, int max_passage_length) throws IOException, ParseException {
        QueryParser queryParser = new QueryParser("", new EnglishAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = queryParser.parse(queryString);

        // System.out.println(query.toString(FIELD_BODY));

        Scanner input = new Scanner(System.in);

        ScoreDoc[] scoreDocs = this.searcher.search(query, max_docs).scoreDocs;
        
        Vector<String> query_words = new Vector<String>(Arrays.asList(queryString.split("\\s")));

        Vector<String> passages = new Vector<String>();

        Vector<String> documents = new Vector<String>();
        int rep_docs = 0;
        int rep_passages = 0;

        for (int i = 0; i < scoreDocs.length; ++i) {
            int docID = scoreDocs[i].doc;    
            Document d = reader.document(docID);
            String doc_body = d.get(FIELD_BODY);

            passages.add(d.get(FIELD_ID) + " :::: " + doc_body);

            // System.out.println(d.get(FIELD_ID));
            // System.out.println(d.get(FIELD_BODY));

/*            if (documents.contains(doc_body)) {
                rep_docs += 1;
                continue;
            }
            documents.add(doc_body);
            // for every document build its own index 
            Index ind = new Index(doc_body);
            Vector<String> parsed_words = new Vector<String>(Arrays.asList(queryString.split("\\s"))); ///Utils.lucene_tokenize(queryString);
            Vector<String> doc_passages = ind.getPassages(parsed_words, max_passage_length, 250);

            for (String s : doc_passages) {
                if (passages.contains(s)) {
                    rep_passages += 1;passageSearchFromDocument
                    continue;
                } else {
                    passages.add(s);
                }
            }*/
        }
        return passages;
    }

    public Vector<String> passageSearchFromDocument(String queryString, String textDocument, int maxPassageLength) throws IOException, ParseException {
        Vector<String> query = new Vector<String>(lucene_tokenize(new EnglishAnalyzer(), queryString));
        Vector<String> doc = new Vector<String>(lucene_tokenize(new EnglishAnalyzer(), textDocument));

        Joiner joiner = Joiner.on(" ");
        String docBody = joiner.join(doc);


        Index ind = new Index(docBody);
        Vector<String> passages = ind.getPassages(query, 10, maxPassageLength);

        // System.out.println(query.toString());

        // for (String p : passages) {
        //     System.out.println(p + "\n\n*********\n\n");
        // }
        return passages;
    }

    public List<String> lucene_tokenize(Analyzer analyzer, String text) {
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

    // private int next(Vector<Integer> posting, int position) {
    //     if (posting == null) {
    //         return Integer.MAX_VALUE;
    //     }

    //     if (posting.get(0) > position) {
    //         return posting.get(0);
    //     }

    //     for (int i = 0; i < posting.size(); ++i) {
    //         if (posting.get(i) > position) {
    //             return posting.get(i);
    //         }
    //     }
    //     return Integer.MAX_VALUE;
    // }

    // private int prev(Vector<Integer> posting, int position) {
    //     if (posting == null) {
    //         return Integer.MAX_VALUE;
    //     }

    //     if (posting.get(0) > position) {
    //         return Integer.MAX_VALUE;
    //     }

    //     for (int i = 1; i < posting.size(); ++i) {
    //         if (position < posting.get(i)) {
    //             return posting.get(i - 1);
    //         }
    //     }
    //     return posting.get(posting.size() - 1);
    // }


    // private String nextMCover(Vector<String> query, int position, int m) {
    //     return "";
    // }


    //     public Vector<Document> performSearch1(String queryString, int n) throws IOException, ParseException {
    //     QueryParser queryParser = new QueryParser(FIELD_BODY, new EnglishAnalyzer());
    //     queryParser.setDefaultOperator(QueryParser.Operator.OR);
    //     Query query = queryParser.parse(queryString);
    //     // System.out.println("ANALYSED QUERY:");
    //     // System.out.println(query);

    //     // Query query = this.parser.parse(queryString);
    //     ScoreDoc[] scoreDocs = this.searcher.search(query, n).scoreDocs;
    //     Vector<Document> docs = new Vector<Document>();
    //     for (int i = 0; i < scoreDocs.length; ++i) {
    //         int docID = scoreDocs[i].doc;
    //         Document d = reader.document(docID);
    //         docs.add(d);
    //         // System.out.println(d.get("contents"));
    //     }
    //     return docs;
    // }

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

    // public Vector<String> spellChecker(String word) {
    //     // /// given a token, suggests most popular spellings
    //     // DirectSpellChecker checker = new DirectSpellChecker();
    //     // Vector<String> suggestionsVect = new Vector<String>();
    //     // Term t = new Term(FIELD_BODY, word);
    //     // try {
    //     //     SuggestWord[] suggestions = checker.suggestSimilar(t, 10, this.dictReader, SuggestMode.valueOf("SUGGEST_MORE_POPULAR"));
    //     //     // suggestionsVect.addAll(Arrays.asList(suggestions));
    //     // } catch (IOException e) {
    //     //         System.err.println(e.getClass().getName() + ": " + e.getMessage());
    //     // }
    //     // return suggestionsVect;
    //     return null;
    // }



    public void testStuff() throws IOException, ParseException {
        Scanner input = new Scanner(System.in);

        // Analyzer theAnalyzer = new Analyzer() {
        //     Override 
        //     protected TokenStreamComponents createComponents(String fieldName) {

        //     }
        // };
        // System.out.println(this.reader.document(1));


        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).hasPositions());
        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).hasOffsets());
        // System.out.println(this.reader.getTermVector(1, FIELD_BODY).size());

        // TermsEnum it = this.reader.getTermVector(1, FIELD_BODY).iterator();
        // it.seekExact(new BytesRef("name".getBytes()));
        // PostingsEnum postings = it.postings(null, PostingsEnum.POSITIONS);
        // System.out.println(postings);
        // input.next();

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
        // DirectSpellChecker checker = new DirectSpellChecker();

        // Vector<String> words = new Vector<String>();
        // words.add("misspelt");
        // words.add("peple");
        // words.add("people");
        // words.add("symphoms");
        // words.add("wabbit");
        // words.add("oftened");
        // words.add("good");
        // words.add("these");
        // words.add("are");
        // words.add("correct");
        // words.add("words");

        // for (String w : words) {
        //     Term t = new Term(FIELD_BODY, w);
        //     SuggestWord[] suggestions = checker.suggestSimilar(t, 10, this.dictReader, SuggestMode.valueOf("SUGGEST_MORE_POPULAR")); //, "contents", , 0.0f);
        //     System.out.println("WORD: " + w);
        //     if ((suggestions != null) && (suggestions.length != 0)) {
        //         for (int i = 0; i < suggestions.length; ++i) {
        //             System.out.println(suggestions[i].string + " " + suggestions[i].score);
        //         }
        //         // System.out.println(Arrays.toString(suggestions));
        //     } else {
        //         System.out.println("No suggestions");
        //     }
        // }
        ///Spell checker ///     

        }




    }




