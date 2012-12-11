/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

/**
 * A string builder that builds comma separated lists.
 */
public class CommaStringBuilder {
	private final StringBuilder sb = new StringBuilder();

	public CommaStringBuilder add(String s) {
		sb.append(",").append(s);
		return this;
	}

	public String build() {
		return sb.length() == 0 ? "" : sb.substring(1);
	}

}
