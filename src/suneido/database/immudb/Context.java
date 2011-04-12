/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class Context {
	public final Storage stor;
	public final IntRefs intrefs = new IntRefs();

	public Context(Storage stor) {
		this.stor = stor;
	}
}
