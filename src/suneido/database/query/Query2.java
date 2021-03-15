/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.addUnique;
import static suneido.util.Util.nil;
import static suneido.util.Util.union;
import static suneido.util.Verify.verify;

import java.util.ArrayList;
import java.util.List;

import suneido.database.immudb.Transaction;

public abstract class Query2 extends Query1 {
	Query source2;

	Query2(Query source1, Query source2) {
		super(source1);
		this.source2 = source2;
	}

	@Override
	Query transform() {
		source = source.transform();
		source2 = source2.transform();
		return this;
	}

	@Override
	Query addindex(Transaction t) {
		source2 = source2.addindex(t);
		return super.addindex(t);
	}

	@Override
	public boolean updateable() {
		return false; // override Query1 source.updateable()
	}

	@Override
	public void setTransaction(Transaction tran) {
		super.setTransaction(tran);
		source2.setTransaction(tran);
	}

	protected List<List<String>> keypairs() {
		var keys = new ArrayList<List<String>>();
		for (var k1 : source.keys())
			for (var k2 : source2.keys())
				addUnique(keys, union(k1, k2));
		verify(!nil(keys));
		return keys;
	}

	@Override
	public Header header() {
		return new Header(source.header(), source2.header());
	}

	@Override
	boolean singleDbTable() {
		return false;
	}

	@Override
	public void close() {
		super.close();
		source2.close();
	}

}
