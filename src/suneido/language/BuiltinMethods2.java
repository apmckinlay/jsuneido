/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.UserDefined.userDefinedMethod;
import static suneido.util.Util.isCapitalized;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import suneido.SuException;
import suneido.SuValue;
import com.google.common.collect.ImmutableMap;

public class BuiltinMethods2 extends SuValue {
	private final Map<String, SuCallable> methods;
	private final String userDefined;

	public BuiltinMethods2(Class<?> c) {
		this.methods = methods(c);
		userDefined = null;
	}

	public BuiltinMethods2(Class<?> c, String userDefined) {
		this.methods = methods(c);
		this.userDefined = userDefined;
	}

	/** get methods through reflection */
	private static Map<String, SuCallable>  methods(Class<?> c) {
		ImmutableMap.Builder<String, SuCallable> b = ImmutableMap.builder();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		for (Method m : c.getDeclaredMethods()) {
			int mod = m.getModifiers();
			String methodName = methodName(m);
			if (Modifier.isPublic(mod) && Modifier.isStatic(mod) &&
					isCapitalized(methodName)) {
				try {
					MethodHandle mh = lookup.unreflect(m);
					Params p = m.getAnnotation(Params.class);
					int nParams = m.getParameterTypes().length;
					FunctionSpec params = (p == null) ?
							(nParams == 1) ? FunctionSpec.noParams : null
							: FunctionSpec.from(p.value());
					b.put(methodName, Builtin.method(mh, params));
				} catch (IllegalAccessException e) {
					throw new SuException("error getting method " + 
							c.getName() + " " + m.getName(), e);
				}
			}
		}
		return b.build();
	}
	private static String methodName(Method m) {
		String s = m.getName();
		if (s.endsWith("Q"))
			s = s.substring(0, s.length() - 1) + "?";
		if (s.endsWith("E"))
			s = s.substring(0, s.length() - 1) + "!";
		return s;
	}
	
	public SuCallable lookup(String method) {
		SuCallable m = getMethod(method);
		if (m != null)
			return m;
		return new NotFound(method);
	}

	/** @return method or null */
	public SuCallable getMethod(String method) {
		SuCallable m = methods.get(method);
		if (m != null)
			return m;
		if (userDefined != null)
			return userDefinedMethod(userDefined, method);
		return null;
	}

}
