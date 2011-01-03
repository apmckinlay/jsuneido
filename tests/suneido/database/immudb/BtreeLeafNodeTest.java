/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BtreeLeafNodeTest {

	@Test
	public void with() {
		Record key1 = record(RecordTest.one);
		Record key2 = record(RecordTest.three);
		Record key3 = record(RecordTest.two);

		BtreeLeafNode node = new BtreeLeafNode();
		node = node.with(key2);
		assertEquals(key2, node.get(0));

		node = node.with(key1);
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));

		node = node.with(key3);
		assertEquals(3, node.size());
		assertEquals(key1, node.get(0));
		assertEquals(key2, node.get(1));
		assertEquals(key3, node.get(2));
	}

	Record record(Data... datas) {
		Record r = new MemRecord();
		for (Data d : datas)
			r.add(d);
		return r;
	}

}
