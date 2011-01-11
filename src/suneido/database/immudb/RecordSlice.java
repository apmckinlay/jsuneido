/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

@Immutable
@SuppressWarnings("rawtypes")
public class RecordSlice implements Bufferable {
	private final RecordBase rec;
	private final int idx;
	private final int len;

	public RecordSlice(RecordBase rec, int idx, int len) {
		this.rec = rec;
		this.idx = idx;
		this.len = len;
	}

	@Override
	public int nBufferable() {
		return len;
	}

	@Override
	public int lengths(int[] lengths, int at) {
		for (int i = 0; i < len; ++i)
			lengths[at + i] = rec.fieldLength(idx + i);
		return len;
	}

	@Override
	public void addTo(ByteBuffer buf) {
		for (int i = idx + len - 1; i >= idx; --i)
			rec.addFieldTo(i, buf);
	}

}
