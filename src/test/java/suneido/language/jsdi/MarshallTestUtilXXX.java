package suneido.language.jsdi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Utility methods for testing marshalling.
 *
 * @author Victor Schappert
 * @since 20140719
 */
public class MarshallTestUtilXXX { // TODO: Rename to MarshallTestUtil --- has
									// this name only due to Eclipse/Mercurial
									// fail

	public static class Recursive_StringSum {
		public byte a1;
		public byte b1;
		public short c1;
		public int d1;
		public byte a2;
		public byte b2;
		public short c2;
		public int d2;
		public String str;
		public Buffer buffer;

		public Recursive_StringSum(String str, Buffer buffer, int... x) {
			this.str = str;
			this.buffer = buffer;
			switch (x.length) {
			default:
				this.d2 = (int) x[7];
			case 7:
				this.c2 = (short) x[6];
			case 6:
				this.b2 = (byte) x[5];
			case 5:
				this.a2 = (byte) x[4];
			case 4:
				this.d1 = (int) x[3];
			case 3:
				this.c1 = (short) x[2];
			case 2:
				this.b1 = (byte) x[1];
			case 1:
				this.a1 = (byte) x[0];
			case 0:
				break;
			}
		}
	}

	public static MarshallPlan makeNamedPlan(
			Class<? extends MarshallTestUtilXXX> clazz, String planName,
			int... params) {
		final Method method = findMethod(clazz, planName);
		try {
			if (0 == method.getParameterCount() && 0 == params.length) {
				return (MarshallPlan) method.invoke(null);
			} else if (1 == method.getParameterCount()
					&& int.class.equals(method.getParameterTypes()[0])
					&& 1 == params.length) {
				return (MarshallPlan) method.invoke(null, params[0]);
			} else if (2 == method.getParameterCount()
					&& int.class.equals(method.getParameterTypes()[0])
					&& 1 <= params.length) {
				return (MarshallPlan) method.invoke(null, params[1],
						Arrays.copyOfRange(params, 1, params.length));
			} else {
				return (MarshallPlan) method.invoke(null, params);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("can't invoke plan-making method: "
					+ planName, e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("can't invoke plan-making method: "
					+ planName, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("can't invoke plan-making method: "
					+ planName, e);
		}
	}

	private static Method findMethod(
			Class<? extends MarshallTestUtilXXX> clazz, String methodName) {
		Method method = find1Method(clazz, methodName);
		if (null != method)
			return method;
		method = find1Method(clazz, methodName, int.class);
		if (null != method)
			return method;
		method = find1Method(clazz, methodName, int[].class);
		if (null != method)
			return method;
		method = find1Method(clazz, methodName, int.class, int[].class);
		if (null != method)
			return method;
		throw new RuntimeException("no such plan-making method: " + methodName
				+ " in " + clazz.getName());
	}

	private static Method find1Method(
			Class<? extends MarshallTestUtilXXX> clazz, String methodName,
			Class<?>... parameterTypes) {
		try {
			return clazz.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
