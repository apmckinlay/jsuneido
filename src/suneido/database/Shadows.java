package suneido.database;

import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.util.ByteBuf;
import suneido.util.LruCache;

@ThreadSafe
public class Shadows {
	private final Map<Long, ByteBuf> shadows = new HashMap<Long, ByteBuf>();
	private final LruCache<Long, ByteBuf> readcache = new LruCache<Long, ByteBuf>(20);

	// used by complete > writeBtreeNodes
	public synchronized Set<Map.Entry<Long, ByteBuf>> entrySet() {
		//assert Thread.holdsLock(Database.commitLock);
		return shadows.entrySet();
	}

	public synchronized ByteBuf node(Destination dest, long offset) {
		ByteBuf buf = shadows.get(offset);
		if (buf == null) {
			buf = readcache.get(offset);
			if (buf == null) {
				buf = dest.adr(offset).copy(Btree.NODESIZE);
				readcache.put(offset, buf);
			}
		}
		return buf;
	}

	public synchronized ByteBuf nodeForWrite(Destination dest, long offset) {
		ByteBuf buf = shadows.get(offset);
		if (buf == null) {
			buf = readcache.get(offset);
			if (buf == null)
				buf = dest.adr(offset).copy(Btree.NODESIZE);
			else
				readcache.remove(offset);
			shadows.put(offset, buf);
			return buf;
		} else if (!buf.isReadOnly())
			return buf;
		else
			return null; // write conflict - shadowed so another tran wrote it
	}

	public synchronized ByteBuf shadow(Destination dest, Long offset, ByteBuf copy) {
		//assert Thread.holdsLock(Database.commitLock);
		readcache.remove(offset);
		ByteBuf b = shadows.get(offset);
		if (b == null) {
			if (copy == null)
				copy = dest.adr(offset).readOnlyCopy(Btree.NODESIZE);
			shadows.put(offset, copy);
		}
		return copy;
	}

}
