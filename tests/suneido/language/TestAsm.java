package suneido.language;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

public class TestAsm implements Opcodes {

	public static void main(String[] args) throws Exception {

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor cv = new CheckClassAdapter(cw);
		MethodVisitor mv;

		cv.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "suneido/language/SampleFunction",
				null, "suneido/language/SuFunction", null);
		mv = cv.visitMethod(ACC_PUBLIC + ACC_VARARGS, "call",
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;", null, l0, l3, 0);
		mv.visitLocalVariable("self", "Ljava/lang/Object;", null, l0, l3, 1);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l3, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cv.visitEnd();
	}

}
