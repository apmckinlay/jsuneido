/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.nio.ByteBuffer;

public interface NetworkOutput {
	void add(ByteBuffer buf);
	void write();
	void close();
}
