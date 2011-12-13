/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb.future;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suneido.immudb.future.Record.Mode;
import suneido.intfc.database.RecordBuilder;

class RecordConvert {

	/** convert from old record format to new format
	 *	WARNING: modifies recbuf in place
	 */
	public static Record oldToNew(ByteBuffer recbuf, int n) {
		recbuf.order(ByteOrder.LITTLE_ENDIAN);
		int mode = recbuf.get(0);
		mode = mode == 'c' ? Mode.BYTE : mode == 's' ? Mode.SHORT : Mode.INT;
		int nfields = (recbuf.get(2) & 0xff) + (recbuf.get(3) << 8);
		assert recbuf.get(3) < 0x3f;
		recbuf.put(3, (byte) (recbuf.get(3) | (mode << 6)));
		switch (mode) {
		case Mode.BYTE:
			for (int i = 0; i < nfields + 1; ++i)
				recbuf.put(4 + i, (byte) (recbuf.get(4 + i) - 2));
			break;
		case Mode.SHORT:
			for (int i = 0; i < nfields + 1; ++i)
				recbuf.putShort(4 + i * 2, (short) ((recbuf.getShort(4 + i * 2) & 0xffff) - 2));
			break;
		case Mode.INT:
			for (int i = 0; i < nfields + 1; ++i)
				recbuf.putInt(4 + i * 4, recbuf.getInt(4 + i * 4) - 2);
			break;
		default:
			throw new RuntimeException("bad record type");
		}
		Record rec = new Record(recbuf, 2);
		assert rec.bufSize() == n - 2;
		rec.check();
		return rec;
	}

	/**
	 * also omits deleted fields
	 */
	public static suneido.intfc.database.Record newToOld(Record rec, List<String> fields) {
		RecordBuilder rb = new suneido.database.Record(rec.bufSize() + 2);
		int i = 0;
		for (String f : fields) {
			if (! f.equals("-"))
				rb.add(rec.getRaw(i));
			++i;
		}
		return rb.build();
	}

}
