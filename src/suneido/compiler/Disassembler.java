package suneido.compiler;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.runtime.SuCallable;

/**
 * <p>
 * Disassembles a Java class using ASM.
 * </p>
 *
 * <p>
 * This class is used to implement the Suneido built-in
 * {@code function.Disasm()}
 * </p>
 *
 * @author Victor Schappert
 * @since 20140919
 */
public final class Disassembler {

	//
	// PUBLIC METHODS
	//

	/**
	 * Disassembles a {@link SuCallable} callable Suneido value.
	 *
	 * @param callable
	 *            Callable value
	 * @return Disassembly output as a {@link String}, if available, or the
	 *         value {@link Boolean#FALSE} otherwise
	 * @see #disassemble(Class)
	 */
	public static Object disassemble(SuCallable callable) {
		byte[] byteCode = callable.byteCode();
		if (null == byteCode) {
			return Boolean.FALSE;
		} else {
			return disassemble(byteCode);
		}
	}

	//
	// INTERNALS
	//

	private static String disassemble(byte[] byteCode) {
		StringWriter sw = new StringWriter(4096);
		PrintWriter pw = new PrintWriter(sw);
		TraceClassVisitor v = new TraceClassVisitor(pw);
		ClassReader r = new ClassReader(byteCode);
		r.accept(v, 0);
		return sw.toString();
	}

	//
	// DON'T INSTANTIATE
	//

	private Disassembler() {

	}
}
