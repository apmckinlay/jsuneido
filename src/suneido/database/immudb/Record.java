/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.Packable;
import suneido.language.Pack;

public abstract class Record implements Comparable<Record>, Packable {
	public static Record EMPTY = new MemRecord();

	public abstract int size();

	public abstract int fieldOffset(int i);

	public abstract int fieldLength(int i);

	public abstract ByteBuffer fieldBuffer(int i);

	public abstract int store(Storage stor);

	public abstract int length();

	public int packSize(int nest) {
		return length();
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (that instanceof Record)
			return 0 == compareTo((Record) that);
		return false;
	}

	@Override
	public int compareTo(Record that) {
		int len1 = this.size();
		int len2 = that.size();
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(this, that, i);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public boolean startsWith(Record rec) {
		int n = rec.size();
		if (n > size())
			return false;
		for (int i = 0; i < n; ++i)
			if (0 != compare1(this, rec, i))
				return false;
		return true;
	}

	private static int compare1(Record x, Record y, int i) {
		return compare1(
				x.fieldBuffer(i), x.fieldOffset(i), x.fieldLength(i),
				y.fieldBuffer(i), y.fieldOffset(i), y.fieldLength(i));
	}

	public static int compare1(
			ByteBuffer buf1, int off1, int len1,
			ByteBuffer buf2, int off2, int len2) {
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = (buf1.get(off1 + i) & 0xff) - (buf2.get(off2 + i) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public Object get(int i) {
		if (i >= size())
			return "";
		return get(fieldBuffer(i), fieldOffset(i), fieldLength(i));
	}

	public static Object get(ByteBuffer buf, int off, int len) {
		if (len == 0)
			return "";
		byte x = buf.get(off);
		if (x == 'c' || x == 's' || x == 'l')
			return new DbRecord(buf, off);
		ByteBuffer b = buf.duplicate();
		b.position(off);
		b.limit(off + len);
		return Pack.unpack(b);
		// TODO change unpack to take buf,i,n to eliminate duplicate
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		for (int i = 0; i < size(); ++i) {
			Object x = get(i);
			if (x instanceof Integer)
				sb.append(Integer.toHexString(((Integer) x)));
			else
				sb.append(x);
			sb.append(",");
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

}
