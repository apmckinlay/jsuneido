/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import suneido.runtime.Loader;
import suneido.runtime.ModuleLoader;

public class ModuleLoaderTest {

	@Test
	public void test() {
		Loader loader = mock(Loader.class);
		when(loader.load("mod", "Name")).thenReturn("foobar");
		when(loader.load("mod", "Name2")).thenReturn(123);

		ModuleLoader m = new ModuleLoader("mod", loader);

		assertThat(m.get("Name"), equalTo((Object) "foobar"));
		verify(loader, times(1)).load("mod", "Name");
		assertThat(m.get("Name"), equalTo((Object) "foobar"));
		verify(loader, times(1)).load("mod", "Name");

		m.clear("Name");
		assertThat(m.get("Name"), equalTo((Object) "foobar"));
		verify(loader, times(2)).load("mod", "Name");

		assertThat(m.get("Name2"), equalTo((Object) 123));
	}

}
