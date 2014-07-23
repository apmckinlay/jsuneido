/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import suneido.SuContainer;
import suneido.language.Ops;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class ObjectConversions {

	private static void throwCantConvert(Object value) throws JSDIException {
		throw new JSDIException("can't convert " + Ops.typeName(value)
				+ " to object");
	}

	// TODO: docs -- since 20130718
	public static SuContainer containerOrThrow(Object value, int vecCapacity) {
		if (null == value) {
			return new SuContainer(vecCapacity);
		} else {
			return containerOrThrow(value);
		}
	}

	// TODO: docs -- since 20130808
	public static SuContainer containerOrThrow(Object value) {
		assert null != value;
		final SuContainer c = Ops.toContainer(value);
		if (null == c) {
			throwCantConvert(value);
		}
		return c;
	}
}
