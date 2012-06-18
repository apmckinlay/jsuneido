/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Print {

	/** System.out.print a time stamp */
	public static void timestamp() {
		System.out.print(
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
				+ " ");
	}

	public static void timestamped(String s) {
		timestamp();
		System.out.println(s);
	}

}
