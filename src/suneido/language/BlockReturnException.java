package suneido.language;

/**
 * return from within block is implemented as throw BlockReturnException
 */
@SuppressWarnings("serial")
public class BlockReturnException extends RuntimeException {
	public final Object returnValue;

	public BlockReturnException(Object returnValue) {
		this.returnValue = returnValue;
	}

	@Override
	public String toString() {
		return "block-return(" + Ops.toString(returnValue) + ")";
	}
}
