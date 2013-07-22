package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ColumnsTest {

	@Test
	public void commas_to_nums() {
		Columns columns = new Columns(ImmutableList.of(
			column(1, "a"), column(2, "b"), column(3, "c")));
		assertEquals(ImmutableList.of(3, 2), columns.nums("c,b"));
	}

	@Test(expected = RuntimeException.class)
	public void column_not_found() {
		Columns columns = new Columns(ImmutableList.of(column(3, "c")));
		columns.nums("a,b");
	}

	private static final int TBLNUM = 1;
	private static Column column(int num, String name ) {
		return new Column(TBLNUM, num, name);
	}

}
