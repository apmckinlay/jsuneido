/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import org.junit.Test;

public class BlockMergeTreeTest {

	@Test
	public void test() {
		BlockMergeTree<Integer> bmt = new BlockMergeTree<>();
		for (int i = 20000; i > 0; --i)
			bmt.add(i);
		bmt.check();
	}

}
