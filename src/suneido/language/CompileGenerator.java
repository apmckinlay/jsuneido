package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;
import static suneido.language.Token.EQ;
import static suneido.language.Token.INC;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.*;
import suneido.database.query.TreeQueryGenerator.MemDef;
import suneido.language.ParseExpression.Value;

public class CompileGenerator implements Generator<Object> {
    private PrintWriter pw = null;
	private ClassWriter cw;
	private ClassVisitor cv;
	private MethodVisitor mv;
	private Label startLabel;
	private List<String> locals;
	private List<SuValue> constants;
	private final static int SELF = 0;
	private final static int LOCALS = 1;
	private final static int CONSTANTS = 2;
	enum Stack { VALUE, LOCAL, CALLRESULT };

	public CompileGenerator() {
	}

	public CompileGenerator(PrintWriter pw) {
		this.pw = pw;
	}

	public CompileGenerator(CompileGenerator other) {
		cv = other.cv;
		pw = other.pw;
	}

	public Generator<Object> create() {
		return new CompileGenerator(this);
	}

	// constants

	public Object bool(boolean value) {
		return SuBoolean.valueOf(value);
	}

	public Object number(String value) {
		return SuNumber.valueOf(value);
	}

	public Object string(String value) {
		return SuString.valueOf(value);
	}

	public Object symbol(String value) {
		return SuString.valueOf(value); // same as string for now
	}

	public Object date(String value) {
		return SuDate.valueOf(value);
	}

	public Object object(ObjectOrRecord which, Object members) {
		return members;
	}

	public Object memberList(ObjectOrRecord which, Object list, Object member) {
		SuContainer c = (list == null
				? which == OBJECT ? new SuContainer() : new SuRecord()
				: (SuContainer) list);
		MemDef m = (MemDef) member;
		if (m.name == null)
			c.append(m.value);
		else
			c.put(m.name, m.value);
		return c;
	}

	public Object memberDefinition(Object name, Object value) {
		return new MemDef((SuValue) name, (SuValue) value);
	}

	// function

	public void startFunction() {
		cv = cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		if (pw != null)
			cv = new TraceClassVisitor(cw, pw);

		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER,
				"suneido/language/SampleFunction", null,
				"suneido/language/SuFunction", null);

		cv.visitSource("function.suneido", null);

		asm_init();
		asm_toString("SampleFunction");

		mv = cv.visitMethod(ACC_PUBLIC, "invoke",
				"([Lsuneido/SuValue;)Lsuneido/SuValue;", null, null);
		startLabel = new Label();
		mv.visitLabel(startLabel);

		//		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
		//				"Ljava/io/PrintStream;");
		//		mv.visitLdcInsn("hello world");
		//		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
		//				"(Ljava/lang/String;)V");

		locals = new ArrayList<String>();
		constants = new ArrayList<SuValue>();

