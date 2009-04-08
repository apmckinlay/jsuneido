package suneido.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K,V> {
	private final LinkedHashMap<K, V> map;
	private final int cacheSize;
	private static final float loadFactor = 0.75f;

	public LruCache(int size) {
		this.cacheSize = size;
		int capacity = (int) Math.ceil(cacheSize / loadFactor) + 1;
		map = new LinkedHashMap<K, V>(capacity, loadFactor, true) {
			private static final long serialVersionUID = 1;
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > cacheSize;
			}
		};
	}

	public synchronized V get(K key) {
		return map.get(key);
	}

	public synchronized void put(K key, V value) {
		map.put(key, value);
	}

	public synchronized void clear() {
		map.clear();
	}

}
