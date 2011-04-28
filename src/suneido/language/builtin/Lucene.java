package suneido.language.builtin;

import static suneido.util.Util.array;

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
import suneido.SuValue;
import suneido.language.*;

/*
Lucene.Update("lucene", create:) {|u| u.Add('now', 'now is the time for all good men') }
Lucene.Search("lucene", "good") {|id| Print(id: id) }
 */

public class Lucene extends BuiltinClass {
	public static final Version version = Version.LUCENE_31;
	public static final Lucene singleton = new Lucene();

	private Lucene() {
		super(Lucene.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Lucene");
	}

	public static class Update extends SuMethod3 {
		{ params = new FunctionSpec(array("dir", "block", "create"), false); }
		@Override
		public Object eval3(Object self, Object a, Object b, Object c) {
			String dir = Ops.toStr(a);
			boolean create = Ops.toBoolean_(c);
			Updater updater = new Updater(dir, create);
			try {
				Ops.call1(b, updater);
			} finally {
				updater.close();
			}
			return null;
		}
	}

	public static class Updater extends SuValue {
		private static BuiltinMethods methods = new BuiltinMethods(Updater.class);
		private final IndexWriter writer;

		private Updater(String dir, boolean create) {
			writer = writer(dir, create);
		}

		@Override
		public SuValue lookup(String method) {
			return methods.lookup(method);
		}

		public static class Add extends SuMethod2 {
			{ params = new FunctionSpec("id", "text"); }
			@Override
			public Object eval2(Object self, Object id, Object text) {
				IndexWriter writer = ((Updater) self).writer;
				Document doc = new Document();
				doc.add(new Field("id", Ops.toStr(id),
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("text", Ops.toStr(text),
						Field.Store.NO, Field.Index.ANALYZED));
				try {
					writer.addDocument(doc);
				} catch (IOException e) {
					throw new SuException("Lucene.Update: Add failed", e);
				}
				return null;
			}
		}

		public static class Update extends SuMethod2 {
			{ params = new FunctionSpec("id", "text"); }
			@Override
			public Object eval2(Object self, Object a, Object text) {
				IndexWriter writer = ((Updater) self).writer;
				String id = Ops.toStr(a);
				Term term = new Term("id", id);
				Document doc = new Document();
				doc.add(new Field("id", id,
						Field.Store.YES, Field.Index.NOT_ANALYZED));
				doc.add(new Field("text", Ops.toStr(text),
						Field.Store.NO, Field.Index.ANALYZED));
				try {
					writer.updateDocument(term, doc);
				} catch (IOException e) {
					throw new SuException("Lucene.Update: Add failed", e);
				}
				return null;
			}
		}

		public static class Delete extends SuMethod1 {
			{ params = new FunctionSpec("id"); }
			@Override
			public Object eval1(Object self, Object id) {
				IndexWriter writer = ((Updater) self).writer;
				Term term = new Term("id", Ops.toStr(id));
				try {
					writer.deleteDocuments(term);
				} catch (IOException e) {
					throw new SuException("Lucene.Update: Delete failed", e);
				}
				return null;
			}
		}

		void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new SuException("Lucene.Update: close failed", e);
			}
		}
	}

	private static IndexWriter writer(String path, boolean create) {
		Directory dir;
		try {
			dir = FSDirectory.open(new File(path));
			Analyzer analyzer = new StandardAnalyzer(version);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);
			iwc.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND);
			return new IndexWriter(dir, iwc);
		} catch (IOException e) {
			throw new SuException("Lucene.Update: can't open index", e);
		}
	}

	public static class Search extends SuMethod4 {
		{ params = new FunctionSpec(array("dir", "query", "block", "limit"), 10); }
		@Override
		public Object eval4(Object self, Object a, Object b, Object c, Object d) {
			String path = Ops.toStr(a);
			String queryStr = Ops.toStr(b);
			int limit = Ops.toInt(d);
			Directory dir;
			try {
				dir = FSDirectory.open(new File(path));
				IndexSearcher searcher = new IndexSearcher(dir);
			    Analyzer analyzer = new StandardAnalyzer(version);
			    QueryParser parser = new QueryParser(version, "text", analyzer);
// TODO don't use parser, just TermQuery and BooleanQuery/Clause
			    Query query = parser.parse(queryStr);
			    TopDocs results = searcher.search(query, limit);
			    ScoreDoc[] hits = results.scoreDocs;
			    for (ScoreDoc hit : hits) {
			        Document doc = searcher.doc(hit.doc);
			        String id = doc.get("id");
			        Ops.call1(c, id);
			    }
			    searcher.close();
				return null;
			} catch (Exception e) {
				throw new SuException("Lucene.Search: failed", e);
			}
		}
	}

	// test --------------------------------------------------------------------

	private static void createIndex() throws IOException,
			CorruptIndexException, LockObtainFailedException {
		IndexWriter writer = writer("index", true);

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

	public static void main(String[] args)
			throws IOException, ParseException {
		createIndex();
	    searchIndex();
	}

}
