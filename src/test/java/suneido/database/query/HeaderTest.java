/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static suneido.util.Util.union;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HeaderTest {

	@Test
	public void test() {
		List<List<String>> flds = new ArrayList<>();
		List<String> cols = new ArrayList<>();
		Header hdr = new Header(flds, cols);
		assertEquals(0, hdr.size());

		hdr = makeHeader();
		assertEquals(4, hdr.size());
		assertEquals(asList("a", "b", "c", "x", "y", "z"), hdr.fields());
		assertEquals(asList("me", "no"), hdr.rules());
		assertEquals(asList("a", "b", "c", "x", "y", "z", "Me", "No"),
				hdr.schema());

		hdr = hdr.rename(asList("x", "b"), asList("xx", "bb"));
		assertEquals(asList("a", "bb", "me", "c", "xx", "no", "y", "z"), hdr.cols);
		flds = asList(asList("a"), asList("a", "bb", "c"),
				asList("xx"), asList("xx", "y", "z"));
		assertEquals(flds, hdr.flds);


		hdr = hdr.project(asList("c", "y", "me", "a"));
		assertEquals(asList("a", "me", "c", "y"), hdr.cols);
		flds = asList(asList("a"), asList("a", "-", "c"),
				asList("-"), asList("-", "y", "-"));
		assertEquals(flds, hdr.flds);
	}

	// extend issue - see version control 2012-04-12
	@Test
	public void extend() {
		List<String> cols = asList("a", "b", "c");
		List<List<String>> flds = asList(asList("a"), cols);
		Header srchdr = new Header(flds, cols);

		List<String> ecols = asList("e1", "e2");
		List<String> rules = asList("r1", "r2");
		Header hdr = new Header(srchdr,
				new Header(asList(ecols, Query.noFields), union(ecols, rules)));
		assertEquals(asList("a", "b", "c", "e1", "e2", "r1", "r2"), hdr.columns());
		assertEquals(asList("a", "b", "c"), hdr.fields());
		List<String> schema = hdr.schema();
		assertEquals(asList("a", "b", "c", "R1", "R2"), schema);
		// BUG: doesn't include e1, e2
	}

	static Header makeHeader() {
		List<List<String>> flds = asList(asList("a"), asList("a", "b", "c"),
				asList("x"), asList("x", "y", "z"));
		List<String> cols = asList("a", "b", "me", "c", "x", "no", "y", "z");
		return new Header(flds, cols);
	}

}
