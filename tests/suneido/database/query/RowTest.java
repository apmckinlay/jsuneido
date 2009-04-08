package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.util.Util.list;

import java.util.Iterator;

import org.junit.Test;

import suneido.database.Record;

public class RowTest {
	@Test
	public void test() {
		Record rec1 = new Record().add(123);
		Record rec2 = new Record().add(123).add(456);
		Row row = new Row(rec1, rec2);
		assertEquals(2, row.size());
		assertEquals("[123][123,456]", row.toString());
		assertEquals(rec2, row.getFirstData());

		Header hdr = HeaderTest.makeHeader();
		assertEquals(rec1.getraw(0), row.getraw(hdr, "a"));
		assertEquals(rec2.getraw(1), row.getraw(hdr, "b"));
		assertEquals(new Record().add(456), row.project(hdr, list("b")));

		Iterator<Row.Entry> iter = row.iterator(hdr);
		Row.Entry e = iter.next();
		assertEquals("a", e.field);
		assertEquals(rec1.getraw(0), e.value);
		e = iter.next();
		assertEquals("a", e.field);
		assertEquals(rec2.getraw(0), e.value);
		e = iter.next();
		assertEquals("b", e.field);
		assertEquals(rec2.getraw(1), e.value);
		assertFalse(iter.hasNext());
	}

}
