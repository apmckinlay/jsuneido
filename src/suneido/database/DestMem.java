package suneido.database;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DestMem extends Destination {
	private ArrayList<ByteBuffer> nodes = new ArrayList<ByteBuffer>();
	
	@Override
	public long alloc(int size) {
		nodes.add(ByteBuffer.allocate(size));
		return nodes.size(); // start at one not zero
	}
	
	@Override
	public ByteBuffer adr(long offset) {
		return nodes.get((int) offset - 1);
	}
}
