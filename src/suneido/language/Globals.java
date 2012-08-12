/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.language.builtin.*;
import suneido.language.builtin.AssertionError;
import suneido.language.builtin.NullPointerException;

import com.google.common.collect.ImmutableMap;

/**
 * Stores global names and values.
 * Uses the class itself as a singleton by making everything static.
 */
public class Globals {
	private static final ImmutableMap<String, Object> builtins;
	private static final Map<String, Object> globals =
			new ConcurrentHashMap<String, Object>(1000);
	private static final AtomicInteger overload = new AtomicInteger();
	static {
		builtins = new ImmutableMap.Builder<String,Object>()
			.put("True", Boolean.TRUE)
			.put("False", Boolean.FALSE)
			.put("Suneido", new SuContainer())
			.put("Add", new Add())
			.put("Adler32", Adler32.clazz)
			.put("And", new And())
			.put("AssertionError", new AssertionError())
			.put("Boolean?", new BooleanQ())
			.put("Built", new Built())
			.put("Cat", new Cat())
			.put("Class?", new ClassQ())
			.put("Cmdline", new Cmdline())
			.put("Construct", new Construct())
			.put("CopyFile", new CopyFile())
			.put("CreateDirectory", new CreateDirectory())
			.put("Cursor", Cursor.clazz)
			.put("Database", Database.singleton)
			.put("Date", DateClass.singleton)
			.put("Date?", new DateQ())
			.put("Delayed", new Delayed())
			.put("DeleteDir", new DeleteDir())
			.put("DeleteFile", new DeleteFile())
			.put("Dir", new Dir())
			.put("DirExists?", new DirExistsQ())
			.put("Display", new Display())
			.put("Div", new Div())
			.put("Dump", new Dump())
			.put("DoWithoutTriggers", new DoWithoutTriggers())
			.put("Eq", new Eq())
			.put("ExePath", new ExePath())
			.put("Exit", new Exit())
			.put("File", SuFile.clazz)
			.put("FileExists?", new FileExistsQ())
			.put("Frame", new Frame())
			.put("Function?", new FunctionQ())
			.put("GetComputerName", new GetComputerName())
			.put("GetCurrentDirectory", new GetCurrentDirectory())
			.put("GetDiskFreeSpace", new GetDiskFreeSpace())
			.put("Getenv", new Getenv())
			.put("GetMacAddress", new GetMacAddress())
			.put("GetTempFileName", new GetTempFileName())
			.put("GetTempPath", new GetTempPath())
			.put("Gt", new Gt())
			.put("Gte", new Gte())
			.put("Libraries", new Libraries())
			.put("Lt", new Lt())
			.put("Lte", new Lte())
			.put("Lucene", lucene())
			.put("Match", new Match())
			.put("Md5", new Md5())
			.put("MemoryArena", new MemoryArena())
			.put("Mod", new Mod())
			.put("MoveFile", new MoveFile())
			.put("Mul", new Mul())
			.put("Neg", new Neg())
			.put("Neq", new Neq())
			.put("NoMatch", new NoMatch())
			.put("Not", new Not())
			.put("NullPointerException", new NullPointerException())
			.put("Number?", new NumberQ())
			.put("Object", new ObjectClass())
			.put("Object?", new ObjectQ())
			.put("OperatingSystem", new OperatingSystem())
			.put("Or", new Or())
			.put("Pack", new suneido.language.builtin.Pack())
			.put("Print", new Print())
			.put("Query1", new Query1())
			.put("QueryFirst", new QueryFirst())
			.put("QueryLast", new QueryLast())
			.put("Random", new Random())
			.put("Record", new RecordClass())
			.put("Record?", new RecordQ())
			.put("RunPiped", RunPiped.clazz)
			.put("Scanner", Scanner.clazz)
			.put("Seq", new Seq())
			.put("ServerEval", new ServerEval())
			.put("ServerIP", new ServerIP())
			.put("ServerPort", new ServerPort())
			.put("Server?", new ServerQ())
			.put("Sleep", new Sleep())
			.put("SetFileWritable", new SetFileWritable())
			.put("SocketClient", SocketClient.clazz)
			.put("SocketServer", SocketServer.singleton)
			.put("String?", new StringQ())
			.put("Sub", new Sub())
			.put("Synchronized", new Synchronized())
			.put("System", new SystemFunction())
			.put("SystemMemory", new SystemMemory())
			.put("Thread", new ThreadFunction())
			.put("Timestamp", new Timestamp())
			.put("Trace", new SuTrace())
			.put("Transaction", SuTransaction.clazz)
			.put("Type", new Type())
			.put("UuidString", new UuidString())
			.put("Unload", new Unload())
			.put("Unpack", new Unpack())
			.put("Unuse", new Unuse())
			.put("UnixTime", new UnixTime())
			.put("Use", new Use())
			.build();
	}

	private Globals() { // no instances
		throw SuException.unreachable();
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
		globals.put(name, x == null ? nonExistent : x); // cache
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

	private static Object lucene() {
		try {
			Class.forName("org.apache.lucene.analysis.Analyzer");
			return Lucene.singleton;
		} catch (ClassNotFoundException e) {
			return NoLucene.singleton;
		}
	}

}
