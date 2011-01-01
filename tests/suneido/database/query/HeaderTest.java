package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HeaderTest {
	@SuppressWarnings("unchecked")
	@Test
	public void test() {
		List<List<String>> flds = new ArrayList<List<String>>();
		List<String> cols = new ArrayList<String>();
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

	@SuppressWarnings("unchecked")
	static Header makeHeader() {
		List<List<String>> flds = asList(asList("a"), asList("a", "b", "c"),
				asList("x"), asList("x", "y", "z"));
		List<String> cols = asList("a", "b", "me", "c", "x", "no", "y", "z");
		return new Header(flds, cols);
	}

}
