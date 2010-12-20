package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.*;
import suneido.language.*;

public class Database extends BuiltinClass {
	public static final Database singleton = new Database();

	private Database() {
		super(Database.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Database");
	}

	private static final FunctionSpec requestFS = new FunctionSpec("request");

	@Override
	public Object call(Object... args) {
		args = Args.massage(requestFS, args);
		String request = Ops.toStr(args[0]);
		TheDbms.dbms().admin(request);
		return Boolean.TRUE;
	}

	public static class Connections extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return TheDbms.dbms().connections();
		}
	}

	public static class CurrentSize extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return TheDbms.dbms().size();
		}
	}

	public static class Cursors extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return TheDbms.dbms().cursors();
		}
	}

	public static class Kill extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			return TheDbms.dbms().kill(Ops.toStr(a));
		}
	}

	public static class SessionId extends SuMethod1 {
		{ params = new FunctionSpec(array("string"), ""); }
		@Override
		public Object eval1(Object self, Object a) {
			return TheDbms.dbms().sessionid(Ops.toStr(a));
		}
	}

	public static class TempDest extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return 0;
		}
	}

	public static class Transactions extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return new SuContainer(TheDbms.dbms().tranlist());
		}
	}

}
