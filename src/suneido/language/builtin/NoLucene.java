package suneido.language.builtin;

import suneido.SuException;
import suneido.language.*;

public class NoLucene extends BuiltinClass {
	public static final NoLucene singleton = new NoLucene();
	
	private NoLucene() {
		super(NoLucene.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Lucene");
	}

	public static class AvailableQ extends SuMethod1 {
		{ params = new FunctionSpec("dir"); }
		@Override
		public Object eval1(Object self, Object a) {
			return false;
		}
	}
	
}