		// TODO do this lazily in case method doesn't need it
		mv.visitLdcInsn("SampleFunction");
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Constants", "get",
				"(Ljava/lang/String;)[Lsuneido/SuValue;");
		mv.visitVarInsn(ASTORE, CONSTANTS);
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		locals.add(name);
		// TODO handle default value
		return null;
	}

	private void asm_init() {
		mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuFunction",
				"<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		mv = null;
	}

	private void asm_toString(String name) {
		mv = cv.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;",
				null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLdcInsn(name);
		mv.visitInsn(ARETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	public Object returnStatement(Object expression) {
		if (expression == null)
			mv.visitInsn(ACONST_NULL);
		else if (expression == LOCAL)
			addNullCheck(expression);
		else if (expression == VALUE || expression == CALLRESULT)
			; // return it
		else
			dupAndStore(expression);
		mv.visitInsn(ARETURN);
		return "return";
	}

	/**
	 * function is called at the end of the function
	 */
	public Object function(Object params, Object compound) {

		if (compound != "return")
			returnStatement(compound);

		Label endLabel = new Label();
		mv.visitLabel(endLabel);
		mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;",
				null, startLabel, endLabel, 0);
		mv.visitLocalVariable("args", "[Lsuneido/SuValue;",
				null, startLabel, endLabel, 1);
		mv.visitLocalVariable("constants", "[Lsuneido/SuValue;",
				null, startLabel, endLabel, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cv.visitEnd();

		Constants.put("SampleFunction", constants);

		return cw.toByteArray();
	}

	// expressions

	public Object constant(Object value) {
		int i = constants.size();
		constants.add((SuValue) value);
		mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(i);
		mv.visitInsn(AALOAD);
		return VALUE;
	}

	private void iconst(int i) {
		if (i <= 5 || i == -1)
			mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			mv.visitVarInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, i);
		else
			mv.visitLdcInsn(i);
	}

	public Object identifier(String name) {
		if (name == "this")
			mv.visitVarInsn(ALOAD, SELF);
		else if (Character.isLowerCase(name.charAt(0))) {
			localRef(name);
			mv.visitInsn(AALOAD);
			return LOCAL;
		} else {
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals", "get",
					"(Ljava/lang/String;)Lsuneido/SuValue;");
		}
		return VALUE;
	}

	private void localRef(String name) {
		mv.visitVarInsn(ALOAD, LOCALS);
		iconst(addLocal(name));
	}

	private int addLocal(String name) {
		int i = locals.indexOf(name);
		if (i == -1) {
			i = locals.size();
			locals.add(name);
		}
		return i;
	}

	public Object member(Object term, String identifier) {
		mv.visitLdcInsn(identifier);
		getMember();
		return VALUE;
	}

	public Object subscript(Object term, Object expr) {
		assert (expr instanceof Stack);
		getSubscript();
		return VALUE;
	}

	public Object self() {
		mv.visitVarInsn(ALOAD, SELF);
		return VALUE;
	}

	public void lvalue(Value<Object> value) {
		switch (value.type) {
		case IDENTIFIER:
			if (Character.isUpperCase(value.id.charAt(0)))
				throw new SuException("globals are read-only");
			localRef(value.id);
			break;
		case MEMBER:
			mv.visitLdcInsn(value.id);
			break;
		case SUBSCRIPT:
			break;
		default:
			throw new SuException("invalid lvalue type: " + value.type);
		}
	}

	public Object assignment(Object term, Value<Object> value, Token op,
			Object expression) {
		dupAndStore(expression);
		if (op == EQ) {
			if (value.type == IDENTIFIER
					&& (expression == LOCAL || expression == CALLRESULT))
				addNullCheck(expression);
		} else {
			identifier(value.id);
			valueMethod_v_v(assignOp(op));
		}
		return value.type;
	}

	private void addNullCheck(Object expr) {
		Label label = new Label();
		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNONNULL, label);
		mv.visitTypeInsn(NEW, "suneido/SuException");
		mv.visitInsn(DUP);
		mv.visitLdcInsn(expr == LOCAL ? "uninitialized variable"
				: "no return value");
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		mv.visitInsn(ATHROW);
		mv.visitLabel(label);
	}

	private String assignOp(Token op) {
		String s = op.toString();
		return s.substring(0, s.length() - 2).toLowerCase();
	}

	private void valueMethod_v(String method) {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", method,
				"()Lsuneido/SuValue;");
	}
	private void valueMethod_v_v(String method) {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", method,
				"(Lsuneido/SuValue;)Lsuneido/SuValue;");
	}
	private void getMember() {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "get",
				"(Ljava/lang/String;)Lsuneido/SuValue;");
	}

	private void putMember() {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "put",
				"(Ljava/lang/String;Lsuneido/SuValue;)V");
	}

	private void getSubscript() {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "get",
				"(Lsuneido/SuValue;)Lsuneido/SuValue;");
	}

	private void putSubscript() {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "put",
				"(Lsuneido/SuValue;Lsuneido/SuValue;)V");
	}

	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		mv.visitInsn(AALOAD);
		lvalue(value);
		valueMethod_v(incdec == INC ? "add1" : "sub1");
		dupAndStore(value.type);
		return VALUE;
	}

	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		identifier(value.id);
		valueMethod_v(incdec == INC ? "add1" : "sub1");
		return value.type;
	}

	private void dupAndStore(Object expr) {
		if (!(expr instanceof Value.Type))
			return;
		mv.visitInsn(DUP_X2);
		store(expr);
	}

	private void store(Object expression) {
		if (expression == IDENTIFIER)
			mv.visitInsn(AASTORE);
		else if (expression == MEMBER)
			putMember();
		else if (expression == SUBSCRIPT)
			putSubscript();
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		valueMethod_v_v(op.toString().toLowerCase());
		return VALUE;
	}

	public Object unaryExpression(Token op, Object expression) {
		switch (op) {
		case SUB:
			mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "uminus",
					"()Lsuneido/SuValue;");
			break;
		case ADD:
			// TODO should implement this (altho cSuneido doesn't)
			break;
		default:
			throw new SuException("invalid unaryExpression op: " + op);
		}
		return VALUE;
	}

	public Object functionCall(Object function, Value<Object> value,
			Object arguments) {
		int nargs = arguments == null ? 0 : (Integer) arguments;
		if (value.id == null)
			invokeFunction(nargs);
		else {
			mv.visitLdcInsn(value.id);
			invokeMethod(nargs);
		}
		return CALLRESULT;
	}

	private static final String[] args = new String[] {
		"",
		"Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
	};
	private void invokeFunction(int i) {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "invokeN", "("
				+ args[i] + ")Lsuneido/SuValue;");
	}
	private void invokeMethod(int i) {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "invokeN",
				"(Ljava/lang/String;" + args[i] + ")Lsuneido/SuValue;");
	}

	public Object argumentList(Object list, String keyword, Object expression) {
		// TODO handle named args
		int n = (list == null ? 0 : (Integer) list);
		return n + 1;
	}

	public void atArgument(String n) {
		assert "0".equals(n) || "1".equals(n);
		mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass",
				n.charAt(0) == '1' ? "EACH1" : "EACH", "Lsuneido/SuString;");
	}
	public Object atArgument(String n, Object expr) {
		return null;
	}

	// end of expression stuff

	public Object block(Object params, Object statements) {
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

	public Object and(Object expr1, Object expr2) {
		// TODO and
		return null;
	}

	public Object or(Object expr1, Object expr2) {
		// TODO or
		return null;
	}

	public Object conditional(Object primaryExpression, Object first,
			Object second) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object breakStatement() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object continueStatement() {
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
		return expression;
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

	public Object ifStatement(Object expression, Object t, Object f) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object in(Object expression, Object constant) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object newExpression(Object term, Object arguments) {
		// TODO Auto-generated method stub
		return null;
	}

	public void beforeStatement(Object list) {
		if (list instanceof Stack)
			mv.visitInsn(POP);
		else if (list instanceof Value.Type)
			store(list);
	}

	public Object statementList(Object list, Object next) {
		return next;
	}

	public Object switchCases(Object cases, Object values, Object statements) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object switchStatement(Object expression, Object cases) {
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

	public Object whileStatement(Object expression, Object statement) {
		// TODO Auto-generated method stub
		return null;
	}

}
