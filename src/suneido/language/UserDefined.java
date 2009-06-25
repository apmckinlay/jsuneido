package suneido.language;

import static suneido.language.SuClass.Marker.METHOD;
import suneido.SuException;

public class UserDefined {

	public static SuClass userDefined(String where, String method) {
		Object x = Globals.tryget(where);
		if (x != null && x instanceof SuClass) {
			SuClass c = (SuClass) x;
			if (c.get3(method) == METHOD)
				return c;
		}
		String type = where.substring(0, where.length() - 1).toLowerCase();
		throw SuException.methodNotFound(type, method);
	}

}
