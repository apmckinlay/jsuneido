/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

/**
 * The "global" context for a function or method.
 * Names are assigned unique integer slots.
 * A context knows how to lookup the value for a name.
 * Values are cached.<p>
 * Derived classes must define fetch(name)
 *
 * @see ContextLayered
 * @see ContextModules
 */
@ThreadSafe
public abstract class Context {
	private final Contexts contexts;
	private final ConcurrentHashMap<String,Integer> nameToSlot =
			new ConcurrentHashMap<String,Integer>();
	private final GrowableArray<String> names = new GrowableArray<String>();
	private final GrowableArray<Object> values = new GrowableArray<Object>();

	Context(Contexts contexts) {
		this.contexts = contexts;
		contexts.addContext(this);
		// don't use slot 0
		names.add(null);
		values.add(null);
	}

	/** @return The slot for a name, assigning a new slot for a new name */
	int slotForName(String name) {
		Integer slot = nameToSlot.get(name);
		return (slot == null) ? newSlot(name) : slot;
	}

	synchronized private int newSlot(String name) {
		int slot = names.add(name);
		values.add(null);
		nameToSlot.put(name, slot);
		// WARNING: concurrency bug if nameToSlot change becomes visible before
		// names and values adds
		return slot;
	}

	public Object get(String name) {
		return get(slotForName(name));
	}

	public Object tryget(String name) {
		return tryget(slotForName(name));
	}

	/** Get the value for a slot. If no cached value, then do lookup */
	Object get(int slot) {
		Object value = tryget(slot);
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		return value;
	}

	private Object tryget(int slot) {
		Object value = values.get(slot);
		if (value == null) {
			String name = nameForSlot(slot);
			value = name.contains("@") ? contexts.fetchExplicit(name) : fetch(name);
		}
		if (value != null)
			values.set(slot, value);
		return value;
	}

	String nameForSlot(int slot) {
		return names.get(slot);
	}

	/** Lookup the value for a name in this context */
	abstract protected Object fetch(String name);

	/** Remove the cached value for a slot. */
	public void clear(String name) {
		clear(slotForName(name));
	}

	/** Remove the cached value for a slot. */
	void clear(int slot) {
		values.set(slot, null);
	}

	/** Remove the cached values for all slots. */
	public synchronized void clearAll() {
		values.nullFill();
	}

	/** used by overloading and tests */
	public void set(String name, Object value) {
		values.set(slotForName(name), value);
	}

	@NotThreadSafe
	private static class GrowableArray<T> {
		private Object[] data = new Object[8];
		private int size = 0; // only accessed by add & size

		/** Can be used unsynchronized if you can tolerate stale data */
		@SuppressWarnings("unchecked")
		T get(int i) {
			// NOTE: do NOT access size since it may be inconsistent
			return (T) data[i];
		}

		/** Can be used unsynchronized */
		void set(int i, T x) {
			data[i] = x;
		}

		/** Should be synchronized */
		int add(T x) {
			if (size >= data.length)
				data = Arrays.copyOf(data, (size * 3) / 2 + 1);
			data[size] = x;
			return size++;
		}

		/** Should be synchronized */
		void nullFill() {
			Arrays.fill(data, null);
		}
	}

}
