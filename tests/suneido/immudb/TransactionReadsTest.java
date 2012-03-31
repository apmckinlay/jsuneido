/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TransactionReadsTest {
	private final TransactionReads trs = new TransactionReads();

	@Test
	public void empty() {
		assertThat(str(), is("[]"));
	}

	@Test
	public void singleValue() {
		add(123, 123);
		assertEquals("[[[123]..[123]]]", str());
		assert trs.contains(rec(123));
	}

	@Test
	public void singleRange() {
		add(123, 456);
		assertEquals("[[[123]..[456]]]", str());

		trs.add(new IndexRange(DatabasePackage.MIN_RECORD, DatabasePackage.MAX_RECORD));
		trs.build();
		assert trs.contains(rec(1));
	}

	@Test
	public void nonOverlapping() {
		add(1, 3);
		add(4, 6);
		add(8, 9);
		assertEquals(str(), "[[[1]..[3]], [[4]..[6]], [[8]..[9]]]");
		assert ! trs.contains(rec(0));
		assert trs.contains(rec(1));
		assert trs.contains(rec(2));
		assert trs.contains(rec(3));
		assert trs.contains(rec(4));
		assert trs.contains(rec(5));
		assert trs.contains(rec(6));
		assert ! trs.contains(rec(7));
		assert trs.contains(rec(8));
		assert trs.contains(rec(9));
		assert ! trs.contains(rec(10));
	}

	@Test
	public void overlapping() {
		add(1, 2);
		add(3, 5);
		add(4, 6);
		add(7, 8);
		assertEquals(str(), "[[[1]..[2]], [[3]..[6]], [[7]..[8]]]");
	}

	void add(int lo, int hi) {
		trs.add(new IndexRange(rec(lo), rec(hi)));
	}

	Record rec(int n) {
		return new RecordBuilder().add(n).build();
	}

	String str() {
		trs.build();
		return trs.toString();
	}

}
