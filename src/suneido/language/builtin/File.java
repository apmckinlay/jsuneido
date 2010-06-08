package suneido.language.builtin;

import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.io.*;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;

public class File extends BuiltinClass {

	@Override
	public Instance newInstance(Object[] args) {
		return new Instance(args);
	}

	private static final FunctionSpec fileFS =
			new FunctionSpec(array("filename", "mode", "block"),
					"r", Boolean.FALSE);

	@Override
	public Object call(Object... args) {
		Instance f = newInstance(args);
		args = Args.massage(fileFS, args);
		Object block = args[2];
		if (block == Boolean.FALSE)
			return f;
		try {
			return Ops.call(block, f);
		} finally {
			f.Close();
		}
	}

	private static class Instance extends SuValue {
		private final String filename;
		private final String mode;
		private final RandomAccessFile f;

		private static final FunctionSpec fileFS =
				new FunctionSpec(array("filename", "mode"), "r");

		public Instance(Object[] args) {
			args = Args.massage(fileFS, args);
			filename = Ops.toStr(args[0]);
			mode = Ops.toStr(args[1]);
			if ("w".equals(mode))
				new java.io.File(filename).delete();
			boolean seekEnd = false;
			if (mode.startsWith("a"))
				seekEnd = true;
			try {
				f = new RandomAccessFile(filename, mode.equals("r") ? "r" : "rw");
			} catch (FileNotFoundException e) {
				throw new SuException("File: can't open '" + filename
						+ "' in mode '" + mode + "'");
			}
			if (seekEnd)
				try {
					f.seek(f.length());
				} catch (IOException e) {
					throw new SuException("File io exception", e);
				}
		}

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Close")
				return Close(args);
			if (method == "Flush")
				return Flush(args);
			if (method == "Read")
				return Read(args);
			if (method == "Readline")
				return Readline(args);
			if (method == "Seek")
				return Seek(args);
			if (method == "Tell")
				return Tell(args);
			if (method == "Write")
				return Write(args);
			if (method == "Writeline")
				return Writeline(args);
			return userDefined("Files", self, method, args);
		}

		private Object Flush(Object[] args) {
			Args.massage(FunctionSpec.noParams, args);
			try {
				f.getChannel().force(true);
			} catch (IOException e) {
				throw new SuException("File Flush failed", e);
			}
			return null;
		}

		private static final FunctionSpec readFS =
				new FunctionSpec(array("nbytes"), Integer.MAX_VALUE);

		private Object Read(Object[] args) {
			args = Args.massage(readFS, args);
			int n = Ops.toInt(args[0]);
			long remaining;
			try {
				remaining = f.length() - f.getFilePointer();
				if (remaining == 0)
					return Boolean.FALSE;
				if (n > remaining)
					n = (int) remaining;
				byte buf[] = new byte[n];
				f.readFully(buf);
				return new String(buf);
			} catch (IOException e) {
				throw new SuException("File Read failed", e);
			}
		}

		private Object Readline(Object[] args) {
			String s;
			try {
				s = f.readLine();
			} catch (IOException e) {
				throw new SuException("File Readline failed", e);
			}
			return s == null ? Boolean.FALSE : s;
		}

		private static final FunctionSpec seekFS =
				new FunctionSpec(array("offset", "origin"), "set");

		private Object Seek(Object[] args) {
			args = Args.massage(seekFS, args);
			long offset = Ops.toInt(args[0]);
			String origin = Ops.toStr(args[1]);
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

		private int Tell(Object[] args) {
			Args.massage(FunctionSpec.noParams, args);
			try {
				return (int) f.getFilePointer();
			} catch (IOException e) {
				throw new SuException("File Tell failed", e);
			}
		}

		private Object Write(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			try {
				f.writeBytes(Ops.toStr(args[0]));
			} catch (IOException e) {
				throw new SuException("File Write failed", e);
			}
			return null;
		}

		private Object Writeline(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			try {
				f.writeBytes(Ops.toStr(args[0]));
				f.writeBytes("\r\n");
			} catch (IOException e) {
				throw new SuException("File Writeline failed", e);
			}
			return null;
		}

		private Object Close(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			try {
				f.close();
			} catch (IOException e) {
				throw new SuException("File Close failed", e);
			}
			return null;
		}

		@Override
		protected void finalize() throws Throwable {
			Close();
		}

		@Override
		public String toString() {
			return "File(" + filename + ", " + mode + ")";
		}

	}
}
