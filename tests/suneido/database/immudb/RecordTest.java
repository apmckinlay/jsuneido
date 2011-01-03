/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class RecordTest {
	static final Data one = new DataBytes(new byte[] { 'o', 'n', 'e' });
	static final Data two = new DataBytes(new byte[] { 't', 'w', 'o' });
	static final Data three = new DataBytes(new byte[] { 't', 'h', 'r', 'e', 'e' });

	@Test
	public void main() {
		MemRecord mr = new MemRecord();
		assertEquals(0, mr.size());
		mr.add(one);
		mr.add(two);
		mr.add(three);
		assertEquals(3, mr.size());
		assertEquals(one, mr.get(0));
		assertEquals(two, mr.get(1));
		assertEquals(three, mr.get(2));

		ByteBuffer buf = mr.asByteBuffer();
		DbRecord dbr = new DbRecord(buf);
		assertEquals(3, dbr.size());
		assertEquals(one, dbr.get(0));
		assertEquals(two, dbr.get(1));
		assertEquals(three, dbr.get(2));
	}

	@Test
	public void compare() {
		MemRecord mr1 = new MemRecord();
		MemRecord mr2 = new MemRecord();
		assertEquals(mr1, mr2);
		assertEquals(mr2, mr1);
		mr1.add(one);
		mr1.add(two);
		mr2.add(one);
		mr2.add(two);
		assertEquals(mr1, mr2);
		assertEquals(mr2, mr1);
		mr1.add(three);
		assertEquals(+1, Integer.signum(mr1.compareTo(mr2)));
		assertEquals(-1, Integer.signum(mr2.compareTo(mr1)));
		mr2.add(two);
		assertEquals(-1, Integer.signum(mr1.compareTo(mr2)));
		assertEquals(+1, Integer.signum(mr2.compareTo(mr1)));
	}

}
