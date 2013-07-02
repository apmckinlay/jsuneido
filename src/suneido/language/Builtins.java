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
import com.google.common.collect.ImmutableSet;
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
			.put("BuiltinNames", function(BuiltinNames.class))
			.put("Cat", function(Cat.class))
			.put("Class?", function(ClassQ.class))
			.put("Cmdline", function(Cmdline.class))
			.put("Construct", function(Construct.class))
			.put("CopyFile", function(CopyFile.class))
			.put("CreateDirectory", function(CreateDirectory.class))
			.put("Cursor", Cursor.clazz)
			.put("Database", Database.singleton)
			.put("Date", DateClass.singleton)
			.put("Date?", function(DateQ.class))
			.put("Delayed", function(Delayed.class))
			.put("DeleteDir", function(DeleteDir.class))
			.put("DeleteFile", function(DeleteFile.class))
			.put("Dir", function(Dir.class))
			.put("DirExists?", function(DirExistsQ.class))
			.put("Display", function(Display.class))
			.put("Div", function(Div.class))
			.put("Dump", function(Dump.class))
			.put("DoWithoutTriggers", function(DoWithoutTriggers.class))
			.put("Eq", function(Eq.class))
			.put("ExePath", function(ExePath.class))
			.put("Exit", function(Exit.class))
			.put("File", SuFile.clazz)
			.put("FileExists?", function(FileExistsQ.class))
			.put("Frame", function(Frame.class))
			.put("Function?", function(FunctionQ.class))
			.put("GetComputerName", function(GetComputerName.class))
			.put("GetCurrentDirectory", function(GetCurrentDirectory.class))
			.put("GetDiskFreeSpace", function(GetDiskFreeSpace.class))
			.put("Getenv", function(Getenv.class))
			.put("GetMacAddress", function(GetMacAddress.class))
			.put("GetMacAddresses", function(GetMacAddresses.class))
			.put("GetTempFileName", function(GetTempFileName.class))
			.put("GetTempPath", function(GetTempPath.class))
			.put("Gt", function(Gt.class))
			.put("Gte", function(Gte.class))
			.put("Libraries", function(Libraries.class))
			.put("Lt", function(Lt.class))
			.put("Lte", function(Lte.class))
			.put("Lucene", lucene())
			.put("Match", function(Match.class))
			.put("Md5", new Digest.Instance("MD5"))
			.put("MemoryArena", function(MemoryArena.class))
			.put("Mod", function(Mod.class))
			.put("MoveFile", function(MoveFile.class))
			.put("Mul", function(Mul.class))
			.put("Neg", function(Neg.class))
			.put("Neq", function(Neq.class))
			.put("NoMatch", function(NoMatch.class))
			.put("Not", function(Not.class))
			.put("NullPointerException", function(NullPointerException.class))
			.put("Number?", function(NumberQ.class))
			.put("Object", new ObjectClass())
			.put("Object?", function(ObjectQ.class))
			.put("OperatingSystem", function(OperatingSystem.class))
			.put("Or", function(Or.class))
			.put("Pack", function(suneido.language.builtin.Pack.class))
			.put("Print", function(Print.class))
			.put("Query1", new Query1())
			.put("QueryFirst", new QueryFirst())
			.put("QueryLast", new QueryLast())
			.put("Random", function(Random.class))
			.put("Record", new RecordClass())
			.put("Record?", function(RecordQ.class))
			.put("RunPiped", RunPiped.clazz)
			.put("Scanner", Scanner.clazz)
			.put("Seq", new Seq())
			.put("ServerEval", function(ServerEval.class))
			.put("ServerIP", function(ServerIP.class))
			.put("ServerPort", function(ServerPort.class))
			.put("Server?", function(ServerQ.class))
			.put("Sha1", new Digest.Instance("SHA-1"))
			.put("Sleep", function(Sleep.class))
			.put("SetFileWritable", function(SetFileWritable.class))
			.put("SocketClient", SocketClient.clazz)
			.put("SocketServer", SocketServer.singleton)
			.put("String?", function(StringQ.class))
			.put("Sub", function(Sub.class))
			.put("Synchronized", function(Synchronized.class))
			.put("System", function(SystemFunction.class))
			.put("SystemMemory", function(SystemMemory.class))
			.put("Thread", function(ThreadFunction.class))
			.put("Timestamp", function(Timestamp.class))
			.put("Trace", function(SuTrace.class))
			.put("Transaction", SuTransaction.clazz)
			.put("Type", function(Type.class))
			.put("Unload", function(Unload.class))
			.put("Unpack", function(Unpack.class))
			.put("Unuse", function(Unuse.class))
			.put("UnixTime", function(UnixTime.class))
			.put("Use", function(Use.class))
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

	public static ImmutableSet<String> builtinNames() {
		return builtins.keySet();
	}

}
