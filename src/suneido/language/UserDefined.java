package suneido.language;

import suneido.SuException;
import suneido.language.SuClass.Method;

/**
 * Used to implement user defined methods for builtin classes e.g. Numbers,
 * Strings, Objects
 * 
 * @author Andrew McKinlay
 */
public class UserDefined {

	public static SuClass userDefined(String where, String method) {
		SuClass c = userDefinedClass(where, method);
		if (c != null)
			return c;
		String type = where.substring(0, where.length() - 1).toLowerCase();
		throw SuException.methodNotFound(type, method);
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
