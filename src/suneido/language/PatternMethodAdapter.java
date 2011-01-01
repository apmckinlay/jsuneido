package suneido.language;

import org.objectweb.asm.*;

public abstract class PatternMethodAdapter extends MethodAdapter {

	public PatternMethodAdapter(MethodVisitor mv) {
		super(mv);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		visitInsn();
		mv.visitFrame(type, nLocal, local, nStack, stack);
	}

	@Override
	public void visitInsn(int opcode) {
		visitInsn();
		mv.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		visitInsn();
		mv.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		visitInsn();
		mv.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		visitInsn();
		mv.visitTypeInsn(opcode, desc);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		visitInsn();
		mv.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc) {
		visitInsn();
		mv.visitMethodInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		visitInsn();
		mv.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(Label label) {
		visitInsn();
		mv.visitLabel(label);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		visitInsn();
		mv.visitLdcInsn(cst);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		visitInsn();
		mv.visitIincInsn(var, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt,
			Label labels[]) {
		visitInsn();
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
		visitInsn();
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		visitInsn();
		mv.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		visitInsn();
		mv.visitMaxs(maxStack, maxLocals);
	}

	protected abstract void visitInsn();
}