/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.UUID;

public class UuidString {

	public static String UuidString() {
		return UUID.randomUUID().toString();
	}

}