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
import java.util.Collections;
import java.util.Map;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.language.builtin.ContainerMethods;
import suneido.language.builtin.NumberMethods;
import suneido.language.builtin.StringMethods;

import com.google.common.collect.ImmutableMap;

// MAYBE add support for @Aka("...") e.g. string.StartsWith and Prefix?

// MAYBE add a way to have params but still be MethodN e.g. SuQuery

/**
 * Uses reflection to get methods from a class
 * - the methods must be public, static, capitalized, and return Object.
 * {@link FunctionSpec} is provided by an @Param(string) annotation.
 * The annotation is not required if there are no arguments.
 * Provides lookup for those methods.
 * Uses {@link Builtin} to wrap MethodHandle's into an {@link SuCallable}.
 * Also handles user defined methods e.g. Numbers, Strings
 * Used for methods for Java types e.g. {@link NumberMethods}, {@link StringMethods}
 * and for separate methods e.g. {@link ContainerMethods} for {@link SuContainer}
 * Is the base class for {@link BuiltinClass}
 */
public class BuiltinMethods extends SuValue {
	private final Map<String, SuCallable> methods;
	private final String userDefined;

	public BuiltinMethods() {
		methods = Collections.emptyMap();
		userDefined = null;
	}

	public BuiltinMethods(Class<?> c) {
		this(c, null);
	}

	public BuiltinMethods(Class<?> c, String userDefined) {
		this.methods = methods(c);
		this.userDefined = userDefined;
	}

	/** get methods through reflection
	 * 
	 * NOTE (VCS ==> APM): 20130628...I changed the visibility on this method so
	 *                     that it can be called from JSDI packages (e.g.
	 *                     struct).
	 */
	public static Map<String, SuCallable>  methods(Class<?> c) {
		ImmutableMap.Builder<String, SuCallable> b = ImmutableMap.builder();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		for (Method m : c.getDeclaredMethods()) {
			int mod = m.getModifiers();
			String methodName = methodName(m);
			if (Modifier.isPublic(mod) && Modifier.isStatic(mod) &&
					isCapitalized(methodName)) {
				try {
					MethodHandle mh = lookup.unreflect(m);
					b.put(methodName, Builtin.method(mh, params(m, 1)));
				} catch (IllegalAccessException e) {
					throw new SuException("error getting method " +
							c.getName() + " " + m.getName(), e);
				}
			}
		}
		return b.build();
	}

	static Map<String, SuCallable> functions(Class<?> c) {
		ImmutableMap.Builder<String, SuCallable> b = ImmutableMap.builder();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		for (Method m : c.getDeclaredMethods()) {
			int mod = m.getModifiers();
			String name = methodName(m);
			if (Modifier.isPublic(mod) && Modifier.isStatic(mod) &&
					isCapitalized(name)) {
				try {
					MethodHandle mh = lookup.unreflect(m);
					b.put(name, Builtin.function(mh, params(m, 0)));
				} catch (IllegalAccessException e) {
					throw new SuException("error getting function " +
							c.getName() + " " + m.getName(), e);
				}
			}
		}
		return b.build();
	}

	private static FunctionSpec params(Method m, int nExtra) {
		Params p = m.getAnnotation(Params.class);
		int nParams = m.getParameterTypes().length;
		FunctionSpec params = (p == null)
				? (nParams == nExtra) ? FunctionSpec.noParams : null
				: FunctionSpec.from(p.value());
		return params;
	}

	private static String methodName(Method m) {
		String s = m.getName();
		if (s.endsWith("Q"))
			s = s.substring(0, s.length() - 1) + "?";
		if (s.endsWith("E"))
			s = s.substring(0, s.length() - 1) + "!";
		return s;
	}

	@Override
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
