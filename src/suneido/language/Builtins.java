/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuContainer;
import suneido.language.builtin.*;
import suneido.language.builtin.AssertionError;
import suneido.language.builtin.NullPointerException;

import com.google.common.collect.ImmutableMap;

class Builtins {
	static final ImmutableMap<String, Object> builtins;

	static Object get(String name) {
		return builtins.get(name);
	}

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

	private static Object lucene() {
		try {
			Class.forName("org.apache.lucene.analysis.Analyzer");
			return Lucene.singleton;
		} catch (ClassNotFoundException e) {
			return NoLucene.singleton;
		}
	}

}
