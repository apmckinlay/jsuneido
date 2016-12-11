package suneido.runtime;

/**
 * Ancestor class for callables which are "compiled" from Suneido source code as
 * opposed to built-in in some way.
 *
 * @author Victor Schappert
 * @since 20140913
 * @see SuWrappingCallable
 */
public class SuCompiledCallable extends SuCallable {

	//
	// DATA
	//

	private String library;
	private String sourceCode;
	private byte[] byteCode;

	/**
	 * Called by the compiler to set source library and source code information
	 * of a newly compiled callable.
	 *
	 * @param library
	 *            Library where {@code sourceCode} came from
	 * @param name
	 *            Library record name where {@code sourceCode} came from
	 * @param sourceCode
	 *            Suneido source code of the compiled object
	 * @return {@code this}
	 */
	public final SuCompiledCallable setSource(String library, String name,
	        String sourceCode) {
		this.name = name;
		this.library = library;
		this.sourceCode = sourceCode;
		return this;
	}

	/**
	 * Called by the compiler to complete construction of a newly compiled
	 * callable.
	 *
	 * @since 20140829
	 * @param myClass
	 *            Callable's class if applicable
	 * @param params
	 *            Callable's function spec
	 * @param context
	 *            Context callable belongs to
	 * @param callableType
	 *            What kind of callable this is, <i>eg</i> function
	 * @param byteCode
	 *            Java bytecode of the compiled object (may be {@code null})
	 * @return {@code this}
	 */
	public final SuCompiledCallable finishInit(SuClass myClass,
	        FunctionSpec params, ContextLayered context,
	        CallableType callableType, byte[] byteCode) {
		this.myClass = myClass;
		this.params = params;
		this.context = context;
		this.callableType = callableType;
		this.byteCode = byteCode;
		return this;
	}

	//
	// ANCESTOR CLASS: SuCallable
	//

	@Override
	protected StringBuilder appendLibrary(StringBuilder sb) {
		if (null != library && !library.isEmpty()) {
			sb.append(library).append(' ');
		}
		return sb;
	}

	@Override
	public String sourceCode() {
		return sourceCode;
	}

	@Override
	public byte[] byteCode() {
		return byteCode;
	}
}
