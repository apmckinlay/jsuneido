package suneido.util;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class LruCache<K,V> {
	private final LinkedHashMap<K, V> map;
	private final int cacheSize;
	private static final float loadFactor = 0.75f;

	@SuppressWarnings("serial")
	public LruCache(int size) {
		this.cacheSize = size;
		int capacity = (int) Math.ceil(cacheSize / loadFactor) + 1;
		map = new LinkedHashMap<K, V>(capacity, loadFactor, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > cacheSize;
			}
		};
	}

	synchronized public V get(K key) {
		return map.get(key);
	}

	synchronized public void put(K key, V value) {
		map.put(key, value);
	}

	synchronized public void clear() {
		map.clear();
	}

}
