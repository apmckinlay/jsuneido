/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

public class BlockMergeTreeTest {

	@Test
	public void test() {
		ArrayList<Integer> list = new ArrayList<>();
		Random rand = new Random();
		BlockMergeTree<Integer> bmt = new BlockMergeTree<>(2);
		int N = 64 + 16 + 4 + 1;
		for (int i = 0; i < N; ++i) {
			int n = rand.nextInt();
			bmt.add(n);
			list.add(n);
		}
		bmt.check();
		//bmt.print();
		int i = 0;
		list.sort(null);
		for (Integer x : bmt)
			assertThat(x, equalTo(list.get(i++)));
	}

}
