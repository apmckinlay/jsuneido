/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.jsdi.DllInterface;


/**
 * Utility class to convert between Java strings and values that can be passed
 * to {@code dll} functions.
 * 
 * @author Victor Schappert
 * @since 20130718
 * @see Buffer
 */
@DllInterface
final class StringConversions {

	static byte[] stringToZeroTerminatedByteArray(String value) {
		final int N = value.length();
		byte[] b = new byte[N + 1];
		for (int k = 0; k < N; ++k) {
			b[k] = (byte)value.charAt(k);
		}
		return b;
	}
}
