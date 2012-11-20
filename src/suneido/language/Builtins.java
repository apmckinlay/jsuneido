/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.Map;

import suneido.SuContainer;
import suneido.Suneido;
import suneido.language.builtin.*;
import suneido.language.builtin.AssertionError;
import suneido.language.builtin.NullPointerException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

public class Builtins {
	static final ImmutableMap<String, Object> builtins;

	public static Object get(String name) {
		return builtins.get(name);
	}

	static {
		builtins = new ImmutableMap.Builder<String,Object>()
			.put("True", Boolean.TRUE)
			.put("False", Boolean.FALSE)
			.put("Suneido", new SuContainer())
			.put("Add", function(Add.class))
			.put("Adler32", Adler32.clazz)
			.put("And", function(And.class))
			.put("AssertionError", function(AssertionError.class))
			.put("Boolean?", function(BooleanQ.class))
			.put("Built", function(Built.class))
			.put("Cat", function(Cat.class))
			.put("Class?", function(ClassQ.class))
			.put("Cmdline", function(Cmdline.class))
			.put("Construct", function(Construct.class))
			.put("CopyFile", function(CopyFile.class))
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
			.put("GetComputerName", function(GetComputerName.class))
			.put("GetCurrentDirectory", function(GetCurrentDirectory.class))
			.put("GetDiskFreeSpace", new GetDiskFreeSpace())
			.put("Getenv", new Getenv())
			.put("GetMacAddress", new GetMacAddress())
			.put("GetTempFileName", new GetTempFileName())
			.put("GetTempPath", function(GetTempPath.class))
			.put("Gt", new Gt())
			.put("Gte", new Gte())
			.put("Libraries", function(Libraries.class))
			.put("Lt", new Lt())
			.put("Lte", new Lte())
			.put("Lucene", lucene())
			.put("Match", new Match())
			.put("Md5", new Md5())
			.put("MemoryArena", function(MemoryArena.class))
			.put("Mod", new Mod())
			.put("MoveFile", new MoveFile())
			.put("Mul", new Mul())
			.put("Neg", new Neg())
			.put("Neq", new Neq())
			.put("NoMatch", new NoMatch())
			.put("Not", new Not())
			.put("NullPointerException", function(NullPointerException.class))
			.put("Number?", new NumberQ())
			.put("Object", new ObjectClass())
			.put("Object?", new ObjectQ())
			.put("OperatingSystem", function(OperatingSystem.class))
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
			.put("ServerIP", function(ServerIP.class))
			.put("ServerPort", function(ServerPort.class))
			.put("Server?", function(ServerQ.class))
			.put("Sleep", new Sleep())
			.put("SetFileWritable", new SetFileWritable())
			.put("SocketClient", SocketClient.clazz)
			.put("SocketServer", SocketServer.singleton)
			.put("String?", new StringQ())
			.put("Sub", new Sub())
			.put("Synchronized", new Synchronized())
			.put("System", new SystemFunction())
			.put("SystemMemory", function(SystemMemory.class))
			.put("Thread", new ThreadFunction())
			.put("Timestamp", function(Timestamp.class))
			.put("Trace", new SuTrace())
			.put("Transaction", SuTransaction.clazz)
			.put("Type", new Type())
			.put("Unload", new Unload())
			.put("Unpack", new Unpack())
			.put("Unuse", new Unuse())
			.put("UnixTime", function(UnixTime.class))
			.put("Use", new Use())
			.put("UuidString", function(UuidString.class))
			.build();
	}

	private static Object lucene() {
		try {
			Class.forName("org.apache.lucene.analysis.Analyzer");
			return Lucene.singleton;
		} catch (ClassNotFoundException e) {
			Suneido.errlog("ERROR: lucene not found");
			return NoLucene.singleton;
		}
	}

	private static SuCallable function(Class<?> c) {
		Map<String, SuCallable> ms = BuiltinMethods.functions(c);
		return Iterables.getOnlyElement(ms.values());
	}

}
