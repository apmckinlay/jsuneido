/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import org.junit.Test;

public class RedirectsTest {

	@Test
	public void main() {
		DbHashTree mock = mock(DbHashTree.class);
		when(mock.get(123)).thenReturn(456);
		when(mock.with(anyInt(), anyInt())).thenReturn(mock);
		Redirects r = new Redirects(mock);
		assertThat(r.get(123), is(456));
		r.put(123, 456);
		verify(mock).with(123, 456);
	}

}
