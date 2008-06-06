package suneido;

import static suneido.Symbols.*;

/**
 * SuMethod makes methods first class values.
 * It binds the method and the instance it "came from".
 * @author Andrew McKinlay
 *
 */
public class SuMethod extends SuValue {
	private final SuValue instance;
	private final int method;
	
	public SuMethod(SuValue instance, int method) {
		this.instance = instance;
		this.method = method;
	}
	
	@Override
	public SuValue invoke(int method, SuValue ... args) {
		return method == Num.CALL
			? instance.invoke(this.method, args)
			: super.invoke(method, args);
	}

	@Override
	public String toString() {
		return instance.toString() + "." + Symbols.symbol(method);
	}

}
