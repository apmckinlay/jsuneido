package suneido.language;

/**
 * return from within block is implemented as throw BlockReturnException
 */
@SuppressWarnings("serial")
public class BlockReturnException extends RuntimeException {
	public final Object returnValue;
	public final Object block; // used by SuCallable blockReturnHandler to identify "parent"

	public BlockReturnException(Object returnValue, Object block) {
		this.returnValue = returnValue;
		this.block = block;
	}

	@Override
	public String toString() {
		return "block-return(" + Ops.toStr(returnValue) + ")";
	}
}
