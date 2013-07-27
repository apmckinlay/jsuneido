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

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130708
 */
@DllInterface
public class Dll extends SuCallable {

	//
	// DATA
	//

	final long            funcPtr;
	final TypeList        dllParams; // not to confuse with SuCallable.params
	final Type            returnType;
	final ReturnTypeGroup returnTypeGroup; // never null
	final NativeCall      nativeCall;      // null if params isn't closed

	final String     suTypeName;
	final DllFactory dllFactory;
	final String     libraryName;
	final String     userFunctionName;
	final String     actualFunctionName;

	MarshallPlan marshallPlan;

	//
	// CONSTRUCTORS
	//

	// deliberately package-internal
	// todo: docs
	Dll(long funcPtr, TypeList params, Type returnType,
			ReturnTypeGroup returnTypeGroup, NativeCall nc, String suTypeName,
			DllFactory dllFactory, String libraryName, String userFuncName,
			String funcName, FunctionSpec functionSpec) {
		assert 0 != funcPtr : "Invalid dll function pointer";
		assert (TypeId.VOID == returnType.getTypeId() || TypeId.BASIC == returnType
				.getTypeId() && StorageType.VALUE == returnType.getStorageType())
				|| InOutString.INSTANCE == returnType : "Invalid dll return type";
		assert null != returnTypeGroup;
		//
		// Initialize Dll fields
		//
		this.funcPtr = funcPtr;
		this.dllParams = params;
		this.returnType = returnType;
		this.returnTypeGroup = returnTypeGroup;
		this.nativeCall = nc;
		this.suTypeName = suTypeName;
		this.dllFactory = dllFactory;
		this.libraryName = libraryName;
		this.userFunctionName = userFuncName;
		this.actualFunctionName = funcName;
		this.marshallPlan = null;
		//
		// Initialize SuCallable fields
		//
		
		super.params = functionSpec;
	}

	//
	// MUTATORS
	//

	private boolean resolve() {
		try {
			return dllParams.resolve(0);
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
		if (resolve() || null == marshallPlan) {
			marshallPlan = dllParams.makeParamsMarshallPlan();
		}
		return marshallPlan;
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
	public Object call(Object... args) { // TODO: should this method be final?
		Args.massage(super.params, args);
		final MarshallPlan plan = getMarshallPlan();
		final Marshaller m = plan.makeMarshaller();
		dllParams.marshallInParams(m, args);
		NativeCall nc = null == nativeCall ? NativeCall.get(
				CallGroup.fromTypeList(dllParams, true), returnTypeGroup,
				dllParams.size()) : nativeCall;
		long returnValueRaw = nc.invoke(funcPtr, plan.getSizeDirectWholeWords(), m);
		Object returnValue = null;
		if (VoidType.INSTANCE != returnType) {
			returnValue = returnType.marshallOutReturnValue(returnValueRaw);
		}
		if (! dllParams.isClosed()) {
			m.rewind();
			dllParams.marshallOutParams(m, args);
		}
		return returnValue;
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

}
