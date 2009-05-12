package suneido.language.builtin;

import static suneido.language.SuClass.Marker.METHOD;
import suneido.SuException;
import suneido.language.Globals;
import suneido.language.SuClass;

public class UserDefined {

	public static SuClass userDefined(String where, String method) {
		Object x = Globals.get(where);
		if (x != null && x instanceof SuClass) {
			SuClass c = (SuClass) x;
			if (c.get3(method) == METHOD)
				return c;
		}
		String type = where.substring(0, where.length() - 1).toLowerCase();
		throw new SuException("unknown method: " + type + "." + method);
	}

}
