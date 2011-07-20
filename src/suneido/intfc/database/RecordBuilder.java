/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.nio.ByteBuffer;

public interface RecordBuilder {

	RecordBuilder addAll(Record r);
	RecordBuilder add(ByteBuffer src);
	RecordBuilder add(Object x);
	RecordBuilder addMin();
	RecordBuilder addMax();
	RecordBuilder truncate(int n);

	Record build();
}
