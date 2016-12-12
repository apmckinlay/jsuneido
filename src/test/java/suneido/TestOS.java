/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

public class TestOS {
	public static void main(String[] args) {
		System.out.println("running on " +
				System.getProperty("os.name") + " - " +
				System.getProperty("os.version"));
	}
}
