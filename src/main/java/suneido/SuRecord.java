/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.intfc.database.Record;
import suneido.runtime.builtin.SuTransaction;

@NotThreadSafe
public class SuRecord extends SuRecordOld {

	public SuRecord() {
	}

	public SuRecord(SuRecord rec) {
		super(rec);
	}

	public SuRecord(Row row, Header hdr) {
		this(row, hdr, (SuTransaction) null);
	}

	public SuRecord(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, tran == null ? null : new SuTransaction(tran));
	}

	public SuRecord(Row row, Header hdr, SuTransaction tran) {
		super(row, hdr, tran);
	}

	public SuRecord(Record rec, List<String> flds, SuTransaction tran) {
		super(rec, flds, tran);
	}

}
