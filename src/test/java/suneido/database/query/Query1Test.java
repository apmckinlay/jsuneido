/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.database.query.Query1.prefixed;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Query1Test {

	@Test
	public void test_prefixed() {
		List<Fixed> noFixed = Collections.emptyList();
		assertTrue(prefixed(list("a"), list("a"), noFixed));
		assertFalse(prefixed(list("a"), list("b"), noFixed));

		assertTrue(prefixed(list("a", "b"), list("a"), noFixed));
		assertTrue(prefixed(list("a", "b"), list("a", "b"), noFixed));

		assertFalse(prefixed(list("a", "b", "c"), list("b"), noFixed));
		assertTrue(prefixed(list("a", "b", "c"), list("b"), fixed("a")));
		assertTrue(prefixed(list("a", "b", "c"), list("b", "c"), fixed("a")));

		assertFalse(prefixed(list("b", "c"), list("a", "b"), fixed("x")));
		assertTrue(prefixed(list("b", "c"), list("a", "b"), fixed("a")));

		assertTrue(prefixed(list("a", "b"), list("b", "c"), fixed("a", "c")));
	}

	private static List<String> list(String... fields) {
		return ImmutableList.copyOf(fields);
	}

	private static List<Fixed> fixed(String... fields) {
		List<Fixed> fixed = Lists.newArrayList();
		for (String field : fields)
			fixed.add(new Fixed(field, new Object()));
		return fixed;
	}

}
