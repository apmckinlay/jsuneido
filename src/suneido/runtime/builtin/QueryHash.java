/* Copyright 2023 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.Adler32;

import com.google.common.collect.Lists;

import suneido.TheDbms;
import suneido.database.query.Header;
import suneido.database.query.Query.Dir;
import suneido.database.query.Row;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Dnum;
import suneido.runtime.Pack;

public final class QueryHash {

	@Params("query, details = false")
	public static Object QueryHash(Object a, Object b) {
		var query = Ops.toStr(a);
		var details = Ops.toBoolean(b);
		var dbms = TheDbms.dbms();
		var t = dbms.transaction(false);
		var q = t.query(query);
		if (details) {
			System.out.println(q);
		}
		var hdr = q.header();
		var fields = Lists.newArrayList(hdr.fields());
		fields.removeIf(s -> "-".equals(s));
		fields.sort(null);
		// System.out.println("fields: " + fields);
		var adler = new Adler32();
		Row row;
		int hash = hashCols(adler, hdr, details);
		int n = 0;
		while (null != (row = q.get(Dir.NEXT))) {
			hash += hashRow(adler, hdr, fields, row);
			// System.out.println("row " + row);
			++n;
			// if (n >= 10)
			//  break;
		}
		t.complete();
		if (details) {
			System.out.println("nrows " + n);
		}
		return Dnum.from(hash & 0xffffffffL);
	}

	private static int hashCols(Adler32 adler, Header hdr, boolean details) {
		int hash = 31;
		var cols = Lists.newArrayList(hdr.columns());
		cols.sort(null);
		for (var col : cols) {
			adler.reset();
			adler.update(col.getBytes());
			hash = hash * 31 + (int) adler.getValue();
		}
		if (details) {
			System.out.println("cols " + (hash & 0xffffffffL) + " " + cols);
		}
		return hash;
	}

	private static int hashRow(Adler32 adler,
			Header hdr, List<String> fields, Row row) {
		int hash = 0;
		for (var fld : fields) {
			var buf = row.getraw(hdr, fld);
			hash += hashField(adler, buf);
			// System.out.print(fld + ": " + Pack.unpack(row.getraw(hdr, fld)) + " ");
		}
		// System.out.println("");
		return hash;
	}

	private static int hashField(Adler32 adler, ByteBuffer buf) {
		adler.reset();
		adler.update(buf);
		return (int) adler.getValue();
	}

}
