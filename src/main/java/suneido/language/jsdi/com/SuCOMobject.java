/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.com;

import static suneido.util.Util.array;
import suneido.SuException;
import suneido.SuValue;
import suneido.language.Args;
import suneido.language.BuiltinClass;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;

/**
 * <p>
 * Implements the Suneido {@code COMobject} type which wraps a COM
 * {@code IDispatch} or {@code IUnknown} interface pointer.
 * </p>
 * @author Victor Schappert
 * @since 20130928
 */
@DllInterface
public final class SuCOMobject {

	//
	// DATA
	//

	private final String progid;

	//
	// CONSTRUCTORS
	//

	public SuCOMobject() {
		throw new RuntimeException("Not implemented");
	}

	//
	// BUILT-IN CLASS
	//

	/**
	 * Reference to a {@link BuiltinClass} that describes how to expose this
	 * class to the Suneido programmer.
	 * @see suneido.language.Builtins
	 */
	public static final SuValue clazz = new BuiltinClass() {

		private final FunctionSpec newFS = new FunctionSpec(
				array("progid-or-ptr"));

		@Override
		protected Object newInstance(Object... args) {
			args = Args.massage(newFS, args);
			Object x = args[0];
			if (x instanceof CharSequence) {
				// TODO: implement COMobject(progid)
				throw new RuntimeException("Not implemented: COMobject(progid)");
			} else {
				int ptr = 0;
				try {
					ptr = Ops.toIntIfNum(x);
				} catch (SuException e) {
					throw new SuException("can't convert " + Ops.typeName(x)
							+ " to String or Number");
				}
				// TODO: implement COMobject(ptr)
				throw new RuntimeException("Not implemented: COMobject(ptr)");
			}
		}
	};
}
