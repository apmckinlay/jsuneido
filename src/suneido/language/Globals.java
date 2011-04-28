/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import suneido.*;
import suneido.language.builtin.*;
import suneido.language.builtin.AssertionError;
import suneido.language.builtin.NullPointerException;

/**
 * Stores global names and values.
 * Uses the class itself as a singleton by making everything static.
 */
public class Globals {
	private static final Map<String, Object> globals =
			new ConcurrentHashMap<String, Object>(1000);
	private static final Map<String, Object> builtins =
			new ConcurrentHashMap<String, Object>(100);
	private static final AtomicInteger overload = new AtomicInteger();
	static {
		builtins.put("True", Boolean.TRUE);
		builtins.put("False", Boolean.FALSE);
		builtins.put("Suneido", new SuContainer());

		builtins.put("Add", new Add());
		builtins.put("Adler32", Adler32.clazz);
		builtins.put("And", new And());
		builtins.put("AssertionError", new AssertionError());
		builtins.put("Boolean?", new BooleanQ());
		builtins.put("Built", new Built());
		builtins.put("Cat", new Cat());
		builtins.put("Class?", new ClassQ());
		builtins.put("Cmdline", new Cmdline());
		builtins.put("Construct", new Construct());
		builtins.put("CopyFile", new CopyFile());
		builtins.put("CreateDirectory", new CreateDirectory());
		builtins.put("Cursor", Cursor.clazz);
		builtins.put("Database", Database.singleton);
		builtins.put("Date", DateClass.singleton);
		builtins.put("Date?", new DateQ());
		builtins.put("Delayed", new Delayed());
		builtins.put("DeleteDir", new DeleteDir());
		builtins.put("DeleteFile", new DeleteFile());
		builtins.put("Dir", new Dir());
		builtins.put("DirExists?", new DirExistsQ());
		builtins.put("Display", new Display());
		builtins.put("Div", new Div());
		builtins.put("Dump", new Dump());
		builtins.put("DoWithoutTriggers", new DoWithoutTriggers());
		builtins.put("Eq", new Eq());
		builtins.put("ExePath", new ExePath());
		builtins.put("Exit", new Exit());
		builtins.put("File", SuFile.clazz);
		builtins.put("FileExists?", new FileExistsQ());
		builtins.put("Frame", new Frame());
		builtins.put("Function?", new FunctionQ());
		builtins.put("GetCurrentDirectory", new GetCurrentDirectory());
		builtins.put("GetDiskFreeSpace", new GetDiskFreeSpace());
		builtins.put("GetMacAddress", new GetMacAddress());
		builtins.put("GetTempPath", new GetTempPath());
		builtins.put("Gt", new Gt());
		builtins.put("Gte", new Gte());
		builtins.put("Libraries", new Libraries());
		builtins.put("Lt", new Lt());
		builtins.put("Lte", new Lte());
		builtins.put("Lucene", Lucene.singleton);
		builtins.put("Match", new Match());
		builtins.put("Md5", new Md5());
		builtins.put("MemoryArena", new MemoryArena());
		builtins.put("Mod", new Mod());
		builtins.put("MoveFile", new MoveFile());
		builtins.put("Mul", new Mul());
		builtins.put("Neg", new Neg());
		builtins.put("Neq", new Neq());
		builtins.put("NoMatch", new NoMatch());
		builtins.put("Not", new Not());
		builtins.put("NullPointerException", new NullPointerException());
		builtins.put("Number?", new NumberQ());
		builtins.put("Object", new ObjectClass());
		builtins.put("Object?", new ObjectQ());
		builtins.put("OperatingSystem", new OperatingSystem());
		builtins.put("Or", new Or());
		builtins.put("Pack", new suneido.language.builtin.Pack());
		builtins.put("Print", new Print());
		builtins.put("Query1", new Query1());
		builtins.put("QueryFirst", new QueryFirst());
		builtins.put("QueryLast", new QueryLast());
		builtins.put("Random", new Random());
		builtins.put("Record", new RecordClass());
		builtins.put("Record?", new RecordQ());
		builtins.put("RunPiped", RunPiped.clazz);
		builtins.put("Scanner", Scanner.clazz);
		builtins.put("Seq", new Seq());
		builtins.put("ServerEval", new ServerEval());
		builtins.put("ServerIP", new ServerIP());
		builtins.put("ServerPort", new ServerPort());
		builtins.put("Server?", new ServerQ());
		builtins.put("Sleep", new Sleep());
		builtins.put("SocketClient", SocketClient.clazz);
		builtins.put("SocketServer", SocketServer.singleton);
		builtins.put("String?", new StringQ());
		builtins.put("Sub", new Sub());
		builtins.put("Synchronized", new Synchronized());
		builtins.put("System", new SystemFunction());
		builtins.put("SystemMemory", new SystemMemory());
		builtins.put("Thread", new ThreadFunction());
		builtins.put("Timestamp", new Timestamp());
		builtins.put("Trace", new suneido.language.builtin.Trace());
		builtins.put("Transaction", SuTransaction.clazz);
		builtins.put("Type", new Type());
		builtins.put("UuidString", new UuidString());
		builtins.put("Unload", new Unload());
		builtins.put("Unpack", new Unpack());
		builtins.put("Unuse", new Unuse());
		builtins.put("UnixTime", new UnixTime());
		builtins.put("Use", new Use());
	}

