/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import com.google.common.base.Joiner;

import suneido.boot.Bootstrap;

public class Jvm {

	public static boolean runWithNewJvm(String cmd) {
		return 0 == Bootstrap.runSuneidoInNewJVM(new String[] { cmd }, false,
				null, null);
	}

	public static String runWithNewJvmCmd(String cmd) {
		String[] args = Bootstrap.runSuneidoInNewJVMArgs(new String[] { cmd }, false);
		return Joiner.on(" ").join(args);
	}

//	public static void main(String[] args) {
//		System.out.println("success? " + runWithNewJvm("-load:fred"));
//	}

}
