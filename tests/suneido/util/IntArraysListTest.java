package suneido.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static suneido.util.ArraysList.CHUNK_SIZE;

import org.junit.Test;

public class IntArraysListTest {

	@Test
	public void test() {
		IntArraysList list = new IntArraysList();
		assertThat(list.size(), is(0));

		list.add(0);
		assertThat(list.size(), is(1));
		assertThat(list.get(0), is(0));

		for (int i = 1; i < 3 * CHUNK_SIZE; ++i)
			list.add(i * 2);
		for (int i = 1; i < 3 * CHUNK_SIZE; ++i)
			assertThat(list.get(i), is(i * 2));

		list.set(5, 555);
		assertThat(list.get(5), is(555));

		list.set(5 + CHUNK_SIZE, -1);
		assertThat(list.get(5 + CHUNK_SIZE), is(-1));

		list.clear();
		assertThat(list.size(), is(0));
	}

}
