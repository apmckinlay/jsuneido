/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Print {

	public static void timestamped(String s) {
		System.out.println(
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +
				" " + s);
	}

}
