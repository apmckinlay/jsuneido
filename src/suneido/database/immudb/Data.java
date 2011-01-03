/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

/**
 * Record field values.
 */
public abstract class Data implements Comparable<Data> {
	public abstract int length();

	public abstract void addTo(ByteBuffer buf);

	public abstract byte[] asArray();

	public abstract byte byteAt(int i);

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof Data)
			return 0 == compareTo((Data) other);
		return false;
	}

	/** compares bytes as unsigned (to match cSuneido) */
	@Override
	public int compareTo(Data that) {
		int len1 = this.length();
		int len2 = that.length();
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = (this.byteAt(i) & 0xff) - (that.byteAt(i) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

}
