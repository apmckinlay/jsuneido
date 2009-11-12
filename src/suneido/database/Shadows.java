package suneido.database;

import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.util.ByteBuf;

@ThreadSafe
public class Shadows {
	private final Map<Long, ByteBuf> shadows = new HashMap<Long, ByteBuf>();

	public synchronized ByteBuf get(long offset) {
		return shadows.get(offset);
	}

	public synchronized void put(long offset, ByteBuf buf) {
		shadows.put(offset, buf);
	}

	public synchronized ByteBuf redirect(Database db, Long offset, ByteBuf copy) {
		ByteBuf b = shadows.get(offset);
		if (b != null && ! b.isDirect())
			return copy; // already redirected
		if (copy == null)
			copy = db.dest.adr(offset).copy(Btree.NODESIZE)
					.asReadOnlyBuffer(); // shared read-only copy
		if (b == null)
			shadows.put(offset, copy);
		else
			b.update(copy);
		return copy;
	}

	public synchronized Set<Map.Entry<Long, ByteBuf>> entrySet() {
		return shadows.entrySet();
	}

}
