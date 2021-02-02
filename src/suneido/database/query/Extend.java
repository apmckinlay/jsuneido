/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import suneido.SuException;
import suneido.database.immudb.Dbpkg;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.expr.Expr;

public class Extend extends Query1 {
	List<String> flds; // modified by Project.transform
	List<Expr> exprs; // modified by Project.transform
	private List<String> eflds;
	private Header hdr = null;
	private List<Fixed> fix;

	Extend(Query source, List<String> flds, List<Expr> exprs) {
		super(source);
		this.flds = flds;
		this.exprs = exprs;
		fold();
		checkDependencies();
		init();
	}

	private void fold() {
		for (int i = 0; i < exprs.size(); ++i) {
			Expr e = exprs.get(i);
			if (e != null)
				exprs.set(i, e.fold());
		}
	}

	private void checkDependencies() {
		Set<String> avail = Sets.newHashSet(source.columns());
		for (int i = 0; i < flds.size(); ++i) {
			if (exprs.get(i) != null) {
				List<String> eflds = exprs.get(i).fields();
				if (! avail.containsAll(eflds))
					throw new SuException("extend: invalid column(s) in expressions: " +
						setDifference(eflds, avail));
			}
			avail.add(flds.get(i));
		}
	}

	void init() {
		List<String> srccols = source.columns();

		if (! Collections.disjoint(srccols, flds))
			throw new SuException("extend: column(s) already exist: " +
					intersect(srccols, flds));

		eflds = new ArrayList<>();
		for (Expr e : exprs)
			if (e != null)
				addAllUnique(eflds, e.fields());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(source.toString());
		sb.append(" EXTEND ");
		String sep = "";
		for (int i = 0; i < flds.size(); ++i) {
			sb.append(sep).append(flds.get(i));
			if (exprs.get(i) != null)
				sb.append(" = ").append(exprs.get(i));
			sep = ", ";
		}
		return sb.toString();
	}

	@Override
	Query transform() {
		// remove empty Extends
		if (nil(flds))
			return source.transform();
		// combine Extends
		if (source instanceof Extend) {
			Extend e = (Extend) source;
			flds = concat(e.flds, flds);
			exprs = concat(e.exprs, exprs);
			source = e.source;
			init();
			return transform();
		}
		return super.transform();
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		if (!nil(intersect(index, flds)))
			return IMPOSSIBLE;
		// NOTE: optimize1 to bypass tempindex
		return source.optimize1(index,
				setDifference(setUnion(eflds, needs), flds),
				setDifference(firstneeds, flds), is_cursor, freeze);
	}

	@Override
	List<String> columns() {
		return union(source.columns(), flds);
	}

	@Override
	int recordsize() {
		return source.recordsize() + flds.size() * columnsize();
	}

	// the changes below are so that the fields (used for output)
	// don't include the extended fields
	// since they should not be output
	// BUT had to undo these changes because they don't work client-server
	// since Header.schema and DbmsQueryRemote.parseHeader don't handle it

	@Override
	public Row get(Dir dir) {
		if (hdr == null)
			hdr = header();
		Row srcrow = source.get(dir);
		if (srcrow == null)
			return null;
		RecordBuilder rb = new RecordBuilder();
		for (int i = 0; i < flds.size(); ++i)
			if (exprs.get(i) != null) {
//				Row row = srcrow.with(rb.build(), Dbpkg.MIN_RECORD);
				Row row = srcrow.with(Dbpkg.MIN_RECORD, rb.build());
				rb.add(exprs.get(i).eval(hdr, row));
			}
//		return srcrow.with(rb.build(), Dbpkg.MIN_RECORD);
		return srcrow.with(Dbpkg.MIN_RECORD, rb.build());
	}

	@Override
	public Header header() {
		return new Header(source.header(),
//				new Header(asList(real_fields(), noFields), flds));
				new Header(asList(noFields, real_fields()), flds));
	}

	/** i.e. not rules */
	private List<String> real_fields() {
		ImmutableList.Builder<String> b = ImmutableList.builder();
		for (int i = 0; i < flds.size(); ++i)
			if (exprs.get(i) != null)
				b.add(flds.get(i));
		List<String> real_flds = b.build();
		return real_flds;
	}

	boolean hasRules() {
		return exprs.contains(null);
	}

	/**
	 * @return Whether or not a field depends on a rule.
	 * This could be indirect e.g a depends on rule in: extend r, a = r
	 */
	boolean needRule(List<String> fields) {
		for (String fld : fields)
			if (needRule(fld))
				return true;
		return false;
	}

	// recursive
	private boolean needRule(String fld) {
		int i = flds.indexOf(fld);
		if (i == -1)
			return false; // fld is not a result of extend
		if (exprs.get(i) == null)
			return true; // direct dependency
		List<String> exprdeps = exprs.get(i).fields();
		return needRule(exprdeps);
	}

	@Override
	boolean singleDbTable() {
		return false;
	}

	@Override
	List<Fixed> fixed() {
		if (fix != null)
			return fix;
		Expr e;
		Object val;
		fix = new ArrayList<>();
		for (int i = 0; i < flds.size(); ++i)
			if (null != (e = exprs.get(i)) && null != (val = e.constant()))
				fix.add(new Fixed(flds.get(i), val));
		fix = Fixed.combine(fix, source.fixed());
		return fix;
	}

}
