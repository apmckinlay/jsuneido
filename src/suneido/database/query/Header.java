/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.concat;
import static suneido.util.Util.intersect;
import static suneido.util.Util.union;
import static suneido.util.Verify.verify;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;

import suneido.util.Util;

public class Header {
	List<List<String>> flds;
	List<String> cols;
	private List<String> fldsyms;
	private String timestamp = "";

	public Header(List<List<String>> flds, List<String> cols) {
		this.flds = flds;
		this.cols = cols;
	}

	public Header(Header x, Header y) {
		this.flds = concat(x.flds, y.flds);
		this.cols = union(x.cols, y.cols);
	}

	public int size() {
		return flds.size();
	}

	public Header project(List<String> fields) {
		List<List<String>> newhdr = new ArrayList<>();
		for (List<String> fs : flds) {
			List<String> newflds = new ArrayList<>();
			for (String g : fs)
				newflds.add(fields.contains(g) ? g : "-");
			newhdr.add(newflds);
		}
		List<String> newcols = intersect(cols, fields);
		return new Header(newhdr, newcols);
	}

	public boolean equal(Row r1, Row r2) {
		return Row.equal(this, r1, this, r2, columns());
	}

	/**
	 * @return A list of the logical columns, including rules.
	 * Does not include deleted fields.
	 */
	public List<String> columns() {
		return cols;
	}

	/**
	 * @return A list of the physical fields, as in the data records.
	 * Includes deleted fields as "-".
	 * Does not include rule columns.
	 */
	public List<String> fields() {
		// NOTE: this includes deleted fields - important for output
		if (size() == 1)
			return flds.get(0);
		if (size() == 2)
			return flds.get(1);
		verify(size() % 2 == 0);
		List<String> fields = new ArrayList<>();
		// WARNING: assumes "real" data is in every other (odd) record
		for (int i = 1; i < flds.size(); i += 2)
			for (String f : flds.get(i))
				if (f.equals("-") || ! fields.contains(f))
					fields.add(f);
		return fields;
	}

	/**
	 * @return A list of the rule columns, i.e. columns() - fields()
	 */
	public List<String> rules() {
		return Util.difference(cols, fields());
	}

	/**
	 * @return A list of the logical columns with the rules capitalized.
	 */
	public List<String> schema() {
		var fs = fields();
		List<String> schema = new ArrayList<>(fs);
		for (String c : cols)
			if (! fs.contains(c))
				schema.add(isSpecialField(c) ? c : Util.capitalize(c));
		return schema;
	}

	public static boolean isSpecialField(String column) {
		return column.endsWith("_lower!");
	}

	public List<String> output_fldsyms() {
		if (fldsyms == null) {
			// WARNING: this depends on flds[1] being the actual fields
			fldsyms = fields();
		}
		return fldsyms;
	}

	public String timestamp_field() {
		if (timestamp == "") {
			timestamp = null; // no timestamp
			for (String f : fields())
				if (f.endsWith("_TS")) {
					timestamp = f;
					break;
				}
		}
		return timestamp;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("flds", flds)
				.add("cols", cols)
				.toString();
	}

}
