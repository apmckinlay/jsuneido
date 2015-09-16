/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import suneido.SuContainer;
import suneido.jsdi.Buffer;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.com.COMobject;
import suneido.runtime.builtin.*;
import suneido.util.Errlog;

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
			.put("AssertionError",
						function(suneido.runtime.builtin.AssertionError.class))
			.put("AstParse", function(AstParse.class))
			.put("Boolean?", function(BooleanQ.class))
			.put("Buffer", Buffer.clazz)
			.put("Buffer?", function(BufferQ.class))
			.put("Built", function(Built.class))
			.put("BuiltinNames", function(BuiltinNames.class))
			.put("COMobject", COMobject.clazz)
			.put("Callbacks", function(ThunkManager.Callbacks.class))
			.put("Cat", function(Cat.class))
			.put("CircLog", function(CircLog.class))
			.put("Class?", function(ClassQ.class))
			.put("ClearCallback", function(ThunkManager.ClearCallback.class))
			.put("Cmdline", function(Cmdline.class))
			.put("Construct", function(Construct.class))
			.put("CopyFile", function(CopyFile.class))
			.put("CreateDirectory", function(CreateDirectory.class))
			.put("Cursor", Cursor.clazz)
			.put("Database", Database.singleton)
			.put("Date", DateClass.singleton)
			.put("Date?", function(DateQ.class))
			.put("DeleteDir", function(DeleteDir.class))
			.put("DeleteFile", function(DeleteFile.class))
			.put("Dir", function(Dir.class))
			.put("DirExists?", function(DirExistsQ.class))
			.put("Display", function(Display.class))
			.put("Div", function(Div.class))
			.put("Dll?", function(DllQ.class))
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
			.put("GlobalsInfo", function(GlobalsInfo.class))
			.put("Gt", function(Gt.class))
			.put("Gte", function(Gte.class))
			.put("JSDI", JSDI.singleton)
			.put("Libraries", function(Libraries.class))
			.put("Locals", function(Locals.class))
			.put("Lt", function(Lt.class))
			.put("Lte", function(Lte.class))
			.put("Lucene", lucene())
			.put("Match", function(Match.class))
			.put("Md5", new Digest.Instance("MD5", "Md5"))
			.put("MemoryArena", function(MemoryArena.class))
			.put("Mod", function(Mod.class))
			.put("MoveFile", function(MoveFile.class))
			.put("Mul", function(Mul.class))
			.put("Name", function(Name.class))
			.put("Neg", function(Neg.class))
			.put("Neq", function(Neq.class))
			.put("NoMatch", function(NoMatch.class))
			.put("Not", function(Not.class))
			.put("NullPointerException",
						function(suneido.runtime.builtin.NullPointerException.class))
			.put("Number?", function(NumberQ.class))
			.put("Object", new ObjectClass())
			.put("Object?", function(ObjectQ.class))
			.put("OperatingSystem", function(OperatingSystem.class))
			.put("Or", function(Or.class))
			.put("Pack", function(suneido.runtime.builtin.Pack.class))
			.put("PrintStdout", function(PrintStdout.class))
			.put("Query1", new Query1())
			.put("QueryFirst", new QueryFirst())
			.put("QueryLast", new QueryLast())
			.put("QueryScanner", QueryScanner.clazz)
			.put("Random", function(Random.class))
			.put("Record", new RecordClass())
			.put("Record?", function(RecordQ.class))
			.put("RunPiped", RunPiped.clazz)
			.put("Scanner", Scanner.clazz)
			.put("Scheduled", function(Scheduled.class))
			.put("Seq", new Seq())
			.put("Sequence", function(SuSequence.class))
			.put("ServerEval", function(ServerEval.class))
			.put("ServerIP", function(ServerIP.class))
			.put("ServerPort", function(ServerPort.class))
			.put("Server?", function(ServerQ.class))
			.put("SetFileWritable", function(SetFileWritable.class))
			.put("Sha1", new Digest.Instance("SHA-1", "Sha1"))
			.put("Sleep", function(Sleep.class))
			.put("SocketClient", SocketClient.clazz)
			.put("SocketServer", SocketServer.singleton)
			.put("Spawn", function(Spawn.class))
			.put("String?", function(StringQ.class))
			.put("Struct?", function(StructQ.class))
			.put("Sub", function(Sub.class))
			.put("SymbolsInfo", function(SymbolsInfo.class))
			.put("Synchronized", function(Synchronized.class))
			.put("System", function(SystemFunction.class))
			.put("SystemMemory", function(SystemMemory.class))
			.put("Thread", function(ThreadFunction.class))
			.put("ThreadCount", function(ThreadCount.class))
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
			.put("Zlib", Zlib.singleton)
			.build();
	}

	private static Object lucene() {
		try {
			Class.forName("org.apache.lucene.analysis.standard.StandardAnalyzer");
			return Lucene.singleton;
		} catch (ClassNotFoundException e) {
			Errlog.errlog("ERROR: lucene not found");
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
