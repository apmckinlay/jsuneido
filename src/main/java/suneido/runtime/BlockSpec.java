/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import javax.annotation.concurrent.Immutable;

/**
 * <p>
 * Extended variable information for closures. As well as containing the local
 * variable information provided by an {@link ArgsArraySpec}, contains
 * information about upvalues (a.k.a. "free variables"). Upvalues are variables
 * available to a closure even though they are defined outside of the closure's
 * lexical scope.
 * </p>
 *
 * <p>
 * Note that the compiler lays out each args array as if it were made up of
 * three "blocks" of variables as follows:
 * <pre>&lt; [ upvalues ] ; [ params ] ; [ true locals ] &gt;</pre>
 * </p>
 *
 * @author Andrew McKinlay, Victor Schappert
 */
@Immutable
public final class BlockSpec extends ArgsArraySpec {

	//
	// DATA
	//

	final String[] upvalueNames;
	final int iparams; // index of first param in args array

	//
	// CONSTRUCTORS
	//

	public BlockSpec(String name, String[] paramNames, boolean atParam,
			String[] localNames, String[] upvalueNames) {
		super(paramNames, NO_DEFAULTS, atParam, null, localNames);
		this.upvalueNames = upvalueNames;
		this.iparams = upvalueNames.length;
	}

	//
	// ANCESTOR CLASS: ArgsArraySpec
	//

	@Override
	public Object getParamValueFromArgsArray(Object[] args, int index) {
		return args[iparams + index];
	}

	@Override
	public Object getLocalValueFromArgsArray(Object[] args, int index) {
		return args[iparams + paramNames.length + index];
	}

	@Override
	public int getUpvalueCount() {
		return iparams;
	}

	@Override
	public String getUpvalueName(int index) {
		return upvalueNames[index];
	}

	@Override
	public Object getUpvalueFromArgsArray(Object[] args, int index) {
		return args[index];
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getClass().getSimpleName() + " extends " + super.toString();
	}
}
