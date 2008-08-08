package suneido;

import java.nio.ByteBuffer;

/**
 * Interface for Suneido serialization which is used to store values in the
 * database.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public interface Packable {
	int packSize(int nest);

	/**
	 * Store the data into ByteBuffer starting at it's <b>current</b> position.
	 * (Not necessarily at position 0.)
	 * Must leave the buffer's position at the end of it's data.
	 */
	void pack(ByteBuffer buf);
}
