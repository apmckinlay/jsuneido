/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import suneido.runtime.SuFunction;

public class TestAsm implements Opcodes {

	public static void main(String[] args) throws Exception {

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor cv = new CheckClassAdapter(cw);
		MethodVisitor mv;

		cv.visit(V1_6, ACC_PUBLIC + ACC_SUPER,
				Type.getInternalName(SampleFunction.class), null,
				Type.getInternalName(SuFunction.class), null);
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
