/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
	private final LoadingCache<String, Integer> nameToSlot =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Integer>() {
						@Override
						public Integer load(String name) {
							int slot = names.add(name);
							values.add(null);
							nameToSlot.put(name, slot);
							return slot;
						}});
	private final GrowableArray<String> names = new GrowableArray<>();
	private final GrowableArray<Object> values = new GrowableArray<>();

	/** VCS 20130703 -- I made this protected so we can derive a trivial context
	 *                  for testing purposes...
	 */
	protected Context(Contexts contexts) {
		this.contexts = contexts;
		contexts.addContext(this);
		// don't use slot 0
		names.add(null);
		values.add(null);
	}

	/** @return The slot for a name, assigning a new slot for a new name */
	public int slotForName(String name) {
		return nameToSlot.getUnchecked(name);
	}

	public Object get(String name) {
		return get(slotForName(name));
	}

	public Object tryget(String name) {
		return tryget(slotForName(name));
	}

	Object get(int slot) {
		Object value = tryget(slot);
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		return value;
	}

	/** Get the value for a slot. If no cached value, then do lookup
	 *  VCS 20130702 -- I made this public because it is needed by the jsdi
	 *                  package...
	 */
	public Object tryget(int slot) {
		Object value = values.get(slot);
		if (value == null) {
			String name = nameForSlot(slot);
			value = name.contains("@") ? contexts.fetchExplicit(name) : fetch(name);
		}
		if (value != null)
			values.set(slot, value);
		return value;
	}

	/**
	 * VCS 20130702 -- I made this public because it is needed by the jsdi
	 *                 package.
	 */
	public String nameForSlot(int slot) {
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
