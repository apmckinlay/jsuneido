/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static suneido.Suneido.dbpkg;

import org.junit.Test;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.DbmsServerBinary.ServerDataSet;
import suneido.intfc.database.Record;

/**
 * Test the client and server components by connecting DbmsClientBinary to
 * DbmsServerBinary Handler and CommandBinary using TestChannel
 */
public class ClientServerTest {
	private TestChannel channel;
	private DbmsServerBinary.DbmsServerHandler handler;

	@Test
	public void test() {
		TheDbms.set(dbpkg.testdb()); // local dbms for server, used by Command

		channel = new TestChannel(this::serverHandler);
		handler = new DbmsServerBinary.DbmsServerHandler(channel, new ServerDataSet());

		DbmsClientBinary dbmsClient = new DbmsClientBinary(channel);

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

		// CLOSE, CURSOR, CURSORS, EXPLAIN
		q = dbmsClient.cursor("tmp");
		assertThat(q.explain(), equalTo("tmp^(a)"));
		assertThat(dbmsClient.cursors(), equalTo(1));
		q.close();

		// COMPLETE
		t = dbmsClient.transaction(false);
		assertThat(t.complete(), equalTo(null));

		// EXEC
		result = dbmsClient.exec(SuContainer.of("Object", 123));
		assertThat(result, equalTo(SuContainer.of(123)));

		// GET1
		hr = dbmsClient.get(Dir.NEXT, "tables", false);
		assertThat(hr.header.toString(),
				equalTo("Header{flds=[[table, tablename]], cols=[table, tablename]}"));
		assertThat(hr.row.toString(), startsWith("[1,\"tables\"]"));

		// GET1 eof
		hr = dbmsClient.get(Dir.NEXT, "tables where table = 999", false);
		assertThat(hr, equalTo(null));

		// GET1 with transaction, READCOUNT
		t = dbmsClient.transaction(true);
		hr = t.get(Dir.NEXT, "tables where tablename = 'tmp'", true);
		assertThat(hr.header.toString(),
				equalTo("Header{flds=[[table, tablename]], cols=[table, tablename]}"));
		assertThat(hr.row.toString(), startsWith("[5,\"tmp\"]"));
		assertThat(t.readCount(), equalTo(1));
		t.abort();

		// GET, HEADER, QUERY
		t = dbmsClient.transaction(false);
		q = t.query("tables");
		assertThat(q.header().toString(),
				equalTo("Header{flds=[[table, tablename]], cols=[table, tablename]}"));
		assertThat(q.get(Dir.PREV).toString(), startsWith("[5,\"tmp\"]"));
		t.abort();

		// GET, REWIND
		q = dbmsClient.cursor("tables");
		assertThat(q.header().toString(),
				equalTo("Header{flds=[[table, tablename]], cols=[table, tablename]}"));
		t = dbmsClient.transaction(false);
		q.setTransaction(t);
		assertThat(q.get(Dir.NEXT).toString(), startsWith("[1,\"tables\"]"));
		q.rewind();
		q.setTransaction(t);
		assertThat(q.get(Dir.NEXT).toString(), startsWith("[1,\"tables\"]"));
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
		rec = dbpkg.recordBuilder().add(123).add("foo").build();
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
		assertThat(dbmsClient.sessionid(), equalTo(""));
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
		rec = dbpkg.recordBuilder().add(456).add("up").build();
		t.update(row.address(), rec);
		assertThat(t.complete(), equalTo(null));
		hr = dbmsClient.get(Dir.NEXT, "tmp", false);
		assertThat(hr.row.toString(), startsWith("[456,\"up\"]"));
	}

	private void serverHandler() {
		// not request because we don't want threads
		handler.handleRequest(channel, (channel, handler) -> { });
	}

}
