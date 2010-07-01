package suneido.language.builtin;

import java.lang.management.ManagementFactory;

import suneido.language.*;

import com.sun.management.OperatingSystemMXBean;

public class SystemMemory extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
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
