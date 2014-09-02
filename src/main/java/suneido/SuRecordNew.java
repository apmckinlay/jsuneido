/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.Suneido.dbpkg;
import static suneido.util.Verify.verify;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.server.DbmsTran;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.runtime.Pack;
import suneido.runtime.builtin.RecordMethods;
import suneido.runtime.builtin.SuTransaction;

/**
 * Implements a new version of SuRecord
 * by layering {@link SuRules} and {@link SuObservers} on top of {@link SuContainer}
 *
 * NOTE: this version is not in use, it does not pass all the tests
 */
public class SuRecordNew extends SuObservers {
	private Header hdr;
	private SuTransaction tran;
	private int recadr;
	enum Status { NEW, OLD, DELETED };
	private Status status;

	{ defval = ""; }

	public SuRecordNew() {
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	public SuRecordNew(SuRecordNew r) {
		super(r);
		hdr = null;
		tran = null;
		recadr = 0;
		status = r.status;
	}

	public SuRecordNew(Row row, Header hdr) {
		this(row, hdr, (SuTransaction) null);
	}

	public SuRecordNew(Row row, Header hdr, DbmsTran tran) {
		this(row, hdr, tran == null ? null : new SuTransaction(tran));
	}

	public SuRecordNew(Row row, Header hdr, SuTransaction tran) {
		this.hdr = hdr;
		this.tran = tran;
		this.recadr = row.address();
		status = Status.OLD;

		for (Iterator<Row.Entry> iter = row.iterator(hdr); iter.hasNext();) {
			Row.Entry e = iter.next();
			addField(e.field, e.value);
		}
	}

	public SuRecordNew(Record rec, List<String> flds, SuTransaction tran) {
		hdr = null;
		this.tran = tran;
		recadr = 0;
		status = Status.OLD;
		int i = 0;
		for (String field : flds)
			addField(field, rec.getRaw(i++));
	}

	@Override
	public void clear() {
		super.clear();
		hdr = null;
		tran = null;
		recadr = 0;
		status = Status.NEW;
	}

	private void addField(String field, ByteBuffer buf) {
		if (field.equals("-") || buf.remaining() == 0)
			return;
		Object x = Pack.unpack(buf);
		if (field.endsWith("_deps"))
			setdeps(baseFieldName(field), (String) x);
		else
			super.put(field, x);
	}

	/** remove "_deps" suffix */
	private static String baseFieldName(String field) {
		return field.substring(0, field.length() - 5);
	}

	@Override
	public String toString() {
		return toString("[", "]");
	}

	@Override
	public void pack(ByteBuffer buf) {
		super.pack(buf, Pack.Tag.RECORD);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuRecord());
	}

	@Override
	public Record toDbRecord(Header hdr) {
		List<String> fldsyms = hdr.output_fldsyms();
		RecordBuilder rb = dbpkg.recordBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		for (String f : fldsyms)
			if (f == null)
				rb.addMin();
			else if (f.equals(ts))
				rb.add(TheDbms.dbms().timestamp());
			else if (f.endsWith("_deps"))
				rb.add(getdeps(baseFieldName(f)));
			else if (null != (x = get(f)))
				rb.add(x);
			else
				rb.addMin();
		return rb.trim().build();
	}

	public void update() {
		ck_modify("Update");
		Record newrec = toDbRecord(hdr);
		recadr = tran.getTransaction().update(recadr, newrec);
		verify(recadr != 0);
	}

	public void delete() {
		ck_modify("Delete");
		tran.getTransaction().erase(recadr);
	}

	private void ck_modify(String op) {
		if (tran == null)
			throw new SuException("record." + op + ": no Transaction");
		if (tran.isEnded())
			throw new SuException("record." + op
					+ ": Transaction already completed");
		if (status != Status.OLD)
			throw new SuException("record." + op + ": not an old record");
		if (recadr == 0)
			throw new SuException("record." + op + ": not a database record");
	}

	@Override
	public String typeName() {
		return "Record";
	}

	public boolean isNew() {
		return status == Status.NEW;
	}

	public SuTransaction getTransaction() {
		return tran;
	}

	@Override
	public SuValue lookup(String method) {
		SuValue m = RecordMethods.methods.getMethod(method);
		if (m != null)
			return m;
		return super.lookup(method);
	}

}
