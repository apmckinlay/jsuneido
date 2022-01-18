/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.SuObject;
import suneido.TheDbms;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.DbmsServer.ServerDataSet;

/**
 * Test the client and server components by connecting DbmsClient to
 * DbmsServer Handler and Command using TestChannel
 */
public class ClientServerTest {
	private TestChannel channel;
	private DbmsServer.DbmsServerHandler handler;

	@Test
	public void test() {
		TheDbms.set(Dbpkg.testdb()); // local dbms for server, used by Command

		channel = new TestChannel(this::serverHandler);
		handler = new DbmsServer.DbmsServerHandler(channel, new ServerDataSet());

		DbmsClient dbmsClient = new DbmsClient(channel);

		Object result;
		DbmsTran t;
		DbmsQuery q;
		HeaderAndRow hr;
		Record rec;
		Row row;

		// ABORT
		t = dbmsClient.transaction(true);
		t.abort();

		// ADMIN
		dbmsClient.admin("create tmp (a,b,C) key(a)");

		// AUTH
		assertThat(dbmsClient.auth("foo"), equalTo(false));

		// CHECK
		assertThat(dbmsClient.check(), equalTo(""));

		// CLOSE, CURSOR, CURSORS, STRATEGY
		q = dbmsClient.cursor("tmp");
		assertThat(q.strategy(), equalTo("tmp^(a) [nrecs~ 0 cost~ 1]"));
		assertThat(dbmsClient.cursors(), equalTo(1));
		q.close();

		// COMPLETE
		t = dbmsClient.transaction(false);
		assertThat(t.complete(), equalTo(null));

		// EXEC
		result = dbmsClient.exec(SuObject.of("Object", 123));
		assertThat(result, equalTo(SuObject.of(123)));

		String tables_header = "Header{" +
				"flds=[[table, tablename, nrows, totalsize]], " +
				"cols=[table, tablename, nrows, totalsize]}";

		// GET1
		hr = dbmsClient.get(Dir.NEXT, "tables", false);
		assertThat(hr.header.toString(), equalTo(tables_header));
		assertThat(hr.row.toString(), startsWith("[1,\"tables\""));

		// GET1 eof
		hr = dbmsClient.get(Dir.NEXT, "tables where table = 999", false);
		assertThat(hr, equalTo(null));

		// GET1 with transaction, READCOUNT
		t = dbmsClient.transaction(true);
		hr = t.get(Dir.NEXT, "tables where tablename = 'tmp'", true);
		assertThat(hr.header.toString(), equalTo(tables_header));
		assertThat(hr.row.toString(), startsWith("[5,\"tmp\""));
		assertThat(t.readCount(), equalTo(1));
		t.abort();

		// GET, HEADER, QUERY
		t = dbmsClient.transaction(false);
		q = t.query("tables");
		assertThat(q.header().toString(), equalTo(tables_header));
		assertThat(q.get(Dir.PREV).toString(), startsWith("[5,\"tmp\""));
		t.abort();

		// GET, REWIND
		q = dbmsClient.cursor("tables");
		assertThat(q.header().toString(), equalTo(tables_header));
		t = dbmsClient.transaction(false);
		q.setTransaction(t);
		assertThat(q.get(Dir.NEXT).toString(), startsWith("[1,\"tables\""));
		q.rewind();
		q.setTransaction(t);
		assertThat(q.get(Dir.NEXT).toString(), startsWith("[1,\"tables\""));
		t.abort();

		// KEYS
		q = dbmsClient.cursor("columns");
		assertThat(q.keys().toString(), equalTo("[[table, column]]"));
		q.close();

		// LIBGET
		assertThat(dbmsClient.libget("non-existant").toString(), equalTo("[]"));

		// LIBRARIES
		assertThat(dbmsClient.libraries().toString(), equalTo("[stdlib]"));

		// NONCE
		dbmsClient.nonce();

		// ORDER
		t = dbmsClient.transaction(false);
		q = t.query("tmp sort a, b");
		assertThat(q.ordering().toString(), equalTo("[a, b]"));
		t.abort();

		// OUTPUT, WRITECOUNT
		t = dbmsClient.transaction(true);
		assertThat(t.writeCount(), equalTo(0));
		q = t.query("tmp");
		rec = new RecordBuilder().add(123).add("foo").build();
		q.output(rec);
		assertThat(t.readCount(), equalTo(0));
		assertThat(t.writeCount(), equalTo(1));
		assertThat(t.complete(), equalTo(null));
		hr = dbmsClient.get(Dir.NEXT, "tmp", false);
		assertThat(hr.row.toString(), startsWith("[123,\"foo\"]"));

		// REQUEST
		t = dbmsClient.transaction(true);
		assertThat(t.request("update tmp set b = 'bar'"), equalTo(1));
		assertThat(t.complete(), equalTo(null));
		hr = dbmsClient.get(Dir.NEXT, "tmp", false);
		assertThat(hr.row.toString(), startsWith("[123,\"bar\"]"));

		// RUN
		assertThat(dbmsClient.run("123 + 456"), equalTo(579));

		// SESSIONID
		assertThat(dbmsClient.sessionid(), equalTo("127.0.0.1"));
		assertThat(dbmsClient.sessionid("foobar"), equalTo("foobar"));
		assertThat(dbmsClient.sessionid(), equalTo("foobar"));

		// TIMESTAMP
		dbmsClient.timestamp();

		// TOKEN
		dbmsClient.token();

		// UPDATE
		t = dbmsClient.transaction(true);
		q = t.query("tmp");
		row = q.get(Dir.NEXT);
		rec = new RecordBuilder().add(456).add("up").build();
		t.update(row.address(), rec);
		assertThat(t.complete(), equalTo(null));
		hr = dbmsClient.get(Dir.NEXT, "tmp", false);
		assertThat(hr.row.toString(), startsWith("[456,\"up\"]"));
	}

	private void serverHandler() {
		// not request because we don't want threads
		if (handler != null) // needed to handle initial greeting
			handler.handleRequest(channel, (channel, handler) -> { });
	}

}
