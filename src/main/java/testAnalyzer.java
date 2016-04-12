import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.Tokenizer;

public class testAnalyzer extends Analyzer {
	@Override
   protected TokenStreamComponents createComponents(String fieldName) {
     Tokenizer source = new StandardTokenizer();
     // source.setReader(super.reader);

     ShingleFilter filter = new ShingleFilter(source, 3);
     filter.setOutputUnigrams(true);

     return new TokenStreamComponents(source, filter);
   }
}