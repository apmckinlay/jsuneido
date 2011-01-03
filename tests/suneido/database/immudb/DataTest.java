/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataTest {

	@Test
	public void compare() {
		Data[] data = new Data[] {
				new DataBytes(new byte[] { }),
				new DataBytes(new byte[] { 'a' }),
				new DataBytes(new byte[] { 'o', 'n', 'a' }),
				new DataBytes(new byte[] { 'o', 'n', 'e' }),
				new DataBytes(new byte[] { 'o', 'n', 'e', 's' }),
				new DataBytes(new byte[] { 'z' }) };

		for (int i = 0; i < data.length; ++i)
			for (int j = 0; j < data.length; ++j)
				assertEquals(Integer.signum(i - j),
						Integer.signum(data[i].compareTo(data[j])));
	}

}
