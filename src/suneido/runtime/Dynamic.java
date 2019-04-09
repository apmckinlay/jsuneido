/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import suneido.SuException;

/** runtime support for dynamic _variables */
public class Dynamic {
	private final static ThreadLocal<Deque<Map<String,Object>>> stack =
			ThreadLocal.withInitial(ArrayDeque::new);

	public static void put(String name, Object value) {
		stack.get().peek().put(name, value);
	}

	public static Object get(String name) {
		Object value = getOrNull(name);
		if (value == null)
			throw new SuException("uninitialized " + name);
		return value;
	}

	static Object getOrNull(String name) {
		for (Map<String,Object> map : stack.get()) {
			Object value = map.get(name);
			if (value != null)
				return value;
		}
		return null;
	}

	/** called at the start of functions that set dynamic variables */
	public static void push() {
		stack.get().push(new HashMap<String,Object>());
	}

	/** called at the end of functions that set dynamic variables */
	public static void pop() {
		stack.get().pop();
	}

}
