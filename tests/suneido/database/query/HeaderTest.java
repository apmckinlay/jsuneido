package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.Util.list;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HeaderTest {
	@Test
	public void test() {
		List<List<String>> flds = new ArrayList<List<String>>();
		List<String> cols = new ArrayList<String>();
		Header hdr = new Header(flds, cols);
		assertEquals(0, hdr.size());

		hdr = makeHeader();
		assertEquals(4, hdr.size());
		assertEquals(list("a", "b", "c", "x", "y", "z"), hdr.fields());
		assertEquals(list("me", "no"), hdr.rules());
		assertEquals(list("a", "b", "c", "x", "y", "z", "Me", "No"),
				hdr.schema());

		hdr = hdr.rename(list("x", "b"), list("xx", "bb"));
		assertEquals(list("a", "bb", "me", "c", "xx", "no", "y", "z"), hdr.cols);
		flds = list(list("a"), list("a", "bb", "c"),
				list("xx"), list("xx", "y", "z"));
		assertEquals(flds, hdr.flds);


		hdr = hdr.project(list("c", "y", "me", "a"));
		assertEquals(list("a", "me", "c", "y"), hdr.cols);
		flds = list(list("a"), list("a", "-", "c"),
				list("-"), list("-", "y", "-"));
		assertEquals(flds, hdr.flds);
	}

	static Header makeHeader() {
		List<List<String>> flds = list(list("a"), list("a", "b", "c"),
				list("x"), list("x", "y", "z"));
		List<String> cols = list("a", "b", "me", "c", "x", "no", "y", "z");
		return new Header(flds, cols);
	}

}
