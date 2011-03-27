/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.UserDefined.userDefinedMethod;

import java.util.Collections;
import java.util.Map;

import suneido.*;
import suneido.language.builtin.*;

import com.google.common.collect.ImmutableMap;

/**
 * Uses reflection to get SuMethod's from a class
 * and provides lookup for those methods.
 * Also handles user defined methods e.g. Numbers, Strings
 * Used for methods for Java types e.g. {@link NumberMethods}, {@link StringMethods}
 * and for separate methods e.g. {@link ContainerMethods} for {@link SuContainer}
 * Is the base class for {@link BuiltinClass}
 */
public class BuiltinMethods extends SuValue {
	private final String userDefined;
	private final Map<String, SuMethod> methods;

	// TODO use a factory method instead of doing methods() in constructor

	public BuiltinMethods() {
		userDefined = null;
		methods = Collections.emptyMap();
	}
	public BuiltinMethods(Class<?> c) {
		this(c, null);
	}
	public BuiltinMethods(Class<?> c, String userDefined) {
		this.userDefined = userDefined;
		this.methods = methods(c);
	}

	/** get methods through reflection */
	private static Map<String, SuMethod>  methods(Class<?> c) {
		ImmutableMap.Builder<String, SuMethod> b = ImmutableMap.builder();
		for (Class<?> m : c.getDeclaredClasses())
			if (SuMethod.class.isAssignableFrom(m))
				try {
					b.put(methodName(m), (SuMethod) m.newInstance());
				} catch (Exception e) {
					throw new SuException("error during initialization", e);
				}
		return b.build();
	}
	private static String methodName(Class<?> c) {
		String s = c.getSimpleName();
		if (s.endsWith("Q"))
			s = s.substring(0, s.length() - 1) + "?";
		if (s.endsWith("E"))
			s = s.substring(0, s.length() - 1) + "!";
		return s;
	}

	@Override
	public SuValue lookup(String method) {
		SuValue m = getMethod(method);
		if (m != null)
			return m;
		return new NotFound(method);
	}

	/** @return method or null */
	public SuValue getMethod(String method) {
		SuValue m = methods.get(method);
		if (m != null)
			return m;
		if (userDefined != null)
			return userDefinedMethod(userDefined, method);
		return null;
	}

}
