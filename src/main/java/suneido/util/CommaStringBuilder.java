/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

/**
 * A string builder that builds comma separated lists.
 * Adds a comma prior to each add, except for the first.
 */
public class CommaStringBuilder implements Appendable {
	private final StringBuilder sb;
	private boolean first = true;

	public CommaStringBuilder() {
		sb = new StringBuilder();
	}

	public CommaStringBuilder(String s) {
		sb = new StringBuilder(s);
	}

	public CommaStringBuilder(StringBuilder sb) {
		this.sb = sb;
	}

	public CommaStringBuilder add(String s) {
		if (first)
			first = false;
		else
			sb.append(",");
		sb.append(s);
		return this;
	}

	public CommaStringBuilder add(Object x) {
		return add(x.toString());
	}

	public CommaStringBuilder add(long i) {
		if (first)
			first = false;
		else
			sb.append(",");
		sb.append(i);
		return this;
	}

	@Override
	public CommaStringBuilder append(CharSequence s) {
		sb.append(s);
		return this;
	}

	@Override
	public CommaStringBuilder append(CharSequence csq, int start, int end) {
		sb.append(csq, start, end);
		return this;
	}

	@Override
	public CommaStringBuilder append(char c) {
		sb.append(c);
		return this;
	}

	public CommaStringBuilder append(Object x) {
		sb.append(x);
		return this;
	}

	public CommaStringBuilder append(long i) {
		sb.append(i);
		return this;
	}

	public void clear() {
		sb.setLength(0);
		first = true;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

}
