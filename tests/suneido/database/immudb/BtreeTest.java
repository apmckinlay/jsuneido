/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.*;

import org.junit.After;
import org.junit.Test;

public class BtreeTest {

	@Test
	public void main() {
		List<Record> keys = new ArrayList<Record>();
		int NKEYS = 1000;
		Random rand = new Random(1234);
		for (int i = 0; i < NKEYS; ++i)
			keys.add(randomKey(rand));
		Collections.shuffle(keys, rand);

		Btree btree = new Btree();
		for (Record key : keys)
			btree.add(key);
		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat(btree.get(key), equalTo(adr(key)));
	}

	private int adr(Record key) {
		return (Integer) key.get(1);
	}

	public Record randomKey(Random rand) {
		int n = 1 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		return Record.of(s, rand.nextInt());
	}

	@After
	public void teardown() {
		Tran.remove();
	}

}
