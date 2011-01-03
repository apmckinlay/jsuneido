/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * Stores one or more keys.
 * Keys are records.
 * Keys have the data address as the second last field
 * and pointer to child node as the last field.
 * Pointers are long offsets into database file, stored as int using {@link IntLongs}
 */
@Immutable
public class BtreeTreeNode extends DbRecord {

	public BtreeTreeNode(ByteBuffer buf) {
		super(buf);
	}

}
