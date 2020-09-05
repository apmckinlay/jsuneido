/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuException;
import suneido.SuObject;
import suneido.TheDbms;
import suneido.runtime.*;
import suneido.util.Dnum;
import suneido.util.Util;

public class Database extends BuiltinClass {
	public static final Database singleton = new Database();

	private Database() {
		super(Database.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("can't create instances of Database");
	}

	private static final FunctionSpec requestFS = new FunctionSpec("request");

	@Override
	public Object call(Object... args) {
		args = Args.massage(requestFS, args);
		String request = Ops.toStr(args[0]);
		TheDbms.dbms().admin(request);
		return null;
	}

	public static Object Connections(Object self) {
		return TheDbms.dbms().connections();
	}

	public static Object CurrentSize(Object self) {
		return Dnum.from(TheDbms.dbms().size());
	}

	public static Object Cursors(Object self) {
		return TheDbms.dbms().cursors();
	}

	public static Object Final(Object self) {
		return TheDbms.dbms().finalSize();
	}

	public static SuObject Info(Object self){
		return TheDbms.dbms().info();
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

	@Params("tn")
	public static Object Transaction(Object self, Object a) {
		var t = TheDbms.dbms().transaction(Ops.toInt(a));
		return t == null ? false : new SuTransaction(t);
	}

	public static Object Transactions(Object self) {
		return new SuObject(TheDbms.dbms().transactions());
	}

	@Params("table = ''")
	public static Object Dump(Object self, Object table) {
		String result = TheDbms.dbms().dump(Ops.toStr(table));
		if (! "".equals(result))
			throw new SuException("Database.Dump failed: " + result);
		return null;
	}

	@Params("filename")
	public static Object Load(Object self, Object filename) {
		int result = TheDbms.dbms().load(Ops.toStr(filename));
		if (result < 0)
			throw new SuException("Database.Load failed: " + filename);
		return result;
	}

	public static Object Check(Object self) {
		return TheDbms.dbms().check();
	}

	public static Object Nonce(Object self) {
		return Util.bytesToString(TheDbms.dbms().nonce());
	}

	public static Object Token(Object self) {
		return Util.bytesToString(TheDbms.dbms().token());
	}

	@Params("string")
	public static Object Auth(Object self, Object a) {
		return TheDbms.dbms().auth(Ops.toStr(a));
	}

}
