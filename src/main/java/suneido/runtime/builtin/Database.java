/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.SuException;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.runtime.*;

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

	public static Object Connections(Object self) {
		return TheDbms.dbms().connections();
	}

	public static Object CurrentSize(Object self) {
		return TheDbms.dbms().size();
	}

	public static Object Cursors(Object self) {
		return TheDbms.dbms().cursors();
	}

	@Params("string")
	public static Object Kill(Object self, Object a) {
		return TheDbms.dbms().kill(Ops.toStr(a));
	}

	@Params("string=''")
	public static Object SessionId(Object self, Object a) {
		return TheDbms.dbms().sessionid(Ops.toStr(a));
	}

	public static Object TempDest(Object self) {
		return 0;
	}

	public static Object Transactions(Object self) {
		return new SuContainer(TheDbms.dbms().tranlist());
	}

	@Params("string")
	public static Object Impersonate(Object self, Object a) {
		return Suneido.cmdlineoptions.impersonate = Ops.toStr(a);
	}

}
