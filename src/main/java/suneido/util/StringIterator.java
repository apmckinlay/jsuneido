package suneido.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class StringIterator implements Iterator<String> {
	private final String s;
	private int i = 0;

	public StringIterator(String s) {
		this.s = s;
	}

	public boolean hasNext() {
		return i < s.length();
	}

	public String next() {
		if (i >= s.length())
			throw new NoSuchElementException();
		++i;
		return s.substring(i - 1, i);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
