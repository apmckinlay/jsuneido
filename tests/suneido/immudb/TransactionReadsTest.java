/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TransactionReadsTest {
	private final TransactionReads list = new TransactionReads();

	@Test
	public void empty() {
		assertThat(str(), is("[]"));
	}

	@Test
	public void single() {
		add(123, 456);
		assertEquals("[[[123]..[456]]]", str());
	}

	@Test
	public void nonOverlapping() {
		add(1, 2);
		add(3, 4);
		add(5, 6);
		assertEquals(str(), "[[[1]..[2]], [[3]..[4]], [[5]..[6]]]");
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
		list.add(new IndexRange(rec(lo), rec(hi)));
	}

	Record rec(int n) {
		return new RecordBuilder().add(n).build();
	}

	String str() {
		list.build();
		return list.toString();
	}

}
