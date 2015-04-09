/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.array;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
import suneido.util.Util;

public class RunPiped extends SuValue {
	private final String cmd;
	private final Process proc;
	private final PrintStream out;
	private final InputStream in;
	private static final BuiltinMethods methods = new BuiltinMethods(RunPiped.class);

	public RunPiped(String cmd) {
		this.cmd = cmd;

		List<String> cmdargs = splitcmd(cmd);
		try {
			ProcessBuilder pb = new ProcessBuilder(cmdargs);
			pb.redirectErrorStream(true); // merge stderr into stdout
			proc = pb.start();
			out = new PrintStream(proc.getOutputStream(), true);
			in = new BufferedInputStream(proc.getInputStream());

		} catch (IOException e) {
			throw new SuException("RunPiped failed", e);
		}
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static Object Close(Object self) {
		((RunPiped) self).close();
		return null;
	}

	private void close() {
		if (out.checkError())
			throw new SuException("RunPiped Close failed");
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			throw new SuException("RunPiped Close failed", e);
		}
	}

	public static Object CloseWrite(Object self) {
		RunPiped rp = ((RunPiped) self);
		if (rp.out.checkError())
			throw new SuException("RunPiped CloseWrite failed");
		rp.out.close();
		return null;
	}

	public static Object ExitValue(Object self) {
		RunPiped rp = ((RunPiped) self);
		rp.close();
		try {
			rp.proc.waitFor();
		} catch (InterruptedException e) {
			throw new SuException("RunPiped ExitValue failed", e);
		}
		return rp.proc.exitValue();
	}

	public static Object Flush(Object self) {
		((RunPiped) self).out.flush();
		return null;
	}

	@Params("nbytes = 1024")
	public static Object Read(Object self, Object a) {
		int n = Ops.toInt(a);
		try {
			byte buf[] = new byte[n];
			int nr = ((RunPiped) self).in.read(buf, 0, n);
			return nr == -1 ? false : Util.bytesToString(buf, nr);
		} catch (IOException e) {
			throw new SuException("RunPiped Read failed", e);
		}
	}

	public static Object Readline(Object self) {
		try {
			return ((RunPiped) self).readLine();
		} catch (IOException e) {
			throw new SuException("RunPiped Readline failed", e);
		}
	}

	// NOTE: Readline should be consistent across file, socket, and runpiped
	private Object readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = in.read();
			if (c == -1)
				if (sb.length() == 0)
					return Boolean.FALSE;
				else
					break ;
			if (c == '\n')
				break ;
			if (sb.length() < Util.MAX_LINE)
				sb.append((char) c);
		}
		return Util.toLine(sb);
	}

	@Params("string")
	public static Object Write(Object self, Object a) {
		((RunPiped) self).out.append(Ops.toStr(a));
		return null;
	}

	@Params("string")
	public static Object Writeline(Object self, Object a) {
		((RunPiped) self).out.println(Ops.toStr(a));
		return null;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public String toString() {
		return "RunPiped(" + cmd + ")";
	}

	/** split single command line string into arguments */
	private static List<String> splitcmd(String s) {
		ArrayList<String> args = new ArrayList<>();
		while (true) {
			s = s.trim();
			if (s.isEmpty())
				break;
			char delim = ' ';
			if (s.charAt(0) == '"') {
				s = s.substring(1);
				delim = '"';
			}
			int i = s.indexOf(delim);
			args.add(i == -1 ? s : s.substring(0, i));
			if (i == -1 || i + 1 > s.length())
				break;
			s = s.substring(i + 1);
		}
		return args;
	}

	public static final BuiltinClass clazz = new BuiltinClass("RunPiped",
			FunctionSpec.STRING) {
		@Override
		public RunPiped newInstance(Object... args) {
			args = Args.massage(FunctionSpec.STRING, args);
			return new RunPiped(Ops.toStr(args[0]));
		}

		FunctionSpec fs = new FunctionSpec(array("cmd", "block"), Boolean.FALSE);

		@Override
		public Object call(Object... args) {
			args = Args.massage(fs, args);
			RunPiped rp = new RunPiped(Ops.toStr(args[0]));
			if (args[1] == Boolean.FALSE)
				return rp;
			try {
				return Ops.call(args[1], rp);
			} finally {
				rp.close();
			}
		}
	};

}

