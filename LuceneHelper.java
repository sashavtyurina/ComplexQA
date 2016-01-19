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




public class LuceneHelper {
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

    public Vector<Document> performSearch_Doc(String queryString, int n) throws IOException, ParseException {
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



