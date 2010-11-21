package suneido.language.builtin;

import java.lang.management.ManagementFactory;

import suneido.language.BuiltinFunction0;

import com.sun.management.OperatingSystemMXBean;

public class SystemMemory extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return getPhysicalMemory();
	}

	private static Object getPhysicalMemory() {
		OperatingSystemMXBean bean =
				(OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		return bean.getTotalPhysicalMemorySize();
	}

	public static void main(String[] args) {
		System.out.println("Total Physical Memory Size " + getPhysicalMemory());
	}

}
