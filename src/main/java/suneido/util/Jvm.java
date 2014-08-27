/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import suneido.boot.Bootstrap;

public class Jvm {

	public static boolean runWithNewJvm(String cmd) {
		return 0 == Bootstrap.runSuneidoInNewJVM(new String[] { cmd }, false,
				null);
	}

	public static void main(String[] args) {
		System.out.println("success? " + runWithNewJvm("-load:fred"));
	}

}
