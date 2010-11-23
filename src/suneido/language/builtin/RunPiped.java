package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;

public class RunPiped extends BuiltinClass {

	@Override
	public RunPipedInstance newInstance(Object[] args) {
		return new RunPipedInstance(args);
	}

	private static final FunctionSpec fs =
		new FunctionSpec(array("cmd", "block"), Boolean.FALSE);

	@Override
	public Object call(Object... args) {
		RunPipedInstance rp = new RunPipedInstance(args);
		args = Args.massage(fs, args);
		if (args[1] == Boolean.FALSE)
			return rp;
		try {
			return Ops.call(args[1], rp);
		} finally {
			rp.Close();
		}
	}

	private static class RunPipedInstance extends SuValue {
		private final String cmd;
		private final Process proc;
		private final PrintStream out;
		private final BufferedReader in;

		public RunPipedInstance(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			cmd = Ops.toStr(args[0]);
			List<String> cmdargs = splitcmd(cmd);
			try {
				ProcessBuilder pb = new ProcessBuilder(cmdargs);
				pb.redirectErrorStream(true); // merge stderr into stdout
				proc = pb.start();
				out = new PrintStream(proc.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			} catch (IOException e) {
				throw new SuException("RunPiped failed", e);
			}
		}

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Close")
				return Close(args);
			if (method == "CloseWrite")
				return CloseWrite(args);
			if (method == "ExitValue")
				return ExitValue(args);
			if (method == "Flush")
				return Flush(args);
			if (method == "Read")
				return Read(args);
			if (method == "Readline")
				return Readline(args);
			if (method == "Write")
				return Write(args);
			if (method == "Writeline")
				return Writeline(args);
			throw SuException.methodNotFound("RunPiped", method);
		}

		private Object Close(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			if (out.checkError())
				throw new SuException("RunPiped Close failed");
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				throw new SuException("RunPiped Close failed", e);
			}
			return null;
		}

		private Object CloseWrite(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			if (out.checkError())
				throw new SuException("RunPiped CloseWrite failed");
			out.close();
			return null;
		}

		private Object ExitValue(Object[] args) {
                	Args.massage(FunctionSpec.noParams, args);
                	Close();
                	try {
	                        proc.waitFor();
                        } catch (InterruptedException e) {
	                        throw new SuException("RunPiped ExitValue failed", e);
                        }
                        return proc.exitValue();
                }

		private Object Flush(Object[] args) {
			Args.massage(FunctionSpec.noParams, args);
			out.flush();
			return null;
		}

		private static final FunctionSpec readFS =
			new FunctionSpec(array("nbytes"), 1024);

		private Object Read(Object[] args) {
			args = Args.massage(readFS, args);
			int n = Ops.toInt(args[0]);
			try {
				char buf[] = new char[n];
				int nr = in.read(buf, 0, n);
				return nr == -1 ? false : new String(buf, 0, nr);
			} catch (IOException e) {
				throw new SuException("RunPiped Read failed", e);
			}
		}

		private Object Readline(Object[] args) {
			String s;
			try {
				s = in.readLine();
			} catch (IOException e) {
				throw new SuException("RunPiped Readline failed", e);
			}
			return s == null ? Boolean.FALSE : s;
		}

		private Object Write(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			out.append(Ops.toStr(args[0]));
			return null;
		}

		private Object Writeline(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			out.println(Ops.toStr(args[0]));
			return null;
		}

		@Override
		protected void finalize() throws Throwable {
			Close();
		}

		@Override
		public String toString() {
			return "RunPiped(" + cmd + ")";
		}

	}

	/* temporarily split a single command line string into arguments
	 * in the long run we will switch to passing multiple arguments instead
	 */
	private static List<String> splitcmd(String s) {
		ArrayList<String> args = new ArrayList<String>();
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

}

