/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

public interface Storage {

	ByteBuffer buffer(int adr);

	int alloc(int byteBufSize);

}
