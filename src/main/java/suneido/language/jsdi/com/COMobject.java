/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.com;

import static suneido.util.Util.array;

import java.util.HashMap;
import java.util.Map;

import suneido.SuValue;
import suneido.language.*;
import suneido.language.jsdi.DllInterface;
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
	private final long ptr;
	private final Map<String, Integer> name2dispid;
	private boolean isReleased;

	//
	// STATICS
	//

	private static final String UNKNOWN_PROGID = "???";
	private static final String TYPENAME = "COMobject";

	//
	// CONSTRUCTORS
	//

	COMobject(String progid, long ptr, boolean isDispatch) {
		if (null == progid) {
			progid = UNKNOWN_PROGID;
		}
		if (NumberConversions.nullPointer64() == ptr) {
			throw new IllegalArgumentException(
					"COM interface pointer cannot be NULL");
		}
		this.progid = progid;
		this.ptr = ptr;
		this.name2dispid = isDispatch ? new HashMap<String, Integer>() : null;
		this.isReleased = false;
	}

	//
	// INTERNALS
	//

	private void infoString(StringBuilder sb) {
		if (isDispatch()) {
			sb.append("IDispatch 0x");
			sb.append(Long.toHexString(ptr));
		} else {
			sb.append("IUnknown 0x");
			sb.append(Long.toHexString(ptr));
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
			throw new COMException(infoString("already released"));
		}
	}

	private void requireIDispatch() {
		if (! isDispatch()) {
			throw new COMException(infoString("doesn't support IDispatch"));
		}
	}

	private synchronized void release() {
		verifyNotReleased();
		if (isDispatch()) {
			release(ptr, 0L);
		} else {
			release(0L, ptr);
		}
		isReleased = true;
	}

	private boolean isDispatch() {
		return null != name2dispid;
	}

	private void putDispId(String name, Integer dispid) {
		name2dispid.put(name, dispid);
	}

	private void putDispId(String name, int[] dispid) {
		name2dispid.put(name, dispid[0]);
	}

	private synchronized Object invokeMethod(String name, Object ... args) {
		verifyNotReleased();
		requireIDispatch();
		Object result = null;
		Integer dispid = name2dispid.get(name);
		try {
			if (null == dispid) {
				int[] $dispid = new int[1];
				result = callMethodByName(ptr, name, args, $dispid);
				putDispId(name, $dispid);
			} else {
				result = callMethodByDispId(ptr, dispid, args);
			}
		} catch (COMException e) {
			throw rethrowWithInfo(e, name + "()"); 
		}
		assert null != result : "COM method invocation must return something";
		return result;
	}

	private COMException rethrowWithInfo(COMException e, String memberName) {
		String message = progid + "." + memberName + ": " + e.getMessage();
		return new COMException(message, e);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String typeName() {
		return TYPENAME;
	}

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(COMobject.class);

	@Override
	public synchronized Object get(Object member) {
		verifyNotReleased();
		requireIDispatch();
		Object result = null;
		String name = member.toString();
		Integer dispid = name2dispid.get(name);
		try {
			if (null == dispid) {
				int[] $dispid = new int[1];
				result = getPropertyByName(ptr, name, $dispid);
				putDispId(name, $dispid);
			} else {
				result = getPropertyByDispId(ptr, dispid);
			}
		} catch (COMException e) {
			throw rethrowWithInfo(e, name);
		}
		assert null != result : "COM property get must return something";
		return result;
	}

	@Override
	public synchronized void put(Object member, Object value) {
		verifyNotReleased();
		requireIDispatch();
		String name = member.toString();
		if (null == value) {
			throw new NullPointerException("no value for member " + name);
		}
		Integer dispid = name2dispid.get(name);
		try {
			if (null == dispid) {
				dispid = putPropertyByName(ptr, name, value);
				putDispId(name, dispid);
			} else {
				putPropertyByDispId(ptr, dispid, value);
			}
		} catch (COMException e) {
			throw rethrowWithInfo(e, name);
		}
	}

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		if (null == result) {
			result = new IDispatchMethod(method);
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
	 * @return Zero (to be compatible with cSuneido)
	 * @see suneido.language.BuiltinMethods
	 * @see #DispatchQ(Object)
	 */
	public static Object Release(Object self) {
		COMobject comobject = (COMobject)self;
		comobject.release();
		return 0; // TODO: This should be a constant from Numbers...
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
		COMobject comobject = (COMobject)self;
		comobject.verifyNotReleased();
		return comobject.isDispatch();
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
			// Parameters for constructing the COMobject
			String progid = null;
			long ptrToIDispatch = NumberConversions.nullPointer64();
			long ptrToIUnknown = NumberConversions.nullPointer64();
			// If the parameter is a string, it indicates to try to do a
			// CoCreateInstance based on the progid and fetch an IDispatch
			// interface on the created object, or failing that an IUnknown.
			if (x instanceof CharSequence) {
				progid = x.toString();
				long[] ptrPair = new long[2];
				if (coCreateFromProgId(progid, ptrPair)) {
					ptrToIDispatch = ptrPair[0];
					ptrToIUnknown = ptrPair[1];
				} else {
					return Boolean.FALSE;
				}
			// Otherwise, if the parameter is a number, it is an IUnknown
			// pointer which should be wrapped.
			} else {
				ptrToIUnknown = NumberConversions.toPointer64(x);
				if (NumberConversions.nullPointer64() == ptrToIUnknown) {
					return Boolean.FALSE;
				} else {
					String[] progid$ = new String[1];
					ptrToIDispatch = queryIDispatchAndProgId(ptrToIUnknown,
							progid$);
					progid = progid$[0];
				}
			}
			// Now that we have assembled the parameters, construct the
			// COMobject.
			COMobject result = null;
			if (NumberConversions.nullPointer64() != ptrToIDispatch) {
				result = new COMobject(progid, ptrToIDispatch, true);
				if (NumberConversions.nullPointer64() != ptrToIUnknown) {
					release(NumberConversions.nullPointer64(), ptrToIUnknown);
				}
			} else {
				result = new COMobject(progid, ptrToIUnknown, false);
			}
			return result;
		}
	};

	//
	// INTERNAL TYPES
	//

	private static final class IDispatchMethod extends SuValue {

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
		public Object eval(Object self, Object... args) {
			return ((COMobject)self).invokeMethod(name, args);
		}
	}

	//
	// NATIVE METHODS
	//

	private static native long queryIDispatchAndProgId(long ptrToIUnknown,
			String[] progid);

	private static native boolean coCreateFromProgId(String progid,
			long[] ptrPair);

	private static native void release(long ptrToIDispatch, long ptrToIUnknown);

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
