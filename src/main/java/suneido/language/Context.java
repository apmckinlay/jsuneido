/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayList;
import java.util.List;

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
							return newSlot(name);
						}});
	private final List<String> names = new ArrayList<>(1000);
	private final List<Object> values = new ArrayList<>(1000);
	private static final Object nonExistent = new Object();

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

	/**
	 * Called by compile. Cache handles synchronization for existing names.
	 * @return The slot for a name, assigning a new slot for a new name.
	 */
	public final int slotForName(String name) {
		return nameToSlot.getUnchecked(name);
	}

	private synchronized int newSlot(String name) {
		assert names.size() == values.size();
		int slot = names.size();
		names.add(name);
		values.add(null);
		return slot;
	}

	public final synchronized Object get(String name) {
		return get(slotForName(name));
	}

	/** Called by compiled code to get the value of a global */
	final synchronized Object get(int slot) {
		Object value = tryget(slot);
		if (value == null)
			throw new SuException("can't find " + nameForSlot(slot));
		return value;
	}

	/** Called for rules and triggers and UserDefined */
	public synchronized final Object tryget(String name) {
		return tryget(slotForName(name));
	}

	/** Get the value for a slot. If no cached value, then do lookup
	 *  VCS 20130702 -- public because it is needed by the jsdi package
	 */
	public synchronized final Object tryget(int slot) {
		Object value = values.get(slot);
		if (value == null) {
			String name = nameForSlot(slot);
			value = name.contains("@") ? contexts.fetchExplicit(name) : fetch(name);
			values.set(slot, value == null ? nonExistent : value);
			// nonExistent is used to avoid repeating failing fetches
		}
		return value == nonExistent ? null : value;
	}

	/** VCS 20130702 - public because it is needed by the jsdi package. */
	public synchronized final String nameForSlot(int slot) {
		return names.get(slot);
	}

	/** Lookup the value for a name in this context */
	abstract protected Object fetch(String name);

	/** Remove the cached value for a slot. Called by Unload */
	public synchronized final void clear(String name) {
		values.set(slotForName(name), null);
	}

	/** Remove the cached values for all slots. Called by Use & Unuse */
	public synchronized final void clearAll() {
		for (int i = 0; i < values.size(); ++i)
			values.set(i, null);
	}

	/**
	 * Used by ContextLayered overloading.
	 * Also used by tests which is why it is public.
	 */
	public synchronized final void set(String name, Object value) {
		values.set(slotForName(name), value);
	}

}
