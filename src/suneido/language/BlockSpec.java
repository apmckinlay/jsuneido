package suneido.language;

public class BlockSpec extends FunctionSpec {
	final int iparams; // index of first param in locals

	public BlockSpec(String name, String[] locals, int nparams,
			boolean atParam, int iparams) {
		super(name, locals, nparams, noConstants, 0, atParam);
		this.iparams = iparams;
	}

	@Override
	public String toString() {
		return "BlockSpec extends " + super.toString();
	}

}
