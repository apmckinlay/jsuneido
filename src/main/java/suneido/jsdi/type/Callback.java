/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import java.util.Arrays;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.marshall.ElementSkipper;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.NumberConversions;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.jsdi.marshall.VariableIndirectInstruction;

/**
 * Implements the JSDI {@code callback} type.
 * 
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public class Callback extends ComplexType {

	//
	// DATA
	//

	private final ThunkManager thunkManager;
	private       MarshallPlan marshallPlan;

	//
	// CONSTRUCTORS
	//

	public Callback(String valueName, TypeList parameters,
			ThunkManager thunkManager) {
		super(TypeId.CALLBACK, valueName, parameters);
		if (null == thunkManager) {
			throw new SuInternalError("thunkManager cannot be null");
		}
		this.thunkManager = thunkManager;
		this.skipper = SKIPPER;
	}

	//
	// INTERNALS
	//

	private static final ElementSkipper SKIPPER = new ElementSkipper(1, 0);

	protected static long toLong(Object result) {
		if (null == result) {
			// For consistency with CSuneido, a 'null' return value (failure to
			// return any value, as in 'function() { }' or
			// 'function() { return }') should send back 0 to the invoking DLL.
			return 0;
		}
		return NumberConversions.toLong(result);
	}

	// TODO: document
	public final MarshallPlan getMarshallPlan() { // Called by ThunkManagers
		// TODO: resolve thread safety and update issues --
		// this will cause problems if marshall plan on an already bound
		// thunk can change
		if (bind(0) || null == marshallPlan) {
			marshallPlan = typeList
					.makeParamsMarshallPlan(true, false);
		}
		return marshallPlan;
	}

	//
	// ACCESSORS
	//

	/**
	 * Invoked from native side.
	 * 
	 * @param boundValue
	 *            Bound value to invoke
	 * @param argsIn
	 *            Argument array to unmarshall
	 * @return The return value of calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20130806
	 * @see #invokeVariableIndirect(SuValue, byte[], long[])
	 * @see #invoke0(SuValue)
	 * @see #invoke1(SuValue, long)
	 * @see #invoke2(SuValue, long, long)
	 * @see #invoke3(SuValue, long, long, long)
	 * @see #invoke4(SuValue, long, long, long, long)
	 */
	public final long invoke(SuValue boundValue, long[] argsIn) {
		final Marshaller marshaller = getMarshallPlan().makeUnMarshaller(argsIn);
		final Object[] argsOut = typeList.marshallOutParams(marshaller);
		final Object result = boundValue.call(argsOut);
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 * 
	 * @param boundValue
	 *            Bound value to invoke
	 * @param argsIn
	 *            Argument array to unmarshall
	 * @param viArray
	 *            Variable indirect component of arguments
	 * @return The return value of calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20130806
	 * @see #invoke(SuValue, byte[])
	 * @see #invoke0(SuValue)
	 * @see #invoke1(SuValue, long)
	 * @see #invoke2(SuValue, long, long)
	 * @see #invoke3(SuValue, long, long, long)
	 * @see #invoke4(SuValue, long, long, long, long)
	 */
	public final long invokeVariableIndirect(SuValue boundValue, long[] argsIn,
			Object[] viArray) {
		int[] viInstArray = new int[viArray.length];
		Arrays.fill(viInstArray,
				VariableIndirectInstruction.RETURN_JAVA_STRING.ordinal());
		Marshaller marshaller = marshallPlan.makeUnMarshaller(argsIn, viArray,
				viInstArray);
		Object[] argsOut = typeList.marshallOutParams(marshaller);
		Object result = boundValue.call(argsOut);
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 *
	 * @param boundValue
	 *            Bound value to invoke
	 * @return The return value from calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20140801
	 * @see #invoke(SuValue, long[])
	 * @see #invokeVariableIndirect(SuValue, long[], Object[])
	 */
	public static final long invoke0(SuValue boundValue) {
		Object result = boundValue.call0();
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 *
	 * @param boundValue
	 *            Bound value to invoke
	 * @param a
	 *            First argument
	 * @return The return value from calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20140801
	 * @see #invoke(SuValue, long[])
	 * @see #invokeVariableIndirect(SuValue, long[], Object[])
	 */
	public final long invoke1(SuValue boundValue, long a) {
		Object a_ = typeList.get(0).getType().marshallOutFromLong(a, null);
		Object result = boundValue.call1(a_);
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 *
	 * @param boundValue
	 *            Bound value to invoke
	 * @param a
	 *            First argument
	 * @param b
	 *            Second argument
	 * @return The return value from calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20140801
	 * @see #invoke(SuValue, long[])
	 * @see #invokeVariableIndirect(SuValue, long[], Object[])
	 */
	public final long invoke2(SuValue boundValue, long a, long b) {
		Object a_ = typeList.get(0).getType().marshallOutFromLong(a, null);
		Object b_ = typeList.get(1).getType().marshallOutFromLong(b, null);
		Object result = boundValue.call2(a_, b_);
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 *
	 * @param boundValue
	 *            Bound value to invoke
	 * @param a
	 *            First argument
	 * @param b
	 *            Second argument
	 * @param c
	 *            Third argument
	 * @return The return value from calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20140801
	 * @see #invoke(SuValue, long[])
	 * @see #invokeVariableIndirect(SuValue, long[], Object[])
	 */
	public final long invoke3(SuValue boundValue, long a, long b, long c) {
		Object a_ = typeList.get(0).getType().marshallOutFromLong(a, null);
		Object b_ = typeList.get(1).getType().marshallOutFromLong(b, null);
		Object c_ = typeList.get(2).getType().marshallOutFromLong(c, null);
		Object result = boundValue.call3(a_, b_, c_);
		return toLong(result);
	}

	/**
	 * Invoked from native side.
	 *
	 * @param boundValue
	 *            Bound value to invoke
	 * @param a
	 *            First argument
	 * @param b
	 *            Second argument
	 * @param c
	 *            Third argument
	 * @param d
	 *            Fourth argument
	 * @return The return value from calling {@code boundValue}, which must be
	 *         coerceable to a {@code long}
	 * @since 20140801
	 * @see #invoke(SuValue, long[])
	 * @see #invokeVariableIndirect(SuValue, long[], Object[])
	 */
	public final long invoke4(SuValue boundValue, long a, long b, long c, long d) {
		Object a_ = typeList.get(0).getType().marshallOutFromLong(a, null);
		Object b_ = typeList.get(1).getType().marshallOutFromLong(b, null);
		Object c_ = typeList.get(2).getType().marshallOutFromLong(c, null);
		Object d_ = typeList.get(3).getType().marshallOutFromLong(d, null);
		Object result = boundValue.call4(a_, b_, c_, d_);
		return toLong(result);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public final boolean isMarshallableToLong() {
		return true;
	}

	@Override
	public final int getSizeDirect() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public final void marshallIn(Marshaller marshaller, Object value) {
		final long addr = marshallInToLong(value);
		marshaller.putPointerSizedInt(addr);
	}

	@Override
	public final Object marshallOut(Marshaller marshaller, Object oldValue) {
		if (null != oldValue) {
			skipMarshalling(marshaller);
			return oldValue;
		} else {
			return marshaller.getPointerSizedInt();
		}
	}

	@Override
	public final long marshallInToLong(Object value) {
		if (null == value) {
			return 0L;
		} else if (value instanceof SuValue) {
			return thunkManager.lookupOrCreateBoundThunk(
					(SuValue) value, this);
		} else {
			try {
				if (8 == PrimitiveSize.POINTER) {
					return NumberConversions.toPointer64(value);
				} else if (4 == PrimitiveSize.POINTER) {
					return NumberConversions.toPointer32(value);
				}
			} catch (SuException e) {
				throw new JSDIException("can't marshall " + value + " into "
					+ toString());
			}
		}
		throw new SuInternalError("unsupported pointer size");
	}

	@Override
	public final Object marshallOutFromLong(long marshalledData, Object oldValue) {
		return null != oldValue ? oldValue : marshalledData; 
	}

	@Override
	public final void skipMarshalling(Marshaller marshaller) {
		marshaller.skipBasicArrayElements(1);
	}

	@Override
	public final String getDisplayName() {
		return "callback" + typeList.toParamsTypeString();
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
