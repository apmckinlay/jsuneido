package suneido.language.jsdi.dll;

import suneido.language.Args;
import suneido.language.FunctionSpec;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;
import suneido.language.jsdi.type.VoidType;

/**
 * <p>
 * Simple class implementing a "void" {@code dll}. A "void" {@code dll} is a
 * syntactically valid {@code dll} which doesn't actually have a library and
 * thus isn't linked to a DLL function. The purpose of the "void" {@code dll} is
 * to enable test code to obtain a reference to a {@code dll} without the need
 * for an underlying library function.
 * </p>
 * <p>
 * Examples:
 * <pre>dll void library:function(long param)
 *     => dll which calls 'function' declared in 'library' when invoked
 * dll void void:function(long param)
 *     => "void" dll which simply returns false when invoked</pre>
 * </p>
 * @author Victor Schappert
 * @since 20130815
 */
@DllInterface
public final class VoidDll extends Dll {

	//
	// CONSTRUCTORS
	//

	VoidDll(TypeList params, Type returnType,
			ReturnTypeGroup returnTypeGroup, String valueName,
			DllFactory dllFactory, String userFuncName,
			FunctionSpec functionSpec) {
		super(0, params, returnType, returnTypeGroup, null, valueName,
				dllFactory, VoidType.IDENTIFIER, userFuncName, userFuncName,
				functionSpec);
	}

	//
	// ANCESTOR TYPE: SuCallable
	//

	@Override
	public Object call(Object... args) {
		Args.massage(super.params, args);
		return Boolean.FALSE;
	}
}
