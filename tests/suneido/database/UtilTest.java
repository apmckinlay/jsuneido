package suneido.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suneido.database.Util;


public class UtilTest {
	@Test
	public void listToCommas() {
		assertEquals("", Util.listToCommas(null));
		List<String> list = new ArrayList<String>();
		assertEquals("", Util.listToCommas(list));
		list.add("one");
		assertEquals("one", Util.listToCommas(list));
		list.add("two");
		assertEquals("one,two", Util.listToCommas(list));
		list.add("three");
		assertEquals("one,two,three", Util.listToCommas(list));
	}
}
