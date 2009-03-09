package suneido.language;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.*;

public class AsmGenerator implements Generator<Object> {
	private ClassWriter cw;
	private MethodVisitor mv;
	private Label startLabel;

	public Object and(Object expr1, Object expr2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object argumentList(Object list, String keyword, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object assignment(Object term, Token op, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object atArgument(String n, Object expr) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object block(Object params, Object statements) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object bool(boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object breakStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object caseValues(Object values, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object catcher(String variable, String pattern, Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object classConstant(String base, Object members) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object conditional(Object primaryExpression, Object first,
			Object second) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object constant(Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object continueStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object date(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object dowhileStatement(Object statement, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object expressionList(Object list, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object expressionStatement(Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object forInStatement(String var, Object expr, Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object foreverStatement(Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object function(Object params, Object compound) {
		Label endLabel = new Label();
		mv.visitLabel(endLabel);
		mv.visitLocalVariable("this", "LSampleFunction;", null, startLabel,
				endLabel, 0);
		mv.visitLocalVariable("args", "[LSuValue;", null, startLabel, endLabel,
				1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cw.visitEnd();

		return cw.toByteArray();
	}

	public Object functionCall(Object function, Object arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object identifier(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object ifStatement(Object expression, Object t, Object f) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object in(Object expression, Object constant) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object member(Object term, String identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object memberDefinition(Object name, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object memberList(Object list, Object member) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object newExpression(Object term, Object arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object number(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object object(ObjectOrRecord which, Object members) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object or(Object expr1, Object expr2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "SampleFunction", null,
				"SuFunction", null);

		cw.visitSource("function.suneido", null);

		asm_init();

		mv = cw.visitMethod(0, "invoke", "([LSuValue;)LSuValue;", null, null);
		startLabel = new Label();
		mv.visitLabel(startLabel);
		return null;
	}

	private void asm_init() {
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "SuFunction", "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "LSampleFunction;", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		mv = null;
	}

	public Object postIncDec(Token incdec, Object lvalue) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object preIncDec(Token incdec, Object lvalue) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object returnStatement(Object expression) {
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		return null;
	}

	public Object self() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object statementList(Object n, Object next) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object string(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object subscript(Object term, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object switchCases(Object cases, Object values, Object statements) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object switchStatement(Object expression, Object cases) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object symbol(String identifier) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object throwStatement(Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object tryStatement(Object tryStatement, Object catcher) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object unaryExpression(Token op, Object expression) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object whileStatement(Object expression, Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

}
