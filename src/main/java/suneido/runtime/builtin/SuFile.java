/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.array;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
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

	public static Object Flush(Object self) {
		try {
			((SuFile) self).f.getChannel().force(true);
		} catch (IOException e) {
			throw new SuException("File Flush failed", e);
		}
		return null;
	}

	@Params("nbytes = INTMAX")
	public static Object Read(Object self, Object a) {
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

	public static Object Readline(Object self) {
		try {
			String s = ((SuFile) self).f.readLine();
			return s == null ? Boolean.FALSE : s;
		} catch (IOException e) {
			throw new SuException("File Readline failed", e);
		}
	}

	@Params("offset = set, origin")
	public static Object Seek(Object self, Object a, Object b) {
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

	public static Object Tell(Object self) {
		try {
			return (int) ((SuFile) self).f.getFilePointer();
		} catch (IOException e) {
			throw new SuException("File Tell failed", e);
		}
	}

	@Params("string")
	public static Object Write(Object self, Object a) {
		((SuFile) self).write(Ops.toStr(a));
		return null;
	}

	private void write(String s) {
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

	@Params("string")
	public static Object Writeline(Object self, Object a) {
		((SuFile) self).writeline(Ops.toStr(a));
		return null;
	}

	private void writeline(String s) {
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

	public static Object Close(Object self) {
		((SuFile) self).close();
		return null;
	}

	private void close() {
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
