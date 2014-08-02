/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import java.util.Map;

import suneido.SuValue;
import suneido.jsdi.type.InOutString;
import suneido.jsdi.type.BindException;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeId;
import suneido.jsdi.type.TypeList;
import suneido.language.BuiltinMethods;
import suneido.language.FunctionSpec;
import suneido.language.SuCallable;

/**
 * Represents a Suneido {@code dll} callable.
 *
 * @author Victor Schappert
 * @since 20130708
 */
@DllInterface
public abstract class Dll extends SuCallable {

	//
	// DATA
	//

	protected final long     funcPtr;
	protected final TypeList dllParams;     // don't confuse w SuCallable.params
	protected final Type     returnType;

	private final String     valueName;
	private final DllFactory dllFactory;    // for finalization
	        final String     libraryName;   // package-visible for DllFactory
	private final String     funcName;

	//
	// CONSTRUCTORS
	//

	protected Dll(long funcPtr, TypeList params, Type returnType,
			String valueName, DllFactory dllFactory, String libraryName,
			String funcName) {
		assert (TypeId.VOID == returnType.getTypeId() || TypeId.BASIC == returnType
				.getTypeId()
				&& StorageType.VALUE == returnType.getStorageType())
				|| InOutString.INSTANCE == returnType : "Invalid dll return type";
		//
		// Initialize Dll fields
		//
		this.funcPtr = funcPtr;
		this.dllParams = params;
		this.returnType = returnType;
		this.valueName = valueName;
		this.dllFactory = dllFactory;
		this.libraryName = libraryName;
		this.funcName = funcName;
		//
		// Initialize SuCallable fields
		//
		super.params = new FunctionSpec(params.getEntryNames());
	}

	//
	// MUTATORS
	//

	protected boolean bind() {
		try {
			return dllParams.bind(0);
		} catch (BindException e) {
			e.setParentName(valueName());
			throw new JSDIException(e);
		}
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Dll.class);

	@Override
	public final String valueName() {
		return valueName;
	}

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : super.lookup(method);
	}

	@Override
	public final Object eval(Object self, Object... args) {
		// Need to implement 'eval' because a Dll could be a class or object
		// member as well as a standalone object, in which case calling it will
		// look like a method call to the compiler.
		return call(args);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append("dll ").append(returnType.getDisplayName()).append(' ')
				.append(libraryName).append(':').append(funcName);
		sb.append(dllParams.toParamsTypeString());
		sb.append(" /* 0x").append(Long.toHexString(funcPtr)).append(" */");
		return sb.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			dllFactory.freeDll(this);
		} finally {
			super.finalize();
		}
	}

	//
	// BUILT-IN METHODS
	//

	// TODO: implement -- see Suneidoc for dll.Trace
	// TODO: give it a parameter so you can turn it off!
	public static Object Trace(Object self) { 
		throw new RuntimeException("not yet implemented");
	}
}