package suneido;

import java.nio.ByteBuffer;

public interface Packable {
	int packSize();
	
	/**
	 * Store the data into ByteBuffer starting at it's <b>current</b> position.
	 * (Not necessarily at position 0.) 
	 * Must leave the buffer's position at the end of it's data.
	 */
	void pack(ByteBuffer buf);
}
