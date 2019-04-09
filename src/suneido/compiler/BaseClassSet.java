/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import org.objectweb.asm.Type;

import suneido.SuInternalError;
import suneido.runtime.SuCallBase;
import suneido.runtime.SuCallBase0;
import suneido.runtime.SuCallBase1;
import suneido.runtime.SuCallBase2;
import suneido.runtime.SuCallBase3;
import suneido.runtime.SuCallBase4;
import suneido.runtime.SuCallable;
import suneido.runtime.SuClosure;
import suneido.runtime.SuClosure0;
import suneido.runtime.SuClosure1;
import suneido.runtime.SuClosure2;
import suneido.runtime.SuClosure3;
import suneido.runtime.SuClosure4;
import suneido.runtime.SuEvalBase;
import suneido.runtime.SuEvalBase0;
import suneido.runtime.SuEvalBase1;
import suneido.runtime.SuEvalBase2;
import suneido.runtime.SuEvalBase3;
import suneido.runtime.SuEvalBase4;

/**
 * Represents a base class, and a family of specializations on that base class,
 * that dynamically compiled bytecode can extend.
 * 
 * @author Victor Schappert
 * @since 20140829
 */
final class BaseClassSet {

	//
	// TYPES
	//

	/**
	 * Represents a single base class or specialization of a base class.
	 *
	 * @author Victor Schappert
	 * @since 20140913
	 */
	public static final class BaseClass {
		private final String methodName;
		private final Type type;

		private BaseClass(String methodName, Class<?> clazz) {
			this.methodName = methodName;
			this.type = Type.getType(clazz);
		}

		/**
		 * Returns the ASM "internal name" of the base class.
		 *
		 * @return Class internal name
		 */
		public String getInternalName() {
			return type.getInternalName();
		}

		/**
		 * Returns the name of the method of the base class that needs to be
		 * overridden in the compiled subclass.
		 *
		 * @return Name of method to override
		 */
		public String getMethodName() {
			return methodName;
		}
	}

	//
	// DATA
	//

	private final BaseClass unspecialized;
	private final BaseClass[] specializations;

	//
	// CONSTANTS
	//

	static final BaseClassSet CALLBASE = new BaseClassSet("call",
			SuCallBase.class, SuCallBase0.class, SuCallBase1.class,
			SuCallBase2.class, SuCallBase3.class, SuCallBase4.class);
	static final BaseClassSet EVALBASE = new BaseClassSet("eval",
			SuEvalBase.class, SuEvalBase0.class, SuEvalBase1.class,
			SuEvalBase2.class, SuEvalBase3.class, SuEvalBase4.class);
	static final BaseClassSet CLOSURE = new BaseClassSet("?" /* not used */,
			SuClosure.class, SuClosure0.class, SuClosure1.class,
			SuClosure2.class, SuClosure3.class, SuClosure4.class);

	//
	// CONSTRUCTORS
	//

	@SafeVarargs
	private <T extends SuCallable> BaseClassSet(String methodName,
			Class<T> unspecialized, Class<? extends T>... specializations) {
		this.unspecialized = new BaseClass(methodName, unspecialized);
		this.specializations = new BaseClass[specializations.length];
		for (int k = 0; k < specializations.length; ++k) {
			this.specializations[k] = new BaseClass(methodName + k,
					specializations[k]);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the general version of the base class.
	 *
	 * @return Unspecialized base
	 * @see #getSpecialization(int)
	 */
	public BaseClass getUnspecialized() {
		return unspecialized;
	}

	/**
	 * Returns one of the available specializations of the base class.
	 * 
	 *
	 * @param specializationIndex
	 *            Valid zero-based index of a specialization
	 * @return Specialized base class
	 * @see #getUnspecialized()
	 */
	public BaseClass getSpecialization(int specializationIndex) {
		assert 0 <= specializationIndex;
		if (specializations.length < specializationIndex) {
			throw new SuInternalError("invalid specialization of "
					+ getUnspecialized().getInternalName() + ": "
					+ specializationIndex);
		}
		return specializations[specializationIndex];
	}
}
