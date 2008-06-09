package suneido.database;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DestMem extends Destination {
	private ArrayList<ByteBuffer> nodes;
	
	@Override
	public long alloc(int size) {
		nodes.add(ByteBuffer.allocate(size));
		return nodes.size() - 1;
	}
	
	@Override
	public ByteBuffer adr(long offset) {
		return nodes.get((int) offset);
	}
}
