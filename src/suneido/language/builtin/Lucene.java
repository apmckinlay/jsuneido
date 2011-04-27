package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;

import suneido.SuException;
import suneido.language.BuiltinClass;

public class Lucene extends BuiltinClass {
	public static final Lucene singleton = new Lucene();

	private Lucene() {
		super(Lucene.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Lucene");
	}

	
	public static void main(String[] args) 
			throws IOException, ParseException {
		createIndex();
	    searchIndex();
	} 

	private static void createIndex() throws IOException,
			CorruptIndexException, LockObtainFailedException {
		IndexWriter writer = writer("index");
		
		Document doc = new Document();
		doc.add(new Field("name", "mydoc",
				Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		doc.add(new Field("text", "now is the time for all good men",
				Field.Store.NO, Field.Index.ANALYZED));
		writer.addDocument(doc);
		
		doc = new Document();
		doc.add(new Field("name", "another",
				Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		doc.add(new Field("text", "it was a good time for sleep",
				Field.Store.NO, Field.Index.ANALYZED));
		writer.addDocument(doc);
		
		doc = new Document();
		doc.add(new Field("name", "other",
				Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		doc.add(new Field("text", "no rest for the wicked",
				Field.Store.NO, Field.Index.ANALYZED));
		writer.addDocument(doc);
		
		writer.close();
	}

	private static IndexWriter writer(String path) throws IOException,
			CorruptIndexException, LockObtainFailedException {
		Directory dir = FSDirectory.open(new File(path));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		return new IndexWriter(dir, iwc);
	}

	private static void searchIndex() 
			throws CorruptIndexException, IOException, ParseException {
		Directory dir = FSDirectory.open(new File("index"));
		IndexSearcher searcher = new IndexSearcher(dir);
	    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);
	    QueryParser parser = new QueryParser(Version.LUCENE_31, "text", analyzer);
	    Query query = parser.parse("good time");
	    System.out.println("Searching for: " + query.toString("text"));
	    TopDocs results = searcher.search(query, 10);
	    int numTotalHits = results.totalHits;
	    System.out.println(numTotalHits + " total matching documents");
	    ScoreDoc[] hits = results.scoreDocs;
	    for (ScoreDoc hit : hits) {
	        Document doc = searcher.doc(hit.doc);
	        String name = doc.get("name");
	        System.out.println("found name: " + name);
	    }
	    searcher.close();
	}

}
