package suneido.language;


public class SampleFunction extends SuFunction {

	@Override
	public Object call(Object... args) {
		try {
			args[0] = args[1];
		} catch (BlockReturnException e) {
			return e.returnValue;
		}
		return null;
	}

}