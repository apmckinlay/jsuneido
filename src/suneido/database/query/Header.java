package suneido.database.query;

import static suneido.Suneido.verify;
import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuString;

public class Header {
	List<List<String>> flds;
	List<String> cols;
	private SuString[] fldsyms;
	private SuString timestamp = SuString.EMPTY;

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
		List<List<String>> newhdr = new ArrayList<List<String>>();
		for (List<String> fs : flds) {
			List<String> newflds = new ArrayList<String>();
			for (String g : fs)
				newflds.add(fields.contains(g) ? g : "-");
			newhdr.add(newflds);
		}
		List<String> newcols = intersect(cols, fields);
		return new Header(newhdr, newcols);
	}

	public Header rename(List<String> from, List<String> to) {
		int i;
		List<List<String>> newhdr = new ArrayList<List<String>>();
		for (List<String> f : flds)
			if (intersect(from, f).isEmpty())
				newhdr.add(f);
			else
				{
				List<String> newflds = new ArrayList<String>();
				for (String g : f)
					if (-1 == (i = from.indexOf(g)))
						newflds.add(g);
					else
						newflds.add(to.get(i));
				newhdr.add(newflds);
				}
		List<String> newcols = new ArrayList<String>();
		for (String c : cols)
			if (-1 == (i = from.indexOf(c)))
				newcols.add(c);
			else
				newcols.add(to.get(i));
		return new Header(newhdr, newcols);
	}

	public boolean equal(Row r1, Row r2) {
		for (String f : columns())
			if (!r1.getraw(this, f).equals(r2.getraw(this, f)))
				return false;
		return true;
	}

	public List<String> columns() {
		return cols;
	}

	public List<String> fields() {
		// NOTE: this includes deleted fields - important for output
		if (size() == 1)
			return flds.get(0);
		if (size() == 2)
			return flds.get(1);
		verify(size() % 2 == 0);
		List<String> fields = new ArrayList<String>();
		// WARNING: assumes "real" data is in every other (odd) record
		for (int i = 1; i < flds.size(); i += 2)
			for (String f : flds.get(i))
				if (f.equals("-") || !fields.contains(f))
					fields.add(f);
		return fields;
	}

	List<String> rules() {
		List<String> rules = new ArrayList<String>();
		for (String c : cols)
			if (!inflds(flds, c))
				rules.add(c);
		return rules;
	}

	// schema is all the columns with the rules capitalized
	public List<String> schema() {
		List<String> schema = fields();
		for (String c : cols)
			if (!inflds(flds, c)) {
				String s = c.substring(0, 1).toUpperCase() + c.substring(1);
				schema.add(s);
			}
		return schema;
	}

	public SuString[] output_fldsyms() {
		if (fldsyms == null) {
			// WARNING: this depends on flds[1] being the actual fields
			List<String> flds1 = flds.get(1);
			int n = flds1.size();
			fldsyms = new SuString[n];
			for (int i = 0; i < n; ++i)
				fldsyms[i] = flds1.get(i) == "-"
						? null : SuString.valueOf(flds1.get(i));
		}
		return fldsyms;
	}

	public SuString timestamp_field() {
		if (timestamp == SuString.EMPTY) {
			timestamp = null; // no timestamp
			for (String f : flds.get(1))
				if (f.endsWith("_TS")) {
					timestamp = SuString.valueOf(f);
					break;
				}
		}
		return timestamp;
	}

	static boolean inflds(List<List<String>> flds, String field) {
		for (List<String> f : flds)
			if (f.contains(field))
				return true;
		return false;
	}

}
