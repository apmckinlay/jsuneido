/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import suneido.util.Errlog;

public class DbGoodTest {

	@Test
	public void create_check() throws IOException {
		int errs = Errlog.count();
		File file = File.createTempFile("dbgood", null);
		file.deleteOnExit();
		String filename = file.getPath();
		int size = 123456789;
		assertFalse(DbGood.check(filename, size)); // empty file
		DbGood.create(filename, size);
		assertTrue(DbGood.check(filename, size));
		assertFalse(DbGood.check(filename, size - 1)); // size mismatch
		assert file.delete();
		assertFalse(DbGood.check(filename, size)); // missing file
		assertThat(Errlog.count(), equalTo(errs));
	}

}
