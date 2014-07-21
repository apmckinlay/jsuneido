/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.NumberConversions;
import suneido.language.jsdi.PrimitiveSize;
import suneido.language.jsdi.ThunkManager;
import suneido.language.jsdi._64BitIssue;

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

	@_64BitIssue
	// see FIXME note below
	protected static int toInt(Object result) {
		if (null == result) {
			// For consistency with CSuneido, a 'null' return value (failure to
			// return any value, as in 'function() { }' or
			// 'function() { return }') should send back 0 to the invoking DLL.
			return 0;
		}
		return Ops.toInt(result);
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
	 *         an {@code int}
	 * @since 20130806
	 * @see #invokeVariableIndirect(SuValue, byte[], Object[])
	 */
	@_64BitIssue
	// see FIXME below
	public abstract int invoke(SuValue boundValue, byte[] argsIn);

	// TODO: This will change to Object argsIn to be compatible with AMD64
	// TODO: Add invoke0(SuValue) ... invoke4(SuValue, long a, ..., long d) for
	// faster performance -- won't implement on X86, so should throw
	// InternalError instead of being abstract.
	// FIXME: Return value will have to be 64-bit on x64 so it probably
	// makes most sense just to return 64-bit on all platforms for
	// simplicity...

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
	public abstract int invokeVariableIndirect(SuValue boundValue,
			byte[] argsIn, Object[] viArray);

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public final String getDisplayName() {
		return "callback" + typeList.toParamsTypeString();
	}

	@Override
	public final int getSizeDirectIntrinsic() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public final int getSizeDirectWholeWords() {
		return PrimitiveSize.pointerWholeWordBytes();
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
		return oldValue; // Nothing to be done here
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
