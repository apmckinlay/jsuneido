/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.UserDefined.userDefined;
import suneido.SuException;
import suneido.language.builtin.*;

import com.google.common.collect.ImmutableMap;

/**
 * The base class for classes that define methods for
 * Java types such as strings, integers, dates, etc.
 * @see DateMethods
 * @see NumberMethods
 * @see StringMethods
 */
public abstract class PrimitiveMethods extends SuClass {
	private final String name;

	protected PrimitiveMethods(String name, Object members) {
		super(name, null, members);
		this.name = name + "s";
	}

	/** get methods through reflection */
	protected PrimitiveMethods(String name, Class<?> c) {
		this(name, members(c));
	}
	private static Object members(Class<?> c) {
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
		return s;
	}

	@Override
	protected void linkMethods() {
	}

	@Override
	protected Object notFound(Object self, String method, Object... args) {
		return userDefined(name, self, method, args);
	}

}
