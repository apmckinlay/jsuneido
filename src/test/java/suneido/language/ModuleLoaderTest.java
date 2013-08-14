/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ModuleLoaderTest {

	@Test
	public void test() {
		Loader loader = mock(Loader.class);
		when(loader.load("mod", "Name")).thenReturn("foobar");
		when(loader.load("mod", "Name2")).thenReturn(123);

		ModuleLoader m = new ModuleLoader("mod", loader);

		assertThat(m.get("Name"), is((Object) "foobar"));
		verify(loader, times(1)).load("mod", "Name");
		assertThat(m.get("Name"), is((Object) "foobar"));
		verify(loader, times(1)).load("mod", "Name");

		m.clear("Name");
		assertThat(m.get("Name"), is((Object) "foobar"));
		verify(loader, times(2)).load("mod", "Name");

		assertThat(m.get("Name2"), is((Object) 123));
	}

}