package suneido.language;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

/**
 * Used to implement user defined methods for builtin classes e.g. Numbers,
 * Strings, Objects
 */
@ThreadSafe
public class UserDefined {

	public static Object userDefined(String where,
			Object self, String method, Object[] args) {
		SuCallable f = userDefinedMethod(where, method);
		if (f == null)
			throw SuException.methodNotFound(self, method);
		return f.eval(self, args);
	}

	public static SuCallable userDefinedMethod(String where, String method) {
		Object c = Globals.tryget(where);
		if (c instanceof SuClass) {
			Object f = ((SuClass) c).get2(method);
			return f instanceof SuCallable ? (SuCallable) f : null;
		}
		return null;
	}

}
