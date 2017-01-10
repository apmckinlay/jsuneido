/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;

import java.nio.ByteBuffer;
import java.util.List;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.database.server.Dbms.HeaderAndRow;
import suneido.database.server.Dbms.LibGet;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.runtime.builtin.ServerEval;

/**
 * Server side of the *binary* client-server protocol.
 * See {@link DbmsServerBinary} for the actual server.
 * {@link DbmsClientBinary} is the client side.
 * Uses {@link SuChannel} which uses {@link Serializer}.
 * <p>
 * Commands (requests) are identified by their ordinal.
 * NOTE: This makes the order of the enum significant.
 * The sequence must match cSuneido.
 * <p>
 * Successful responses are preceded by a boolean value of true.
 * Exceptions are returned as false followed by an exception string.
 */
public enum CommandBinary {
	/**
	 * Abort a transaction ({@link DbmsTran#abort})
	 * <p>
	 * transaction int
	 */
	ABORT {
		@Override
		public void execute(SuChannel io) {
			int tn = io.getInt();
			DbmsTran t = tran(tn);
			ServerData.forThread().endTransaction(tn);
			t.abort();
			io.put(true);
		}
	},
	/**
	 * Make a database administration request, e.g. create a table
	 * ({@link Dbms#admin})
	 * <p>
	 * string &rarr;
	 */
	ADMIN {
		@Override
		public void execute(SuChannel io) {
			String s = io.getString();
			dbms().admin(s);
			io.put(true);
		}
	},
	/**
	 * Authorize this connection ({@link Auth#auth})
	 * <p>
	 * string &rarr; boolean
	 */
	AUTH {
		@Override
		public void execute(SuChannel io) {
			String s = io.getString();
			io.put(true).put(Auth.auth(s));
		}
	},
	/**
	 * Check the database ({@link Dbms#check})
	 * <p>
	 * &rarr; string
	 */
	CHECK {
		@Override
		public void execute(SuChannel io) {
			String result = dbms().check();
			io.put(true).put(result);
		}
	},
	/**
	 * Close a query or cursor. ({@link DbmsQuery#close})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr;
	 */
	CLOSE {
		@Override
		public void execute(SuChannel io) {
			int n = io.getInt();
			if (io.getByte() == 'q')
				ServerData.forThread().endQuery(n);
			else
				ServerData.forThread().endCursor(n);
			io.put(true);
		}
	},
	/**
	 * Complete a transaction ({@link DbmsTran#complete})
	 * <p>
	 * transaction int &rarr; false or (true, string)
	 */
	COMMIT {
		@Override
		public void execute(SuChannel io) {
			int tn = io.getInt();
			DbmsTran t = tran(tn);
			ServerData.forThread().endTransaction(tn);
			String result = t.complete();
			io.put(true);
			if (result == null)
				io.put(true);
			else
				io.put(false).put(result);
		}
	},
	/**
	 * Return a list of the currently connected session ids
	 * ({@link Dbms#connections})
	 * <p>
	 * &rarr; SuContainer
	 */
	CONNECTIONS {
		@Override
		public void execute(SuChannel io) {
			SuContainer result = dbms().connections();
			io.put(true).putPacked(result);
		}
	},
	/**
	 * Open (create) a cursor ({@link Dbms#cursor})
	 * <p>
	 * query string &rarr; cursor int
	 */
	CURSOR {
		@Override
		public void execute(SuChannel io) {
			String query = io.getString();
			DbmsQuery c = dbms().cursor(query);
			int cn = ServerData.forThread().addCursor(c);
			io.put(true).put(cn);
		}
	},
	/**
	 * Return the number of open cursors ({@link Dbms#cursors})
	 * <p>
	 * &rarr; int
	 */
	CURSORS {
		@Override
		public void execute(SuChannel io) {
			int result = dbms().cursors();
			io.put(true).put(result);
		}
	},
	/**
	 * Dump a table or the database ({@link Dbms#dump})
	 * <p>
	 * table or "" &rarr; string
	 */
	DUMP {
		@Override
		public void execute(SuChannel io) {
			String table = io.getString();
			String result = dbms().dump(table);
			io.put(true).put(result);
		}
	},
	/**
	 * Delete a record ({@link DbmsTran#erase})
	 * <p>
	 * transaction int, record address int &rarr;
	 */
	ERASE {
		@Override
		public void execute(SuChannel io) {
			DbmsTran t = tran(io);
			int recadr = io.getInt();
			t.erase(recadr);
			io.put(true);
		}
	},
	/**
	 * Call a global function or method of class on the server
	 * ({@link suneido.runtime.builtin.ServerEval#exec})
	 * <p>
	 * func and args SuContainer &rarr; value packed
	 */
	EXEC {
		@Override
		public void execute(SuChannel io) {
			SuContainer c = (SuContainer) io.getPacked();
			Object result = ServerEval.exec(c);
			valueResult(io, result);
		}
	},
	/**
	 * Return the strategy for a query or cursor ({@link DbmsQuery#explain})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr; string
	 */
	EXPLAIN {
		@Override
		public void execute(SuChannel io) {
			String result = q_or_c(io).explain();
			io.put(true).put(result);
		}
	},
	/**
	 * Return the number of committed update transactions
	 * that are still outstanding because they overlap active transactions
	 * ({@link Dbms#finalSize})
	 * <p>
	 * &rarr; int
	 */
	FINAL {
		@Override
		public void execute(SuChannel io) {
			int result = dbms().finalSize();
			io.put(true).put(result);
		}
	},
	/**
	 * Get the next or previous record in a query or cursor.
	 * ({@link DbmsQuery#get})
	 * <p>
	 * '+' or '-', -1 or transaction int, cursor or query int
	 * &rarr; false or (true, recadr int, record buffer)
	 */
	GET {
		@Override
		public void execute(SuChannel io) {
			Dir dir = (io.getByte() == '-') ? Dir.PREV : Dir.NEXT;
			DbmsQuery q = q_or_tc(io);
			Row row = q.get(dir);
			if (row == null)
				io.put(true).put(EOF);
			else
				rowResult(row, q.header(), false, io);
		}
	},
	/**
	 * Get the first, last, or only record for a query string.
	 * ({@link Dbms#get}, {@link DbmsTran#get})
	 * <p>
	 * '+' or '-' or '1', -1 or transaction int, query
	 * &rarr; false or (true, recadr int, header string list, record buffer)
	 */
	GET1 {
		@Override
		public void execute(SuChannel io) {
			char d = (char) io.getByte();
			Dir dir = d == '-' ? Dir.PREV : Dir.NEXT;
			boolean one = (d == '1');
			int tn = io.getInt();
			String query = io.getString();
			HeaderAndRow hr = (tn == -1)
					? dbms().get(dir, query, one)
					: tran(tn).get(dir, query, one);
			if (hr == null)
				io.put(true).put(EOF);
			else
				rowResult(hr.row, hr.header, true, io);
		}
	},
	/**
	 * Return a list of the logical columns with rules capitalized
	 * for a query or cursor.
	 * ({@link DbmsQuery#header}, {@link suneido.database.query.Header#schema})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr; string list
	 */
	HEADER {
		@Override
		public void execute(SuChannel io) {
			List<String> result = q_or_c(io).header().schema();
			io.put(true).putStrings(result);
		}
	},
	/**
	 * Return a list of the keys for a query or cursor ({@link DbmsQuery#keys})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr; list of string list
	 */
	KEYS {
		@Override
		public void execute(SuChannel io) {
			List<List<String>> keys = q_or_c(io).keys();
			io.put(true).put(keys.size());
			keys.forEach(io::putStrings);
		}
	},
	/**
	 * Close any connections with the specified session id and
	 * return the number of connections that were closed.
	 * ({@link Dbms#kill})
	 * <p>
	 * session id string &rarr; int
	 */
	KILL {
		@Override
		public void execute(SuChannel io) {
			String sessionid = io.getString();
			int result = dbms().kill(sessionid);
			io.put(true).put(result);
		}
	},
	/**
	 * Return the definitions of a name from the libraries in use.
	 * ({@link Dbms#libget})
	 * <p>
	 * &rarr; list of (library string, size int), text buffers
	 */
	LIBGET {
		@Override
		public void execute(SuChannel io) {
			String name = io.getString();
			List<LibGet> list = dbms().libget(name);
			io.put(true).put(list.size());
			list.forEach((x) -> io.put(x.library).put(x.text.remaining()));
			list.forEach((x) -> io.putBuffer(x.text));
		}
	},
	/**
	 * Return the list of libraries currently in use.
	 * ({@link Dbms#libraries})
	 * <p>
	 * &rarr; string list
	 */
	LIBRARIES {
		@Override
		public void execute(SuChannel io) {
			List<String> result = dbms().libraries();
			io.put(true).putStrings(result);
		}
	},
	/**
	 * Load a table and return the number of records loaded.
	 * ({@link Dbms#load})
	 * <p>
	 * filename string &rarr; int
	 */
	LOAD {
		@Override
		public void execute(SuChannel io) {
			String file = io.getString();
			int result = dbms().load(file);
			io.put(true).put(result);
		}
	},
	/**
	 * Output a string to error.log ({@link Dbms#log})
	 * <p>
	 * string &rarr;
	 */
	LOG {
		@Override
		public void execute(SuChannel io) {
			String s = io.getString();
			dbms().log(s);
			io.put(true);
		}
	},
	/**
	 * Return random bytes to salt the auth hash ({@link Auth#nonce})
	 * <p>
	 * &rarr; bytes
	 */
	NONCE {
		@Override
		public void execute(SuChannel io) {
			byte[] result = Auth.nonce();
			io.put(true).put(result);
		}
	},
	/**
	 * Return the ordering for a query or cursor ({@link DbmsQuery#keys})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr; string list
	 */
	ORDER {
		@Override
		public void execute(SuChannel io) {
			List<String> result = q_or_c(io).ordering();
			io.put(true).putStrings(result);
		}
	},
	/**
	 * Output a record to a query ({@link DbmsQuery#output})
	 * <p>
	 * query int, record buffer &rarr;
	 */
	OUTPUT {
		@Override
		public void execute(SuChannel io) {
			int qn = io.getInt();
			DbmsQuery q = ServerData.forThread().getQuery(qn);
			Record rec = record(io);
			q.output(rec);
			io.put(true);
		}
	},
	/**
	 * Return a new query in a transaction ({@link DbmsTran#query})
	 * <p>
	 * transaction int, query string &rarr; query int
	 */
	QUERY {
		@Override
		public void execute(SuChannel io) {
			int tn = io.getInt();
			String query = io.getString();
			DbmsQuery q = tran(tn).query(query);
			int qn = ServerData.forThread().addQuery(tn, q);
			io.put(true).put(qn);
		}
	},
	/**
	 * Return the read count for a transaction ({@link DbmsTran#readCount})
	 * <p>
	 * transaction int &rarr; int
	 */
	READCOUNT {
		@Override
		public void execute(SuChannel io) {
			int result = tran(io).readCount();
			io.put(true).put(result);
		}
	},
	/**
	 * Make a query request i.e. update, delete, insert
	 * and return the number of records affected.
	 * ({@link DbmsTran#request})
	 * <p>
	 * transaction int, string &rarr; int
	 */
	REQUEST {
		@Override
		public void execute(SuChannel io) {
			DbmsTran t = tran(io);
			String request = io.getString();
			int n = t.request(request);
			io.put(true).put(n);
		}
	},
	/**
	 * Rewind a query or cursor ({@link DbmsQuery#rewind})
	 * <p>
	 * query or cursor int, 'q' or 'c' &rarr;
	 */
	REWIND {
		@Override
		public void execute(SuChannel io) {
			q_or_c(io).rewind();
			io.put(true);
		}
	},
	/**
	 * Evaluate an expression ({@link Dbms#run})
	 * <p>
	 * string &rarr; false or (true, value packed)
	 */
	RUN {
		@Override
		public void execute(SuChannel io) {
			String s = io.getString();
			Object result = dbms().run(s);
			valueResult(io, result);
		}
	},
	/**
	 * Get or set the session id ({@link Dbms#sessionid})
	 * <p>
	 * string or "" &rarr; string
	 */
	SESSIONID {
		@Override
		public void execute(SuChannel io) {
			String sessionId = io.getString();
			String result = dbms().sessionid(sessionId);
			io.put(true).put(result);
		}
	},
	/**
	 * Return the current size of the database ({@link Dbms#size})
	 * <p>
	 * &rarr; long
	 */
	SIZE {
		@Override
		public void execute(SuChannel io) {
			long result = dbms().size();
			io.put(true).put(result);
		}
	},
	/**
	 * Return a unique timestamp ({@link Dbms#timestamp})
	 * <p>
	 * &rarr; {@link suneido.SuDate} packed
	 */
	TIMESTAMP {
		@Override
		public void execute(SuChannel io) {
			Object result = dbms().timestamp();
			io.put(true).putPacked(result);
		}
	},
	/**
	 * Return random bytes that can be used to authorize ({@link Auth#token})
	 * <p>
	 * &rarr; bytes
	 */
	TOKEN {
		@Override
		public void execute(SuChannel io) {
			byte[] result = Auth.token();
			io.put(true).put(result);
		}
	},
	/**
	 * Start a read-only or update transaction ({@link Dbms#transaction})
	 * <p>
	 * update? boolean &rarr; transaction int
	 */
	TRANSACTION {
		@Override
		public void execute(SuChannel io) {
			boolean readwrite = io.getBool();
			int tn = ServerData.forThread().addTransaction(
					dbms().transaction(readwrite));
			io.put(true).put(tn);
		}
	},
	/**
	 * Return the list of open transactions.
	 * ({@link Dbms#transactions})
	 * <p>
	 * &rarr; int list
	 */
	TRANSACTIONS {
		@Override
		public void execute(SuChannel io) {
			List<Integer> result = dbms().transactions();
			io.put(true).putInts(result);
		}
	},
	/**
	 * Update a record ({@link DbmsTran#update})
	 * <p>
	 * transaction int, recadr int, newrec buffer &rarr; newrecadr int
	 */
	UPDATE {
		@Override
		public void execute(SuChannel io) {
			DbmsTran t = tran(io);
			int recadr = io.getInt();
			Record rec = record(io);
			recadr = t.update(recadr, rec);
			io.put(true).put(recadr);
		}
	},
	/**
	 * Return the write count for a transaction ({@link DbmsTran#writeCount})
	 * <p>
	 * transaction int &rarr; int
	 */
	WRITECOUNT {
		@Override
		public void execute(SuChannel io) {
			DbmsTran t = tran(io);
			int result = t.writeCount();
			io.put(true).put(result);
		}
	};

