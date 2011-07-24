package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.Suneido.dbpkg;

import java.util.Iterator;

import org.junit.Test;

import suneido.intfc.database.Record;

public class RowTest {

	@Test
	public void test() {
		Record rec1 = dbpkg.recordBuilder().add(123).build();
		Record rec2 = dbpkg.recordBuilder().add(123).add(456).build();
		Row row = new Row(rec1, rec2);
		assertEquals(2, row.size());
		assertEquals("[123][123,456]", row.toString());
		assertEquals(rec2, row.getFirstData());

		Header hdr = HeaderTest.makeHeader();
		assertEquals(rec1.getRaw(0), row.getraw(hdr, "a"));
		assertEquals(rec2.getRaw(1), row.getraw(hdr, "b"));
		assertEquals(dbpkg.recordBuilder().add(456).build(), row.project(hdr, asList("b")));

		Iterator<Row.Entry> iter = row.iterator(hdr);
		Row.Entry e = iter.next();
		assertEquals("a", e.field);
		assertEquals(rec1.getRaw(0), e.value);
		e = iter.next();
		assertEquals("a", e.field);
		assertEquals(rec2.getRaw(0), e.value);
		e = iter.next();
		assertEquals("b", e.field);
		assertEquals(rec2.getRaw(1), e.value);
		assertFalse(iter.hasNext());
	}

}
