/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.Marshaller;
import suneido.jsdi.NumberConversions;
import suneido.jsdi.PrimitiveSize;
import suneido.jsdi.ThunkManager;

/**
 * Implements the JSDI {@code callback} type.
 * 
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class Callback extends ComplexType {

	//
	// DATA
	//

	private final ThunkManager thunkManager;

	//
	// CONSTRUCTORS
	//

	protected Callback(String valueName, TypeList parameters,
			ThunkManager thunkManager) {
		super(TypeId.CALLBACK, valueName, parameters);
		if (null == thunkManager) {
			throw new SuInternalError("thunkManager cannot be null");
		}
		this.thunkManager = thunkManager;
	}

	//
	// INTERNALS
	//

	protected static long toLong(Object result) {
		if (null == result) {
			// For consistency with CSuneido, a 'null' return value (failure to
			// return any value, as in 'function() { }' or
			// 'function() { return }') should send back 0 to the invoking DLL.
			return 0;
		}
		return NumberConversions.toLong(result);
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
	 * @see #invokeVariableIndirect(SuValue, byte[], Object[])
	 */
	public abstract long invoke(SuValue boundValue, Object argsIn);

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
	public abstract long invokeVariableIndirect(SuValue boundValue,
			Object argsIn, Object[] viArray);

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public final String getDisplayName() {
		return "callback" + typeList.toParamsTypeString();
	}

	@Override
	public final int getSizeDirect() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public final void marshallIn(Marshaller marshaller, Object value) {
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
	public final Object marshallOut(Marshaller marshaller, Object oldValue) {
		marshaller.skipBasicArrayElements(1);  // Nothing to be done here
		return oldValue;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