	//--------------------------------------------------------------------------

	public abstract void execute(SuChannel io);

	//--------------------------------------------------------------------------

	protected DbmsQuery q_or_tc(SuChannel io) {
		int tn = io.getInt();
		int qn = io.getInt();
		if (tn == -1)
			return ServerData.forThread().getQuery(qn);
		DbmsQuery c = ServerData.forThread().getCursor(qn);
		c.setTransaction(ServerData.forThread().getTransaction(tn));
		return c;
	}

	protected DbmsQuery q_or_c(SuChannel io) {
		int n = io.getInt();
		return (io.getByte() == 'q')
				? ServerData.forThread().getQuery(n)
				: ServerData.forThread().getCursor(n);
	}

	protected void rowResult(Row row, Header hdr, boolean sendHeader, SuChannel io) {
		io.put(true).put(true).put(row.address());
		if (sendHeader)
			io.putStrings(hdr.schema());
		io.put(rowToRecord(row, hdr).getBuffer());
	}

	private static Record rowToRecord(Row row, Header hdr) {
		if (row.size() == 1)
			return row.firstData();
		RecordBuilder rb = dbpkg.recordBuilder();
		for (String f : hdr.fields())
			rb.add(row.getraw(hdr, f));
		return rb.trim().build();
	}

	private static Dbms dbms() {
		Dbms dbms = TheDbms.dbms();
		if (! ServerData.forThread().auth)
			dbms = new DbmsUnauth(dbms);
		return dbms;
	}

	private static DbmsTran tran(int tn) {
		return ServerData.forThread().getTransaction(tn);
	}

	private static DbmsTran tran(SuChannel io) {
		int tn = io.getInt();
		return ServerData.forThread().getTransaction(tn);
	}

	private static Record record(SuChannel io) {
		ByteBuffer buf = io.getOwnedBuffer();
		return dbpkg.record(0, buf);
	}

	private static void valueResult(SuChannel io, Object result) {
		io.put(true);
		if (result == null)
			io.put(false);
		else
			io.put(true).putPacked(result);
	}

	private static final int EOF = 0;

}
