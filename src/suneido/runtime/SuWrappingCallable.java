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
	protected final SuCallable wrapped;

	protected SuWrappingCallable(SuCallable wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public String sourceCode() {
		return wrapped.sourceCode();
	}

	@Override
	public String display() {
		return wrapped.display();
	}
	
	@Override
	public String internalName() {
		return wrapped.internalName();
	}

}
