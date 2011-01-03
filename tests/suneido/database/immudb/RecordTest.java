/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class RecordTest {

	@Test
	public void main() {
		byte[] one = new byte[] { 'o', 'n', 'e' };
		byte[] two = new byte[] { 't', 'w', 'o' };
		byte[] three = new byte[] { 't', 'h', 'r', 'e', 'e' };

		MemRecord mr = new MemRecord();
		assertEquals(0, mr.size());
		mr.add(new DataBytes(one));
		mr.add(new DataBytes(two));
		mr.add(new DataBytes(three));
		assertEquals(3, mr.size());
		assertSame(one, mr.get(0).asArray());
		assertSame(two, mr.get(1).asArray());
		assertSame(three, mr.get(2).asArray());

		ByteBuffer buf = mr.asByteBuffer();
		DbRecord dbr = new DbRecord(buf);
		assertEquals(3, dbr.size());
		assertArrayEquals(one, dbr.get(0).asArray());
		assertArrayEquals(two, dbr.get(1).asArray());
		assertArrayEquals(three, dbr.get(2).asArray());
dbr.debugPrint();
	}

}
