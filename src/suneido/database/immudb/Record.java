/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.language.Pack;

/**
 * A list of values stored in a ByteBuffer in the same format as cSuneido.
 * Values are stored using {@link suneido.language.Pack}
 * @see RecordBuilder
 */
@Immutable
public class Record extends RecordBase<Object> implements Comparable<Record> {
	public static final Record EMPTY = new Record();

	protected Record() {
		super();
	}

	public Record(ByteBuffer buf, int offset) {
		super(buf, offset);
	}

	public static Record of(Object a) {
		return new RecordBuilder().add(a).build();
	}

	public static Record of(Object a, Object b) {
		return new RecordBuilder().add(a).add(b).build();
	}

	public Object get(int i) {
		if (i >= size())
			return "";
		ByteBuffer b = buf.duplicate();
		int pos = offset + fieldOffset(buf, offset, i);
		b.position(pos);
		b.limit(pos + fieldLength(buf, offset, i));
		return Pack.unpack(b);
		// TODO change unpack to take buf,i,n to eliminate duplicate
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
		return compare(this.buf, this.offset, that.buf, that.offset);
	}

	public static int compare(
			ByteBuffer buf, int offset,
			ByteBuffer buf2, int offset2) {
		int len1 = size(buf, offset);
		int len2 = size(buf2, offset2);
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(
					buf, offset + fieldOffset(buf, offset, i), fieldLength(buf, offset, i),
					buf2, offset2 + fieldOffset(buf2, offset2, i), fieldLength(buf2, offset2, i));
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public static int compare1(
			ByteBuffer buf1, int idx1, int len1,
			ByteBuffer buf2, int idx2, int len2) {
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = (buf1.get(idx1 + i) & 0xff) - (buf2.get(idx2 + i) & 0xff);
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
			if (0 != compare1(
					buf, offset + fieldOffset(i), fieldLength(i),
					rec.buf, rec.offset + rec.fieldOffset(i), rec.fieldLength(i)))
				return false;
		return true;
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
		sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

}
