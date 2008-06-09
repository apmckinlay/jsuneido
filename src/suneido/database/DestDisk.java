package suneido.database;

import java.nio.ByteBuffer;

public class DestDisk extends Destination {
	Mmfile mmf;
	
	DestDisk(Mmfile mmf) {
		this.mmf = mmf;
	}
	
	@Override
	public long alloc(int size) {
		return mmf.alloc(size, Mmfile.OTHER);
	}
	
	@Override
	public ByteBuffer adr(long offset) {
		return mmf.adr(offset);
	}
}
