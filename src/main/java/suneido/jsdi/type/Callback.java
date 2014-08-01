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
public final class Callback extends ComplexType {

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
	public MarshallPlan getMarshallPlan() { // Called by ThunkManagers
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
	 * @return The return value of {@code callable}, which must be coerceable to
	 *         an {@code long}
	 * @since 20130806
	 * @see #invokeVariableIndirect(SuValue, byte[], long[])
	 */
	public long invoke(SuValue boundValue, long[] argsIn) {
		final Marshaller marshaller = getMarshallPlan().makeUnMarshaller(argsIn);
		final Object[] argsOut = typeList.marshallOutParams(marshaller);
		final Object result = boundValue.call(argsOut);
		return toLong(result);
	}

	// TODO: Add invoke0(SuValue) ... invoke4(SuValue, long a, ..., long d) for
	// faster performance -- won't implement on X86, so should throw
	// InternalError instead of being abstract.

	/**
	 * Invoked from native side.
	 * 
	 * @param boundValue
	 *            Bound value to invoke
	 * @param argsIn
	 *            Argument array to unmarshall
	 * @param viArray
	 *            Variable indirect component of arguments
	 * @return The return value of {@code callable}, which must be coerceable to
	 *         an {@code int}
	 * @since 20130806
	 * @see #invoke(SuValue, byte[])
	 */
	public long invokeVariableIndirect(SuValue boundValue, long[] argsIn,
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

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return "callback" + typeList.toParamsTypeString();
	}

	@Override
	public int getSizeDirect() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.putPointerSizedInt(0L);
		} else if (value instanceof SuValue) {
			long thunkFuncAddr = thunkManager.lookupOrCreateBoundThunk(
					(SuValue) value, this);
			marshaller.putPointerSizedInt(NumberConversions
					.toPointer64(thunkFuncAddr));
		} else
			try {
				marshaller.putPointerSizedInt(NumberConversions
						.toPointer64(value));
			} catch (SuException e) {
				throw new JSDIException("can't marshall " + value + " into "
						+ toString());
			}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		skipMarshalling(marshaller);
		return oldValue;
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		marshaller.skipBasicArrayElements(1);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
