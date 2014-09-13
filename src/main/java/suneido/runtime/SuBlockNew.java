/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * A plain block, which is essentially an un-named function within the lexical
 * scope of another callable entity.
 * </p>
 *
 * <p>
 * May be wrapped in an {@link SuClosure} to form a closure.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140912
 */
public abstract class SuBlockNew extends SuFunction {

	//
	// ANCESTOR CLASS: SuCallable
	//

	@Override
	public final CallableType callableType() {
		return CallableType.BLOCK;
	}
}
