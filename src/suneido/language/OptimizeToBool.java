package suneido.language;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import org.objectweb.asm.MethodVisitor;

// TODO optimize "not"

/**
 * optimizes sequences like lt,toBool => lt_
 * @author Andrew McKinlay
 */
public class OptimizeToBool extends PatternMethodAdapter {
	private String compare_seen = null;

	public OptimizeToBool(MethodVisitor mv) {
		super(mv);
	}

	@Override
	public void visitInsn() {
		if (compare_seen != null) {
			mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
					compare_seen,
					"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
			compare_seen = null;
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (compare_seen != null)
			if (isToBool(opcode, owner, name, desc)) {
				outputIntCompare();
				compare_seen = null;
				return; // omit toBool
			}
		visitInsn();
		if (isCompare(opcode, owner, name, desc)) {
			compare_seen = name;
			return; // don't output till we see if toBool follows
		}
		mv.visitMethodInsn(opcode, owner, name, desc);
	}

	boolean isCompare(int opcode, String owner, String name, String desc) {
		return opcode == INVOKESTATIC
				&& owner.equals("suneido/language/Ops")
				&& desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
	}

	boolean isToBool(int opcode, String owner, String name, String desc) {
		return opcode == INVOKESTATIC
				&& owner.equals("suneido/language/Ops")
				&& name.equals("toBool")
				&& desc.equals("(Ljava/lang/Object;)I");
	}

	void outputIntCompare() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				compare_seen + "_",
				"(Ljava/lang/Object;Ljava/lang/Object;)Z");
	}

}
