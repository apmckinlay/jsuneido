package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ColumnsTest {
	@Test
	public void commas_to_nums() {
		Columns columns = new Columns(ImmutableList.of(
			new Column("a", 1), new Column("b", 2), new Column("c", 3)));
		assertEquals(ImmutableList.of(3, 2), columns.nums("c,b"));
	}

	@Test(expected = RuntimeException.class)
	public void column_not_found() {
		Columns columns = new Columns(ImmutableList.of(new Column("c", 3)));
		columns.nums("a,b");
	}

}