	private Globals() { // no instances
		throw SuException.unreachable();
	}

	public static void builtin(String name, Object value) {
		builtins.put(name, value);
	}

	public static int size() {
		return globals.size();
	}

	public static Object get(String name) {
		Object x = tryget(name);
		if (x == null)
			throw new SuException("can't find " + name);
		return x;
	}

	private static final Object nonExistent = new Object();

	/**
	 * does NOT prevent two threads concurrently getting same name but this
	 * shouldn't matter since it's idempotent i.e. result should be the same no
	 * matter which thread "wins"
	 */
	public static Object tryget(String name) {
		Object x = globals.get(name);
		if (x != null)
			return x == nonExistent ? null : x;
		x = builtins.get(name);
		if (x == null)
			x = Library.load(name);
		globals.put(name, x == null ? nonExistent : x);
		return x;
	}


	/** used by tests */
	public static void put(String name, Object x) {
		globals.put(name, x);
	}

	public static void unload(String name) {
		globals.remove(name);
	}

	/** for Use */
	public static void clear() {
		globals.clear();
	}

	// synchronized by Library.load which should be the only caller
	public static String overload(String base) {
		String name = base.substring(1); // remove leading underscore
		Object x = globals.get(name);
		if (x == null || x == nonExistent)
			throw new SuException("can't find " + base);
		int n = overload.getAndIncrement();
		String nameForPreviousValue = n + base;
		globals.put(nameForPreviousValue, x);
		return nameForPreviousValue;
	}

	/** used by generated code to call globals
	 *  NOTE: does NOT handle calling a string in a global
	 *  requires globals to be SuValue
	 */
	public static Object invoke(String name, Object... args) {
		return ((SuValue) get(name)).call(args);
	}
	public static Object invoke0(String name) {
		return ((SuValue) get(name)).call0();
	}
	public static Object invoke1(String name, Object a) {
		return ((SuValue) get(name)).call1(a);
	}
	public static Object invoke2(String name, Object a, Object b) {
		return ((SuValue) get(name)).call2(a, b);
	}
	public static Object invoke3(String name, Object a, Object b, Object c) {
		return ((SuValue) get(name)).call3(a, b, c);
	}
	public static Object invoke4(String name, Object a, Object b,
			Object c, Object d) {
		return ((SuValue) get(name)).call4(a, b, c, d);
	}

}
