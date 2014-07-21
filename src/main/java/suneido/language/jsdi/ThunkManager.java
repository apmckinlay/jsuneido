/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.language.Params;
import suneido.language.jsdi.type.Callback;

/**
 * Maps a set of {@link SuValue} objects to the Java-side {@link Callback}
 * object and native-side thunk, if any, to which they are bound. 
 * 
 * @author Victor Schappert
 * @since 20130806
 */
@DllInterface
@Allocates
@ThreadSafe
public abstract class ThunkManager {

	//
	// STATIC MEMBERS
	//

	public static final int THUNK_FUNC_ADDR_INDEX = 0;
	public static final int THUNK_OBJECT_ADDR_INDEX = 1;

	//
	// TYPES
	//

	private static class BoundThunk implements Comparable<BoundThunk> {
		public final SuValue boundValue;
		public final Callback callback;
		public final long thunkFuncAddr;
		public final long thunkObjectAddr;
		public final Date createTime;

		public BoundThunk(SuValue boundValue, Callback callback,
				long thunkFuncAddr, long thunkObjectAddr) {
			assert null != boundValue && null != callback;
			if (0 == thunkFuncAddr) {
				throw new SuInternalError(
						"native address of thunk function cannot be 0");
			}
			if (0 == thunkObjectAddr) {
				throw new SuInternalError(
						"native address of thunk object cannot be 0");
			}
			this.boundValue = boundValue;
			this.callback = callback;
			this.thunkFuncAddr = thunkFuncAddr;
			this.thunkObjectAddr = thunkObjectAddr;
			this.createTime = new Date();
		}

		//
		// INTERFACE: Comparable<BoundThunk>
		//

		public int compareTo(BoundThunk other)
		{ return createTime.compareTo(other.createTime); }

		//
		// ANCESTOR CLASS: Object
		//

		@Override
		public String toString()
		{
			return new StringBuilder(128).append(boundValue.toString())
					.append(" + ").append(callback.toString())
					.append(" => (0x").append(Long.toHexString(thunkFuncAddr))
					.append(", 0x").append(Long.toHexString(thunkObjectAddr))
					.append(')').toString();
		}
	}

	//
	// DATA
	//

	@SuppressWarnings("unused")
	private final JSDI jsdi;
	private final HashMap<SuValue, BoundThunk> boundValueMap;

	//
	// CONSTRUCTORS
	//

	protected ThunkManager(JSDI jsdi) {
		this.jsdi = jsdi;
		this.boundValueMap = new HashMap<>();
	}

	//
	// ACCESSORS
	//

	/**
	 * If {@code boundValue} is already bound to an existing thunk, returns the
	 * native-side function address of that thunk. If {@code boundValue} is not
	 * yet bound, binds it to a thunk and returns the native-side function
	 * address of the newly-created thunk.
	 * @param boundValue SuValue to bind
	 * @param callback Callback instance which knows how to unmarshall the
	 *                 arguments sent from the native side and pass them on to
	 *                 {@code boundValue}
	 * @return Native side function address
	 * @throws JSDIException If {@code boundValue} has already been bound to a
	 *         {@link Callback} that is not reference-equal to {@code callback}
	 */
	public long lookupOrCreateBoundThunk(
		SuValue boundValue,
		Callback callback
	)
	{
		if (null == boundValue) {
			throw new SuInternalError("boundValue cannot be null");
		}
		if (null == callback) {
			throw new SuInternalError("callback cannot be null");
		}
		final BoundThunk boundThunk = lookupOrCreateInternal(boundValue, callback);
		if (boundThunk.callback != callback) {
			// Don't permit the same SuValue instance to be bound to multiple
			// different instances of Callback (or indeed multiple different
			// thunks). This limitation isn't forced by technical requirements,
			// but rather by the historical definition of the Suneido
			// 'ClearCallback()' global function, which takes a reference to the
			// callable object to be cleared.
			StringBuilder error = new StringBuilder();
			error.append("can't bind ")
					.append(boundValue.toString())
					.append(" to multiple different callback definitions [original: ")
					.append(boundThunk.callback.toString()).append(" versus: ")
					.append(callback.toString()).append(']');
			throw new JSDIException(error.toString());
		}
		return boundThunk.thunkFuncAddr;
	}

	//
	// INTERNALS
	//

	private ArrayList<BoundThunk> thunkSnapshot() {
		ArrayList<BoundThunk> thunks = new ArrayList<>(
				boundValueMap.size());
		thunks.addAll(boundValueMap.values());
		Collections.sort(thunks);
		return thunks;
	}

	private synchronized SuContainer callbacks() {
		ArrayList<BoundThunk> thunks = thunkSnapshot();
		SuContainer result = new SuContainer(thunks.size());
		for (BoundThunk boundThunk : thunks) {
			result.add(boundThunk.boundValue);
		}
		return result;
	}

	private synchronized Boolean clearCallback(SuValue boundValue) {
		BoundThunk boundThunk = boundValueMap.remove(boundValue);
		if (null == boundThunk) {
			return Boolean.FALSE;
		} else {
			deleteThunk(boundThunk.thunkObjectAddr);
			return Boolean.TRUE;
		}
	}

	private synchronized BoundThunk lookupOrCreateInternal(SuValue boundValue, Callback callback) {
		BoundThunk boundThunk = boundValueMap.get(boundValue);
		if (null == boundThunk)
		{
			long[] addrs = new long[2];
			newThunk(boundValue, callback, addrs);
			boundThunk = new BoundThunk(boundValue, callback,
					addrs[THUNK_FUNC_ADDR_INDEX],
					addrs[THUNK_OBJECT_ADDR_INDEX]);

			boundValueMap.put(boundValue, boundThunk);
		}
		return boundThunk;
	}

	protected abstract void newThunk(SuValue boundValue, Callback callback,
			long[] addrs);

	protected abstract void deleteThunk(long thunkObjectAddr);

	//
	// BUILT-IN FUNCTIONS
	//

	/**
	 * Class which {@link suneido.language.BuiltinMethods} can translate into
	 * the Suneido built-in function {@code Callbacks()}.
	 * 
	 * @see ClearCallback
	 * @see suneido.language.Builtins
	 */
	public static final class Callbacks {
		public static final SuContainer Callbacks() {
			return JSDI.getInstance().getThunkManager().callbacks();
		}
	}

	/**
	 * Class which {@link suneido.language.BuiltinMethods} can translate into
	 * the Suneido built-in function {@code ClearCallback()}.
	 * 
	 * @see Callbacks
	 * @see suneido.language.Builtins
	 */
	public static final class ClearCallback {
		@Params("value")
		public static final Boolean ClearCallback(Object boundValue) {
			return boundValue instanceof SuValue ? JSDI.getInstance()
					.getThunkManager().clearCallback((SuValue) boundValue)
					: Boolean.FALSE;
		}
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(2048);
		result.append("ThunkManager[");
		synchronized(this)
		{
			ArrayList<BoundThunk> thunks = thunkSnapshot();
			for (BoundThunk boundThunk : thunks) {
				result.append("\n\t").append(boundThunk.toString());
			}
		}
		return result.append("\n]").toString();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			for (BoundThunk boundThunk : boundValueMap.values()) {
				deleteThunk(boundThunk.thunkObjectAddr);
			}
		} finally {
			super.finalize();
		}
	}
}
