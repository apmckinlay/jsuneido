package suneido.runtime;

/**
 * <p>
 * Ancestor class for callables which wrap another callable.
 * <p>
 *
 * <p>
 * <i>ie</i> {@link SuBoundMethod}, {@link SuClosure}
 * </p>
 *
 * @author Victor Schappert
 * @since 20140913
 * @see SuCompiledCallable
 */
public class SuWrappingCallable extends SuCallable {

	//
	// DATA
	//

	protected final SuCallable wrapped;

	//
	// CONSTRUCTORS
	//

	protected SuWrappingCallable(SuCallable wrapped) {
		this.wrapped = wrapped;
	}

	//
	// ANCESTOR CLASS: SuCallable
	//

	@Override
	public String sourceCode() {
		return wrapped.sourceCode();
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String display() {
		return wrapped.display();
	}
}
