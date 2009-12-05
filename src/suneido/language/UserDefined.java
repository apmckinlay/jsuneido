package suneido.language;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.language.SuClass.Method;

/**
 * Used to implement user defined methods for builtin classes e.g. Numbers,
 * Strings, Objects
 *
 * @author Andrew McKinlay
 */
@ThreadSafe
public class UserDefined {

	public static Object userDefined(String where,
			Object self, String method, Object[] args) {
		SuClass c = userDefinedClass(where, method);
		if (c == null)
			throw SuException.methodNotFound(self, method);
		return c.invoke(self, method, args);
	}

	public static SuClass userDefinedClass(String where, String method) {
		Object x = Globals.tryget(where);
		if (x != null && x instanceof SuClass) {
			SuClass c = (SuClass) x;
			if (c.get3(method) instanceof Method)
				return c;
		}
		return null;
	}

}
