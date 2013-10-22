/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.com;

import static suneido.util.Util.array;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import suneido.SuValue;
import suneido.language.*;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.JSDIValue;
import suneido.language.jsdi.NumberConversions;

/**
 * <p>
 * Implements the Suneido {@code COMobject} type which wraps a COM
 * {@code IDispatch} or {@code IUnknown} interface pointer.
 * </p>
 * @author Victor Schappert
 * @since 20130928
 */
@DllInterface
public final class COMobject extends JSDIValue {

	//
	// DATA
	//

	private final String progid;
	private final long ptrToIDispatch;
	private final long ptrToIUnknown;
	private final Map<String, Integer> name2dispid;
	private boolean isReleased;

	//
	// STATICS
	//

	private static final String UNKNOWN_PROGID = "???";

	//
	// CONSTRUCTORS
	//

	COMobject(String progid, long ptrToIDispatch, long ptrToIUnknown) {
		if (null == progid) {
			progid = UNKNOWN_PROGID;
		}
		if (0 == ptrToIDispatch && 0 == ptrToIUnknown) {
			throw new IllegalArgumentException(
					"must have at least one of IDispatch or IUnknown pointer");
		}
		this.progid = progid;
		this.ptrToIDispatch = ptrToIDispatch;
		this.ptrToIUnknown = ptrToIUnknown;
		this.name2dispid = Collections
				.synchronizedMap(new HashMap<String, Integer>());
		this.isReleased = false;
	}

	//
	// INTERNALS
	//

	private void infoString(StringBuilder sb) {
		if (0 != ptrToIDispatch) {
			sb.append("IDispatch 0x");
			sb.append(Long.toHexString(ptrToIDispatch));
		} else {
			sb.append("IUnknown 0x");
			sb.append(Long.toHexString(ptrToIUnknown));
		}
		sb.append(" \"");
		sb.append(progid);
		sb.append('"');
	}

	private String infoString(String suffix) {
		StringBuilder sb = new StringBuilder();
		sb.append("COM: ");
		infoString(sb);
		sb.append(suffix);
		return sb.toString();
	}

	private void verifyNotReleased() {
		if (isReleased) {
			throw new JSDIException(infoString("already released"));
		}
	}

	private void requireIDispatch() {
		if (0 == ptrToIDispatch) {
			throw new JSDIException(infoString("doesn't support IDispatch"));
		}
	}

	private synchronized void release() {
		verifyNotReleased();
		if (0 != ptrToIDispatch) release(ptrToIDispatch);
		if (0 != ptrToIUnknown) release(ptrToIUnknown);
		isReleased = true;
	}

	private void putDispId(String name, Integer dispid) {
		name2dispid.put(name, dispid);
	}

