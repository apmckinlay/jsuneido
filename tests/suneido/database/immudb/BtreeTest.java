/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static suneido.database.immudb.RecordTest.record;

import java.util.*;

import org.junit.After;
import org.junit.Test;

public class BtreeTest {

	@Test
	public void main() {
		List<Record> keys = new ArrayList<Record>();
		for (char c = 'a'; c <= 'z'; ++c)
			keys.add(record("" + c));
		Random rand = new Random(1234);
//		Collections.shuffle(keys, rand);

		IntRefs.set(new IntRefs.Impl());
		Btree btree = new Btree();
		for (Record key : keys)
			btree.add(key);
	}

	@After
	public void teardown() {
		IntRefs.set(null);
	}

}
