package suneido.language.jsdi.dll;

import java.util.Iterator;
import java.util.Map;

import suneido.SuValue;
import suneido.language.Args;
import suneido.language.BuiltinMethods;
import suneido.language.FunctionSpec;
import suneido.language.SuCallable;
import suneido.language.jsdi.*;
import suneido.language.jsdi.type.*;

@DllInterface
public class Dll extends SuCallable {

	//
	// DATA
	//

	final long       funcPtr;
	final TypeList   dllParams; // not to confuse with SuCallable.params
	final Type       returnType;

	final String     suTypeName;
	final DllFactory dllFactory;
	final String     libraryName;
	final String     userFunctionName;
	final String     actualFunctionName;

	//
	// CONSTRUCTORS
	//

	// deliberately package-internal
	// todo: docs
	Dll(long funcPtr, TypeList params, Type returnType, String suTypeName,
			DllFactory dllFactory, String libraryName, String userFuncName,
			String funcName, FunctionSpec functionSpec) {
		assert 0 != funcPtr : "Invalid dll function pointer";
		assert (TypeId.VOID == returnType.getTypeId() || TypeId.BASIC == returnType
				.getTypeId() && StorageType.VALUE == returnType.getStorageType())
				|| StringIndirect.INSTANCE_STRING == returnType : "Invalid dll return type";
		//
		// Initialize Dll fields
		//
		this.funcPtr = funcPtr;
		this.dllParams = params;
		this.returnType = returnType;
		this.suTypeName = suTypeName;
		this.dllFactory = dllFactory;
		this.libraryName = libraryName;
		this.userFunctionName = userFuncName;
		this.actualFunctionName = funcName;
		//
		// Initialize SuCallable fields
		//
		
		super.params = functionSpec;
	}

	//
	// MUTATORS
	//

	private void resolve() {
		try {
			dllParams.resolve(0);
		} catch (ProxyResolveException e) {
			e.setMemberType("parameter");
			throw new JSDIException(e);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the Suneido type name.
	 * 
	 * <p>
	 * For example, for a global {@code dll} in a library record called 'X',
	 * the returned value is "X". However, the value returned is not necessarily
	 * a global name. For anonymous types, the Suneido type name is an
	 * arbitrary value assigned by the compiler.
	 * </p>
	 *
	 * <p>
	 * This method is called {@code getSuTypeName()} to differentiate it from
	 * {@link SuValue#typeName()}, which relates to the JSuneido universe rather
	 * than the user's type naming universe. 
	 * </p>
	 *
	 * @return Suneido type name
	 * @see suneido.language.jsdi.type.ComplexType#getSuTypeName()
	 */
	public final String getSuTypeName() {
		return suTypeName;
	}

	public final String getSignature() {
		StringBuilder result = new StringBuilder();
		result.append('(');
		Iterator<TypeList.Entry> i = dllParams.iterator();
		if (i.hasNext()) {
			result.append(i.next().getType().getDisplayName());
			while(i.hasNext()) {
				result.append(',').append(i.next().getType().getDisplayName());
			}
		}
		result.append(')').append(returnType.getDisplayName());
		return result.toString();
	}

	public final Type getReturnType() {
		return returnType;
	}

	public final MarshallPlan getMarshallPlan() {
		// TODO: resolve: thread safety
		resolve();
		return dllParams.getMarshallPlan();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Dll.class);

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : super.lookup(method);
	}

	@Override
	public Object call(Object ... args) { // TODO: should this method be final?
		Args.massage(super.params, args);
		try {
			dllParams.resolve(0);
		} catch (ProxyResolveException e) {
			// XXX: handle proxy resolve exception in dll
		}
		final MarshallPlan plan = dllParams.getMarshallPlan();
		final Marshaller m = plan.makeMarshaller();
		dllParams.marshallInParams(m, args);
		
		// TODO: call the bound jsdi method
		// TODO: marshall out the params
		// TODO: marshall out the return value
		throw new RuntimeException("not implemented"); // TODO: implement
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append("dll ").append(returnType.getDisplayName()).append(' ')
				.append(libraryName).append(':').append(actualFunctionName);
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

	//
	// INTERNALS
	//

	

	//
	// NATIVE METHODS. These functions are available to specific instances of
	//     DllBase which are derived from this class.
	//

	protected static native void callLReturnV(long funcPtr, int arg0);

	protected static native void callLLReturnV(long funcPtr, int arg0, int arg1);

	protected static native void callLLLReturnV(long funcPtr, int arg0,
			int arg1, int arg2);

	protected static native void callLLLLReturnV(long funcPtr, int arg0,
			int arg1, int arg2, int arg3);

	protected static native int callLReturnL(long funcPtr, int arg0);

	protected static native int callLLReturnL(long funcPtr, int arg0, int arg1);

	protected static native int callLLLReturnL(long funcPtr, int arg0,
			int arg1, int arg2);

	protected static native int callLLLLReturnL(long funcPtr, int arg0,
			int arg1, int arg2, int arg3);

	protected static native void callDirectOnlyReturnV(long funcPtr,
			int sizeDirect, byte[] args);

	protected static native int callDirectOnlyReturn32bit(long funcPtr,
			int sizeDirect, byte[] args);

	protected static native long callDirectOnlyReturn64bit(long funcPtr,
			int sizeDirect, byte[] args);

	protected static native void callIndirectReturnV(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	protected static native int callIndirectReturn32bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	protected static native long callIndirectReturn64bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	protected static native void callVariableIndirectReturnV(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, byte[] vi);

	protected static native void callVariableIndirectReturn32bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, byte[] vi);

	protected static native void callVariableIndirectReturn64bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, byte[] vi);
}