	private void putDispId(String name, int[] dispid) {
		name2dispid.put(name, dispid[0]);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(COMobject.class);

	@Override
	public Object get(Object member) {
		requireIDispatch();
		Object result = null;
		String name = member.toString();
		Integer dispid = name2dispid.get(name);
		if (null == dispid) {
			int[] $dispid = new int[1];
			result = getPropertyByName(ptrToIDispatch, name, $dispid);
			putDispId(name, $dispid);
		} else {
			result = getPropertyByDispId(ptrToIDispatch, dispid);
		}
		if (null == result) {
			throw new JSDIException("member " + name + " has no value");
		}
		return result;
	}

	@Override
	public void put(Object member, Object value) {
		requireIDispatch();
		String name = member.toString();
		if (null == value) {
			throw new NullPointerException("no value for member " + name);
		}
		Integer dispid = name2dispid.get(name);
		if (null == dispid) {
			dispid = putPropertyByName(ptrToIDispatch, name, value);
			putDispId(name, dispid);
		} else {
			putPropertyByDispId(ptrToIDispatch, dispid, value);
		}
	}

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		if (null == result) {
			requireIDispatch();
			// TODO: What to do here? Have a map String->method? Or just
			// instantiate a new one each time? I say the latter.
		}
		return result;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" /* ");
		infoString(sb);
		sb.append(" */");
		return sb.toString();
	}

	//
	// BUILT-IN METHODS
	//

	/**
	 * Built-in release method. <em>eg</em>: <code>comobject.Release()</code>.
	 * The requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure
	 * @see suneido.language.BuiltinMethods
	 * @see #DispatchQ(Object)
	 */
	public static void Release(Object self) {
		COMobject comobject = (COMobject)self;
		comobject.release();
	}

	/**
	 * Built-in test for IDispatch method.
	 * <em>eg</em>: <code>comobject.Dispatch?()</code>.
	 * The requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure
	 * @see suneido.language.BuiltinMethods
	 * @see #Release(Object)
	 */
	public static Boolean DispatchQ(Object self) {
		return 0L != ((COMobject)self).ptrToIDispatch;
	}

	//
	// BUILT-IN CLASS
	//

	/**
	 * Reference to a {@link BuiltinClass} that describes how to expose this
	 * class to the Suneido programmer.
	 * @see suneido.language.Builtins
	 */
	public static final SuValue clazz = new BuiltinClass() {

		private final FunctionSpec newFS = new FunctionSpec(
				array("progid-or-ptr"));

		@Override
		protected Object newInstance(Object... args) {
			args = Args.massage(newFS, args);
			Object x = args[0];
			// If the parameter is a string, it indicates to try to do a
			// CoCreateInstance based on the progid and fetch an IDispatch
			// interface on the created object, or failing that an IUnknown.
			if (x instanceof CharSequence) {
				String progid = x.toString();
				long[] ptrPair = new long[2];
				if (coCreateFromProgId(progid, ptrPair)) {
					return new COMobject(progid, ptrPair[0], ptrPair[1]);
				} else {
					return Boolean.FALSE;
				}
			// Otherwise, if the parameter is a number, it is an IUnknown
			// pointer which should be wrapped.
			} else {
				long ptrToIUnknown = NumberConversions.toPointer64(x);
				if (0 == ptrToIUnknown) {
					return Boolean.FALSE;
				} else {
					String[] progid = new String[1];
					long ptrToIDispatch = queryIDispatchAndProgId(
							ptrToIUnknown, progid);
					return new COMobject(progid[0], ptrToIDispatch,
							ptrToIUnknown);
				}
			}
		}
	};

	//
	// INTERNAL TYPES
	//

	private final class IDispatchMethod extends SuValue {

		//
		// DATA
		//

		private final String name;

		//
		// CONSTRUCTORS
		//

		public IDispatchMethod(String name) {
			this.name = name;
		}

		//
		// ANCESTOR CLASS: SuValue
		//

		@Override
		public Object call(Object... args) {
			// TODO: is this needed, or will runtime just correctly call eval?
			return eval(this, args);
		}

		@Override
		public Object eval(Object self, Object... args) {
			throw new RuntimeException("hello");
		}
	}

	//
	// NATIVE METHODS
	//

	private static native long queryIDispatchAndProgId(long ptrToIUnknown,
			String[] progid);

	private static native boolean coCreateFromProgId(String progid,
			long[] ptrPair);

	private static native void release(long ptrToInterface);

	private static native Object getPropertyByName(long ptrToIDispatch,
			String name, int[] dispid);

	private static native Object getPropertyByDispId(long ptrToIDispatch,
			int dispid);

	private static native int putPropertyByName(long ptrToIDispatch,
			String name, Object value);

	private static native void putPropertyByDispId(long ptrToIDispatch,
			int dispid, Object value);

	private static native Object callMethodByName(long ptrToIDispatch,
			String name, Object[] args, int[] dispid);

	private static native Object callMethodByDispId(long ptrToIDispatch,
			int dispid, Object[] args);
}
