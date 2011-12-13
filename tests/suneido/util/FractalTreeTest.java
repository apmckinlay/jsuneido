/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.*;

import org.junit.Test;

import com.google.common.collect.Lists;

public class FractalTreeTest {

	@Test
	public void main() {
		Random rand = new Random(98707);

		ArrayList<Integer> keys = Lists.newArrayList();
		final int NKEYS = 1000;
		for (int i = 0; i < NKEYS; ++i)
			keys.add(rand.nextInt(NKEYS));

		FractalTree<Integer> ft = new FractalTree<Integer>();
		for (Integer key : keys)
			ft.add(key);
		assertThat(ft.size(), is(NKEYS));
		Collections.sort(keys);

		int i = 0;
		for (Integer x : ft)
			assertThat(x, is(keys.get(i++)));
		assertThat(i, is(NKEYS));

		Integer[] data = ft.toArray(new Integer[0]);
		assertThat(data.length, is(NKEYS));
		for (i = 0; i < NKEYS; ++i)
			assertThat(data[i], is(keys.get(i)));
	}

}
