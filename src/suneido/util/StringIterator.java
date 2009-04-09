package suneido.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class StringIterator implements Iterator<Character> {
	private final String s;
	private int i = 0;

	public StringIterator(String s) {
		this.s = s;
	}

	public boolean hasNext() {
		return i < s.length();
	}

	public Character next() {
		if (i >= s.length())
			throw new NoSuchElementException();
		return s.charAt(i++);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
