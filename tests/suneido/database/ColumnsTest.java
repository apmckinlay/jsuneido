package suneido.database;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import suneido.SuException;

public class ColumnsTest {
	@Test
	public void commas_to_nums() {
		Columns columns = new Columns();
		columns.add(new Column("a", (short) 1));
		columns.add(new Column("b", (short) 2));
		columns.add(new Column("c", (short) 3));
		assertArrayEquals(new short[] { 3, 2 }, columns.commaToNums("c,b"));
	}

	@Test(expected = SuException.class)
	public void column_not_found() {
		new Columns().commaToNums("a,b");
	}

}
