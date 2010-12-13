/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.ArrayList;
import java.util.List;

import suneido.util.ByteBuf;

public class MemRecord implements Record {
	private final List<ByteBuf> data = new ArrayList<ByteBuf>();

	public void add(ByteBuf buf) {
		data.add(buf);
	}

	public ByteBuf get(int i) {
		return data.get(i);
	}

	public int size() {
		return data.size();
	}

}
