/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.*;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;
import suneido.util.Util;

public class SuFile extends SuValue {
	private final String filename;
	private final String mode;
	private final boolean append;
	private RandomAccessFile f;
	private static final BuiltinMethods methods = new BuiltinMethods(SuFile.class);

	public SuFile(String filename, String mode) {
		this.filename = filename;
		this.mode = mode;
		File file = new File(filename);
		if ("w".equals(mode) && file.exists()) {
			if (! file.delete())
				throw new SuException("File: can't delete " + filename);
			// can't rely on RandomAccessFile to create file
			// it fails intermittently on Windows
			// 1000.Times() { PutFile("tester", "") }
			try {
				if (! file.createNewFile())
					throw new SuException("File: can't create " + filename);
			} catch (IOException e) {
				throw new SuException("File: can't create '" + filename
						+ "' in mode '" + mode + "'", e);
			}
		}
		append = mode.startsWith("a");
		try {
			f = new RandomAccessFile(filename, mode.equals("r") ? "r" : "rw");
		} catch (FileNotFoundException e) {
			throw new SuException("File: can't open '" + filename
					+ "' in mode '" + mode + "'", e);
		}
		if (append)
			try {
				f.seek(f.length());
			} catch (IOException e) {
				throw new SuException("File io exception", e);
			}
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static class Flush extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			try {
				((SuFile) self).f.getChannel().force(true);
			} catch (IOException e) {
				throw new SuException("File Flush failed", e);
			}
			return null;
		}
	}

	public static class Read extends SuMethod1 {
		{ params = new FunctionSpec(array("nbytes"), Integer.MAX_VALUE); }
		@Override
		public Object eval1(Object self, Object a) {
			RandomAccessFile f = ((SuFile) self).f;
			int n = Ops.toInt(a);
			long remaining;
			try {
				remaining = f.length() - f.getFilePointer();
				if (remaining == 0)
					return Boolean.FALSE;
				if (n > remaining)
					n = (int) remaining;
				byte buf[] = new byte[n];
				f.readFully(buf);
				return Util.bytesToString(buf);
			} catch (IOException e) {
				throw new SuException("File Read failed", e);
			}
		}
	}

	public static class Readline extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			try {
				String s = ((SuFile) self).f.readLine();
				return s == null ? Boolean.FALSE : s;
			} catch (IOException e) {
				throw new SuException("File Readline failed", e);
			}
		}
	}

	public static class Seek extends SuMethod2 {
		{ params = new FunctionSpec(array("offset", "origin"), "set"); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			long offset = Ops.toInt(a);
			String origin = Ops.toStr(b);
			RandomAccessFile f = ((SuFile) self).f;
			try {
				if (origin.equals("cur"))
					offset += f.getFilePointer();
				else if (origin.equals("end"))
					offset += f.length();
				else if (!origin.equals("set"))
					throw new SuException(
							"file.Seek: origin must be 'set', 'end', or 'cur'");
				if (offset < 0)
					offset = 0;
				f.seek(offset);
			} catch (IOException e) {
				throw new SuException("File Seek failed", e);
			}
			return null;
		}
	}

	public static class Tell extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			try {
				return (int) ((SuFile) self).f.getFilePointer();
			} catch (IOException e) {
				throw new SuException("File Tell failed", e);
			}
		}
	}

	public static class Write extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			((SuFile) self).write(Ops.toStr(a));
			return null;
		}
	}

	public void write(String s) {
		try {
			synchronized (SuFile.class) {
				if (append)
					f.seek(f.length());
				f.writeBytes(s);
			}
		} catch (IOException e) {
			throw new SuException("File Write failed", e);
		}
	}

	public static class Writeline extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			((SuFile) self).writeline(Ops.toStr(a));
			return null;
		}
	}

	public void writeline(String s) {
		try {
			synchronized (SuFile.class) {
				if (append)
					f.seek(f.length());
				f.writeBytes(s);
				f.writeBytes("\r\n");
			}
		} catch (IOException e) {
			throw new SuException("File Write failed", e);
		}
	}

	public static class Close extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			((SuFile) self).close();
			return null;
		}
	}

	public void close() {
		try {
			if (f != null) {
				f.close();
				f = null;
			}
		} catch (IOException e) {
			throw new SuException("File Close failed", e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	@Override
	public String toString() {
		return "File(" + filename + ", " + mode + ")";
	}

	public static final BuiltinClass clazz = new BuiltinClass() {
		FunctionSpec newFS = new FunctionSpec(array("filename", "mode"), "r");
		@Override
		public SuFile newInstance(Object... args) {
			args = Args.massage(newFS, args);
			return new SuFile(Ops.toStr(args[0]), Ops.toStr(args[1]));
		}
		FunctionSpec callFS = new FunctionSpec(
				array("filename", "mode", "block"), "r", Boolean.FALSE);
		@Override
		public Object call(Object... args) {
			args = Args.massage(callFS, args);
			SuFile f = new SuFile(Ops.toStr(args[0]), Ops.toStr(args[1]));
			Object block = args[2];
			if (block == Boolean.FALSE)
				return f;
			try {
				return Ops.call(block, f);
			} finally {
				f.close();
			}
		}
	};

}
