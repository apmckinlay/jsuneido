package suneido.database;

import java.nio.ByteBuffer;

public abstract class Slots {
	protected ByteBuffer buf;

	public abstract static class Slot {
		abstract int compareTo(Slot other);
	}
	public abstract static class Factory <T> {
		abstract T create(ByteBuffer buf);
	}
	
	public Slots(ByteBuffer buf) {
		this.buf = buf;
	}

	abstract public boolean empty();
	abstract public int size();
	abstract public int end();
	abstract public Slot front();
	abstract public Slot back();
	abstract public Slot get(int i);
	abstract public boolean insert(int i, Slot slot);
	abstract public void append(Slots slots, int begin, int end);
	abstract public void erase(int i);
	abstract public void erase(Slot slot);
	abstract public void erase(int begin, int end);
	
	final private static int NEXT_OFFSET = Btree.NODESIZE - 4;
	final private static int PREV_OFFSET = Btree.NODESIZE - 8;
	public long next() {
		return buf.getLong(NEXT_OFFSET);
	}
	public long prev() {
		return buf.getLong(PREV_OFFSET);
	}
	public void setNext(long value) {
		buf.putLong(NEXT_OFFSET, value);
	}
	public void setPrev(long value) {
		buf.putLong(PREV_OFFSET, value);
	}

	public static void setBufNext(ByteBuffer buf, long value) {
		buf.putLong(NEXT_OFFSET, value);
	}
	public static void setBufPrev(ByteBuffer buf, long value) {
		buf.putLong(PREV_OFFSET, value);
	}
	
	public int lower_bound(Slot slot) {
		return 0;
	}
}
