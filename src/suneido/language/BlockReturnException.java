package suneido.language;

/**
 * return from within block is implemented as throw BlockReturnException
 */
@SuppressWarnings("serial")
public class BlockReturnException extends RuntimeException {
	public final Object returnValue;
	public final Object[] locals; // used to identify "parent" function

	public BlockReturnException(Object returnValue, Object[] locals) {
		this.returnValue = returnValue;
		this.locals = locals;
	}

	@Override
	public String toString() {
		return "block-return(" + Ops.toString(returnValue) + ")";
	}
}
