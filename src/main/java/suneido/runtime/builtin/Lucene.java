/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.TokenStream;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.BuiltinClass;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;

/*
Lucene.Update("lucene", create:) {|u| u.Insert('now', 'now is the time for all good men') }
Lucene.Search("lucene", "good") {|key| Print(key: key) }
*/

public class Lucene extends BuiltinClass {
	public static final Version version = Version.LATEST;
	public static final Lucene singleton = new Lucene();

	private Lucene() {
		super(Lucene.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Lucene");
	}

	@Params("dir")
	public static Object AvailableQ(Object self, Object a) {
		String path = Ops.toStr(a);
		try (FSDirectory dir = FSDirectory.open(new File(path));
				DirectoryReader reader = DirectoryReader.open(dir);) {
			new IndexSearcher(reader);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Params("dir, block, create = false")
	public static Object Update(Object self, Object a, Object b, Object c) {
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

	// needs to be public to use BuiltinMethods
	public static class Updater extends SuValue {
		private static BuiltinMethods methods = new BuiltinMethods(Updater.class);
		private final IndexWriter writer;

		Updater(String dir, boolean create) {
			writer = writer(dir, create);
		}

		@Override
		public SuValue lookup(String method) {
			return methods.lookup(method);
		}

		@Params("key, text")
		public static Object Insert(Object self, Object key, Object text) {
			IndexWriter writer = ((Updater) self).writer;
			Document doc = new Document();
			doc.add(new StringField("key", Ops.toStr(key), Field.Store.YES));
			
			FieldType type = new FieldType();
	        type.setIndexed(true);
	        type.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
	        type.setStored(true);
	        type.setStoreTermVectors(true);
	        type.setTokenized(true);
	        type.setStoreTermVectorOffsets(true);
	        Field field = new Field("content", Ops.toStr(text), type); //with term vector enabled
	        TextField f = new TextField("ncontent", Ops.toStr(text), Field.Store.YES);
	        doc.add(field);
	        doc.add(f);

			try {
				writer.addDocument(doc);
			} catch (IOException e) {
				throw new SuException("Lucene.Update: Add failed", e);
			}
			return null;
		}

		@Params("key")
		public static Object Remove(Object self, Object key) {
			IndexWriter writer = ((Updater) self).writer;
			Term term = new Term("key", Ops.toStr(key));
			try {
				writer.deleteDocuments(term);
			} catch (IOException e) {
				throw new SuException("Lucene.Update: Delete failed", e);
			}
			return null;
		}

		void close() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new SuException("Lucene.Update: close failed", e);
			}
		}
	}

	static IndexWriter writer(String path, boolean create) {
		try {
			Directory dir = FSDirectory.open(new File(path));
			Analyzer analyzer = analyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(version, analyzer);
			iwc.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND);
			return new IndexWriter(dir, iwc);
		} catch (IOException e) {
			throw new SuException("Lucene.Update: can't open index", e);
		}
	}

	@Params("dir, query, limit, block")
	public static Object Search(Object self, Object a, Object b, Object c, Object d) {
		String path = Ops.toStr(a);
		String queryStr = Ops.toStr(b);
		int limit = Ops.toInt(c);
		try (FSDirectory dir = FSDirectory.open(new File(path));
				IndexReader ir = DirectoryReader.open(dir)) {
			IndexSearcher searcher = new IndexSearcher(ir);
			Analyzer analyzer = analyzer();
			QueryParser parser = new QueryParser("ncontent", analyzer);
			Query query = parser.parse(queryStr);
			TopDocs results = searcher.search(query, limit);
			SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
			Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
			ScoreDoc[] hits = results.scoreDocs;
			for (ScoreDoc hit : hits) {	
				int id = hit.doc;
				Document doc  = ir.document(id);
				String key = doc.get("key");
				String content = doc.get("ncontent");
				TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "ncontent", analyzer);
				TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, content, false, 4); 
				SuContainer fragments = new SuContainer();
				
				for (int j = 0; j < frag.length; j++) {
					if ((frag[j] != null) && (frag[j].getScore() > 0)) {
						fragments.add(frag[j].toString());
					}
				}
				
				Ops.call2(d, key, fragments);
			}
			return null;
		} catch (Exception e) {
			throw new SuException("Lucene.Search: failed", e);
		}
	}

	private static Analyzer analyzer() {
		return new EnglishAnalyzer();
	}
}
