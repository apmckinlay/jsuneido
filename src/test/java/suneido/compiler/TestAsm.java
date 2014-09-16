/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import suneido.runtime.BlockReturnException;
import suneido.runtime.Ops;
import suneido.runtime.SuCallBase;

public class TestAsm implements Opcodes {

	private static class SampleFunction extends SuCallBase {
		private static final Object c;

		static {
			List<Object> constants = ClassGen.shareConstants.get();
			c = constants.get(0);
		}

		@Override
		public Object call(Object... args) {
			try {
				throw Ops.blockReturnException(c, 123);
			} catch (BlockReturnException e) {
				return Ops.blockReturnHandler(e, 123);
			}
		}

	}

	public static void main(String[] args) throws Exception {

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor cv = new CheckClassAdapter(cw);
		MethodVisitor mv;

		cv.visit(V1_6, ACC_PUBLIC + ACC_SUPER,
				Type.getInternalName(SampleFunction.class), null,
				Type.getInternalName(SuCallBase.class), null);
		mv = cv.visitMethod(ACC_PUBLIC + ACC_VARARGS, "call",
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLocalVariable("this", Type.getDescriptor(SampleFunction.class), null, l0, l3, 0);
		mv.visitLocalVariable("self", "Ljava/lang/Object;", null, l0, l3, 1);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l3, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cv.visitEnd();
	}

}
