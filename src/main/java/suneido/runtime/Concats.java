/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Used by Ops.cat when concatenating a "large" string.
 * Prevents the "worst case" scenario of repeated string concatenation.
 * Keeps a StringBuilder which may be shared.
 * When possible, adds to the end of the current StringBuilder.
 * Mostly immutable - the StringBuilder is only appended to
 * so its contents up to len do not change.
 * Ideally, the result of toString would be cached with the StringBuilder
 * but it's cached in Concats instead to avoid another heap object.
 */
@ThreadSafe
public final class Concats extends String2 {
	private final StringBuilder sb;
	private final int len; // may be shorter than sb.length()
	private String s; // cache toString

	public Concats(String s, String t) {
		len = s.length() + t.length();
		sb = new StringBuilder(len);
		sb.append(s).append(t);
	}

	public Concats append(Object x) {
		CharSequence s = x instanceof CharSequence ? (CharSequence) x : Ops.toStr(x);
		if (s.length() == 0)
			return this;
		synchronized (sb) {
			if (len == sb.length()) {
				sb.append(s);
				return new Concats(sb);
			} else {
				// someone else has appended so make new StringBuilder
				StringBuilder sb2 = new StringBuilder(len + s.length());
				// use CharSequence append to avoid conversion to string
				sb2.append(sb, 0, len).append(s);
				return new Concats(sb2);
			}
		}
	}

	// no sync required since only called from inside sync
	private Concats(StringBuilder sb) {
		this.sb = sb;
		this.len = sb.length();
	}

	//
	// INTERFACE: CharSequence
	//

	@Override
	public char charAt(int index) {
		synchronized (sb) {
			return sb.charAt(index);
		}
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		synchronized (sb) {
			return sb.subSequence(start, end);
		}
	}

	@Override
	public String toString() {
		if (s == null)
			// sb may be longer than len
			synchronized (sb) {
				s = sb.substring(0, len); // cache in s
			}
		return s;
	}

}
