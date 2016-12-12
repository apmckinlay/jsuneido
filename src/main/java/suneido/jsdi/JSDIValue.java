/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import suneido.SuValue;

/**
 * Intermediate class implementing a JSDI-specific {@link SuValue}
 * functionality.
 *
 * @author Victor Schappert
 * @since 20130815
 */
@DllInterface
public class JSDIValue extends SuValue {

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String typeName() {
		return getClass().getSimpleName();
	}
}
