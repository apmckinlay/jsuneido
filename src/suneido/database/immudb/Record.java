/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import suneido.util.ByteBuf;

public interface Record {

	public void add(ByteBuf buf);

	public ByteBuf get(int i);

	public int size();

}
