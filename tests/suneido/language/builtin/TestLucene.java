/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import static suneido.language.builtin.Lucene.*;

public class TestLucene {

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
