package suneido.database;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DestMem implements Destination {
	private ArrayList<ByteBuffer> nodes = new ArrayList<ByteBuffer>();
	
	public long alloc(int size) {
		nodes.add(ByteBuffer.allocate(size));
		return nodes.size() << Mmfile.SHIFT; // start at one not zero
	}
	
	public ByteBuffer adr(long offset) {
		return nodes.get((int) (offset >> Mmfile.SHIFT) - 1);
	}

	public long size() {
		return (nodes.size() + 1) << Mmfile.SHIFT;
	}
	
	public boolean visible(int tran, long adr) {
		return true;
	}
	
	public TranRead read_act(int tran, int tblnum, String index) {
		return new TranRead(tblnum, index);
	}
}
