package suneido.database;

import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.util.ByteBuf;

@ThreadSafe
public class Shadows {
	private final Map<Long, ByteBuf> shadows = new HashMap<Long, ByteBuf>();

	// used by complete > writeBtreeNodes
	public synchronized Set<Map.Entry<Long, ByteBuf>> entrySet() {
		assert Thread.holdsLock(Transaction.commitLock);
		return shadows.entrySet();
	}

	public synchronized ByteBuf node(Destination dest, long offset) {
		ByteBuf buf = shadows.get(offset);
		if (buf == null)
			buf = dest.adr(offset).readOnlyCopy(Btree.NODESIZE);
		return buf;
	}

	public synchronized ByteBuf nodeForWrite(Destination dest, long offset) {
		ByteBuf buf = shadows.get(offset);
		if (buf == null)
			buf = dest.adr(offset);
		else if (!buf.isReadOnly())
			return buf;
		buf = buf.copy(Btree.NODESIZE);
		shadows.put(offset, buf);
		return buf;
	}

	public synchronized ByteBuf redirect(Destination dest, Long offset, ByteBuf copy) {
		assert Thread.holdsLock(Transaction.commitLock);
		ByteBuf b = shadows.get(offset);
		if (b == null) {
			if (copy == null)
				copy = dest.adr(offset).readOnlyCopy(Btree.NODESIZE);
			shadows.put(offset, copy);
		}
		return copy;
	}

}
