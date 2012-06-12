package suneido.util;

import java.util.AbstractList;
import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.Lists;

@NotThreadSafe
public class ArraysList<E> extends AbstractList<E> {
	final static int CHUNK_SIZE = 100;
	private final ArrayList<E[]> chunks = Lists.newArrayList();
	private int size = 0;

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(E value) {
		if (chunkFor(size) >= chunks.size())
			chunks.add((E[]) new Object[CHUNK_SIZE]);
		chunks.get(chunkFor(size))[indexFor(size)] = value;
		++size;
		++modCount;
		return true;
	}

	@Override
	public void clear() {
		chunks.clear();
		size = 0;
	}

	@Override
	public E get(int i) {
		return chunks.get(chunkFor(i))[indexFor(i)];
	}

	@Override
	public E set(int i, E value) {
		E[] chunk = chunks.get(chunkFor(i));
		E prev = chunk[indexFor(i)];
		chunk[indexFor(i)] = value;
		return prev;
	}

	@Override
	public int size() {
		return size;
	}

	private int chunkFor(int i) {
		return i / CHUNK_SIZE;
	}

	private int indexFor(int i) {
		return i % CHUNK_SIZE;
	}

}
