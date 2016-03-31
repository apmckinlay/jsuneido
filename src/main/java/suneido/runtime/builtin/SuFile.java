/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.array;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;

import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
import suneido.util.Util;

public class SuFile extends SuValue {
	private final String filename;
	private final String mode;
	private final boolean append;
	private RandomAccessFile f;
	private static final BuiltinMethods methods = new BuiltinMethods("file", SuFile.class);

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

	// NOTE: Readline should be consistent across file, socket, and runpiped
	public static Object Readline(Object self) {
		RandomAccessFile f = ((SuFile) self).f;
		// our own implementation to get Suneido's behavior
		try {
			StringBuilder sb = new StringBuilder();
			while (true) {
				int c = f.read();
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
		} catch (IOException e) {
			throw new SuException("File Readline failed", e);
		}
	}

	@Params("offset = set, origin")
	public static Object Seek(Object self, Object a, Object b) {
		long offset = toLong(a);
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

	private static long toLong(Object x) {
		if (x instanceof BigDecimal)
			return ((BigDecimal) x).longValueExact();
		if (x instanceof BigInteger)
			return ((BigInteger) x).longValueExact();
		if (x instanceof Number)
			return ((Number) x).longValue();
		throw new SuException("can't convert " + Ops.typeName(x) + " to long");
	}

	public static Object Tell(Object self) {
		try {
			return ((SuFile) self).f.getFilePointer();
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

	private static final FunctionSpec newFS = new FunctionSpec(array(
			"filename", "mode"), "r");

	private static final FunctionSpec callFS = new FunctionSpec(array(
			"filename", "mode", "block"), "r", Boolean.FALSE);

	public static final BuiltinClass clazz = new BuiltinClass("File", newFS) {
		@Override
		public SuFile newInstance(Object... args) {
			args = Args.massage(newFS, args);
			return new SuFile(Ops.toStr(args[0]), Ops.toStr(args[1]));
		}

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
