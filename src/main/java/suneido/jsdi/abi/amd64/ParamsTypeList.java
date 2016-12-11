/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import java.util.Iterator;

import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.type.LateBinding;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeId;
import suneido.jsdi.type.VoidType;

/**
 * Customization of type list for x64 function parameters: unlike other type
 * lists, it is aware of parameter register usage in the amd64 ABI.
 *
 * @author Victor Schappert
 * @since 20140801
 */
@DllInterface
final class ParamsTypeList extends TypeList64 {

	//
	// DATA
	//

	private int registerUsage; // Register usage of first four parameters
	private final Type[] prt; // Either four element array or null

	//
	// CONSTRUCTORS
	//

	ParamsTypeList(Args args) {
		super(args);
		registerUsage = ParamRegisterType.ALL_FOUR_REGISTERS_ARE_UINTS;
		prt = initParamRegisterTypes();
		shimLateBindingPassByValues();
	}

	//
	// INTERNALS
	//

	private Type[] initParamRegisterTypes() {
		final Iterator<Entry> it = iterator();
		// If no parameters, don't need to worry about registers
		if (!it.hasNext()) {
			return null;
		}
		final Type[] result = new Type[] { it.next().getType(),
				VoidType.INSTANCE, VoidType.INSTANCE, VoidType.INSTANCE };
		boolean allRegisterParamsClosed = result[0].isClosed();
		if (it.hasNext()) {
			result[1] = it.next().getType();
			allRegisterParamsClosed &= result[1].isClosed();
			if (it.hasNext()) {
				result[2] = it.next().getType();
				allRegisterParamsClosed &= result[2].isClosed();
				if (it.hasNext()) {
					result[3] = it.next().getType();
					allRegisterParamsClosed &= result[3].isClosed();
				}
			}
		}
		// If all of the register parameters are "closed types" (meaning they
		// are bound at "compile" time rather than late-binding types), we can
		// figure out the register usage. The null value for 'prt' signifies
		// that register types don't need to be figured again.
		if (allRegisterParamsClosed) {
			registerUsage = ParamRegisterType.packFromParamTypes(result[0],
					result[1], result[2], result[3]);
			return null;
		}
		// If there's at least one late-binding type in the first four
		// registers, return a four-element array we can use to compute the
		// register usage after every bind.
		return result;
	}

	private void shimLateBindingPassByValues() {
		int k = 0;
		for (final Entry entry : this) {
			final Type type = entry.getType();
			if (TypeId.LATE_BINDING == type.getTypeId()
					&& StorageType.VALUE == type.getStorageType()) {
				modifyEntryType(k, new ByValShim((LateBinding) type));
			}
			++k;
		}
	}

	//
	// ANCESTOR CLASS: TypeList
	//

	@Override
	protected void updateStateAfterBind() {
		super.updateStateAfterBind();
		if (null != prt) {
			registerUsage = ParamRegisterType.packFromParamTypes(prt[0],
					prt[1], prt[2], prt[3]);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns {@code true} iff the types of all parameters that are passed in
	 * registers is closed.
	 *
	 * @return Whether all parameters up to the first four have closed types
	 * @see Type#isClosed()
	 */
	public boolean isRegisterUsageClosed() {
		return null == prt;
	}

	/**
	 * <p>
	 * Returns a value that can be sent to the native side to indicate how the
	 * parameter registers should be used.
	 * </p>
	 *
	 * <p>
	 * If {@link #isRegisterUsageClosed()} returns {@code false} then the value
	 * returned will vary across binds. 
	 * </p>
	 *
	 * @return Register usage value
	 * @see #needsFpRegister()
	 */
	public int getRegisterUsage() {
		return registerUsage;
	}

	/**
	 * <p>
	 * Returns {@code true} iff {@link #getRegisterUsage()} indicates that at
	 * least one parameter must be passed in a floating-point register.
	 *</p>
	 * 
	 * <p>
	 * If {@link #isRegisterUsageClosed()} returns {@code false} then the value
	 * returned will vary across binds. 
	 * </p>
	 *
	 * @return Whether at least one floating-point register is required
	 * @see #getRegisterUsage()
	 */
	public boolean needsFpRegister() {
		return ParamRegisterType.ALL_FOUR_REGISTERS_ARE_UINTS != registerUsage;
	}
}
