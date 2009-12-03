package suneido.language.builtin;

import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.io.*;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;

public class FileInstance extends SuValue {
	RandomAccessFile f;

	private static final FunctionSpec fileFS =
			new FunctionSpec(array("filename", "mode"), "r");

	public FileInstance(Object[] args) {
		args = Args.massage(fileFS, args);
		String filename = Ops.toStr(args[0]);
		String mode = Ops.toStr(args[1]);
		if ("w".equals(mode))
			new File(filename).delete();
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
			return close(args);
		if (method == "Flush")
			return flush(args);
		if (method == "Read")
			return read(args);
		if (method == "Readline")
			return readline(args);
		if (method == "Seek")
			return seek(args);
		if (method == "Tell")
			return tell(args);
		if (method == "Write")
			return write(args);
		if (method == "Writeline")
			return writeline(args);
		return userDefined("Files", method).invoke(self, method, args);
	}

	private Object flush(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		try {
			f.getChannel().force(true);
		} catch (IOException e) {
			throw new SuException("File flush io exception", e);
		}
		return null;
	}

	private static final FunctionSpec readFS =
			new FunctionSpec(array("nbytes"), Integer.MAX_VALUE);

	private Object read(Object[] args) {
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
			throw new SuException("File read io exception", e);
		}
	}

	private Object readline(Object[] args) {
		String s;
		try {
			s = f.readLine();
		} catch (IOException e) {
			throw new SuException("File readline io exception", e);
		}
		return s == null ? Boolean.FALSE : s;
	}

	private static final FunctionSpec seekFS =
			new FunctionSpec(array("offset", "origin"), "set");

	private Object seek(Object[] args) {
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
			throw new SuException("File seek io exception", e);
		}
		return null;
	}

	private int tell(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		try {
			return (int) f.getFilePointer();
		} catch (IOException e) {
			throw new SuException("File tell io exception", e);
		}
	}

	private static final FunctionSpec sFS = new FunctionSpec("string");

	private Object write(Object[] args) {
		args = Args.massage(sFS, args);
		try {
			f.writeBytes(Ops.toStr(args[0]));
		} catch (IOException e) {
			throw new SuException("File write io exception", e);
		}
		return null;
	}

	private Object writeline(Object[] args) {
		args = Args.massage(sFS, args);
		try {
			f.writeBytes(Ops.toStr(args[0]));
			f.writeBytes("\r\n");
		} catch (IOException e) {
			throw new SuException("File write io exception", e);
		}
		return null;
	}

	private Object close(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		close();
		return null;
	}

	void close() {
		try {
			f.close();
		} catch (IOException e) {
		}
	}

}
