package suneido.language.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import suneido.language.*;

public class Type extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		Object x = args[0];
		if (x instanceof SuClass)
			return "Class";
		String cn = x.getClass().getName();
		String type = translate.get(cn);
		if (type != null)
			return type;
		if (x instanceof SuMethod) {
			SuMethod m = (SuMethod) x;
			if (nestedFuncRx.matcher(m.method).find())
				return "Function";
		}
		if (cn.startsWith("suneido.language.Su"))
			return cn.substring(19);
		return cn;
	}
	static final Pattern nestedFuncRx = Pattern.compile("_f[0-9]+$");

	static Map<String, String> translate = new HashMap<String, String>();
	static {
		translate.put("java.lang.String", "String");
		translate.put("java.lang.Integer", "Number");
		translate.put("java.math.BigDecimal", "Number");
		translate.put("suneido.SuContainer", "Object");
		translate.put("suneido.SuRecord", "Record");
	}

}
