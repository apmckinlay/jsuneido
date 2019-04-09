/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

import suneido.util.Dnum;

public class SystemMemory {

	public static Object SystemMemory() {
		OperatingSystemMXBean bean =
				(OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		return Dnum.from(bean.getTotalPhysicalMemorySize());
	}

}
