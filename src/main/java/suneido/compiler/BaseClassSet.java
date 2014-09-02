/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import org.objectweb.asm.Type;

import suneido.SuInternalError;
import suneido.runtime.SuBlock;
import suneido.runtime.SuBlock0;
import suneido.runtime.SuBlock1;
import suneido.runtime.SuBlock2;
import suneido.runtime.SuBlock3;
import suneido.runtime.SuBlock4;
import suneido.runtime.SuCallable;
import suneido.runtime.SuFunction;
import suneido.runtime.SuFunction0;
import suneido.runtime.SuFunction1;
import suneido.runtime.SuFunction2;
import suneido.runtime.SuFunction3;
import suneido.runtime.SuFunction4;
import suneido.runtime.SuMethod;
import suneido.runtime.SuMethod0;
import suneido.runtime.SuMethod1;
import suneido.runtime.SuMethod2;
import suneido.runtime.SuMethod3;
import suneido.runtime.SuMethod4;

/**
 * Represents a base class, and a family of specializations on that base class,
 * that dynamically compiled bytecode can extend.
 * 
 * @author Victor Schappert
 * @since 20140829
 */
final class BaseClassSet {

	//
	// DATA
	//

	private final Type unspecialized;
	private final Type[] specializations;

	//
	// CONSTANTS
	//

	static final BaseClassSet CALLABLE = new BaseClassSet(SuCallable.class);
	static final BaseClassSet FUNCTION = new BaseClassSet(SuFunction.class,
			SuFunction0.class, SuFunction1.class, SuFunction2.class,
			SuFunction3.class, SuFunction4.class);
	static final BaseClassSet METHOD = new BaseClassSet(SuMethod.class,
			SuMethod0.class, SuMethod1.class, SuMethod2.class, SuMethod3.class,
			SuMethod4.class);
	static final BaseClassSet BLOCK = new BaseClassSet(SuBlock.class,
			SuBlock0.class, SuBlock1.class, SuBlock2.class, SuBlock3.class,
			SuBlock4.class);

	//
	// CONSTRUCTORS
	//

	@SafeVarargs
	private <T extends SuCallable> BaseClassSet(Class<T> unspecialized,
			Class<? extends T>... specializations) {
		this.unspecialized = Type.getType(unspecialized);
		this.specializations = new Type[specializations.length];
		for (int k = 0; k < specializations.length; ++k) {
			this.specializations[k] = Type.getType(specializations[k]);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the ASM "internal name" of the unspecialized base class.
	 *
	 * @return Unspecialized class internal name
	 * @see #getInternalName(int)
	 */
	String getInternalName() {
		return unspecialized.getInternalName();
	}

	/**
	 * Returns the ASM "internal name" of a specialization of the base class.
	 *
	 * @param specializationIndex
	 *            Valid zero-based index of a specialization
	 * @return Specialized class internal name
	 */
	String getInternalName(int specializationIndex) {
		assert 0 <= specializationIndex;
		if (specializations.length < specializationIndex) {
			throw new SuInternalError("invalid specialization of "
					+ getInternalName() + ": " + specializationIndex);
		} else {
			return specializations[specializationIndex].getInternalName();
		}
	}
}
