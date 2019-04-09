/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.Date;

public class Intern {

	public static void main(String[] args) {
		Date start = new Date();

		for (int i = 0; i < 30000000; ++i)
			"Next".intern();

		Date end = new Date();

		System.out.println(end.getTime() - start.getTime());
	}

}
