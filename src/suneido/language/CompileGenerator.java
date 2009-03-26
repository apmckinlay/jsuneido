package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;
import static suneido.language.Token.EQ;
import static suneido.language.Token.INC;

import java.io.PrintWriter;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.*;
import suneido.database.query.TreeQueryGenerator.MemDef;
import suneido.language.ParseExpression.Value;

public class CompileGenerator implements Generator<Object> {
    private PrintWriter pw = null;
	private ClassWriter cw;
	private ClassVisitor cv;
	private final static int SELF = 0;
	private final static int LOCALS = 1;
	private final static int CONSTANTS = 2;
	enum Stack { VALUE, LOCAL, CALLRESULT };
	private List<FunctionSpec> functions = null;
	private static final String[] arrayString = new String[0];
	private static final SuValue[] arraySuValue = new SuValue[0];
	private Function f = null; // the current function
	private Deque<Function> fstack = null; // functions nested around f
	List<SuValue[]> constants = null;

	public CompileGenerator() {
	}

	public CompileGenerator(PrintWriter pw) {
		this.pw = pw;
	}

	private static class Function {
		String name;
		MethodVisitor mv;
		Label startLabel;
		List<String> locals;
		List<SuValue> constants;
		boolean constantsUsed = false;
		int ndefaults = 0;
		int iConstants;
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
		if (cv == null) {
			fstack = new ArrayDeque<Function>();
			functions = new ArrayList<FunctionSpec>();

			cv = cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

			if (pw != null)
				cv = new TraceClassVisitor(cw, pw);

			cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER,
					"suneido/language/SampleFunction", null,
					"suneido/language/SuFunction", null);

			cv.visitSource("function.suneido", null);

			asm_init();
			asm_toString("SampleFunction");

			f = new Function();

			f.name = "invoke";
		} else {
			fstack.push(f);
			f = new Function();
			f.name = "f" + functions.size();
		}
System.out.println("startFunction " + f.name);
		f.mv = cv.visitMethod(ACC_PUBLIC, f.name,
				"([Lsuneido/SuValue;)Lsuneido/SuValue;", null, null);
		f.startLabel = new Label();
		f.mv.visitLabel(f.startLabel);

		f.locals = new ArrayList<String>();
		f.constants = new ArrayList<SuValue>();
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		f.locals.add(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert (i == f.ndefaults);
			++f.ndefaults;
		}
		int n = (list == null ? 0 : (Integer) list);
		return n + 1;
	}

	private void asm_init() {
		MethodVisitor mv =
				cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
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
	}

	private void asm_toString(String name) {
		MethodVisitor mv =
				cv.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;",
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
			f.mv.visitInsn(ACONST_NULL);
		else if (expression == LOCAL)
			addNullCheck(expression);
		else if (expression == VALUE || expression == CALLRESULT)
			; // return it
		else
			dupAndStore(expression);
		f.mv.visitInsn(ARETURN);
		return "return";
	}

	/**
	 * function is called at the end of the function
	 */
	public Object function(Object params, Object compound) {

System.out.println("function end " + f.name);

		if (compound != "return")
			returnStatement(compound);

		Label endLabel = new Label();
		f.mv.visitLabel(endLabel);
		f.mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;",
				null, f.startLabel, endLabel, 0);
		f.mv.visitLocalVariable("args", "[Lsuneido/SuValue;",
				null,
				f.startLabel, endLabel, 1);
		f.mv.visitLocalVariable("constants", "[Lsuneido/SuValue;",
				null,
				f.startLabel, endLabel, 2);
		f.mv.visitMaxs(0, 0);
		f.mv.visitEnd();

		SuValue[] constantsArray = f.constants.toArray(arraySuValue);
		if (f.constantsUsed)
			constants.set(f.iConstants, constantsArray);
		if (fstack.isEmpty()) {

			int nparams = (params == null ? 0 : (Integer) params);
			FunctionSpec fs = new FunctionSpec(f.locals.toArray(arrayString),
					nparams, constantsArray, f.ndefaults);
			functions.add(fs);

			genDispatcher(functions);

			cv.visitEnd();
			return cw.toByteArray();
		} else {
			f = fstack.pop();
			return null;
		}
	}

	private void genDispatcher(List<FunctionSpec> functions) {
		final int THIS = 0;
		final int METHOD = 1;
		final int ARGS = 2;

		MethodVisitor mv =
				cw.visitMethod(
						ACC_PUBLIC + ACC_VARARGS,
						"invoke",
				"(Ljava/lang/String;[Lsuneido/SuValue;)Lsuneido/SuValue;",
				null, null);
		mv.visitCode();
		Label begin = new Label();
		mv.visitLabel(begin);

		// if (method == "call")
		//		invoke(args)
		mv.visitVarInsn(ALOAD, METHOD);
		mv.visitLdcInsn("call");
		Label l1 = new Label();
		mv.visitJumpInsn(IF_ACMPNE, l1);
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue",
				"invoke", "([Lsuneido/SuValue;)Lsuneido/SuValue;");
		mv.visitInsn(ARETURN);
		mv.visitLabel(l1);

		// TODO: gen else-if for each method

		// else
		//		super.invoke(method, args)
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, METHOD);
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuFunction",
				"invoke",
				"(Ljava/lang/String;[Lsuneido/SuValue;)Lsuneido/SuValue;");
		mv.visitInsn(ARETURN);

		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/SampleFunction;",
				null, begin, end, 0);
		mv.visitLocalVariable("method", "Ljava/lang/String;",
				null, begin, end, 1);
		mv.visitLocalVariable("args", "[Lsuneido/SuValue;",
				null, begin, end, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	// expressions

	public Object constant(Object value) {
		int i = constantFor(value);
		if (!f.constantsUsed) {
			f.constantsUsed = true;
			if (constants == null)
				constants = new ArrayList<SuValue[]>();
			f.iConstants = constants.size();
			constants.add(null); // to increase size, set correctly in function
			f.mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass",
					"constants", "[[Lsuneido/SuValue;");
			f.mv.visitIntInsn(BIPUSH, f.iConstants);
			f.mv.visitInsn(AALOAD);
			f.mv.visitInsn(DUP);
			f.mv.visitVarInsn(ASTORE, CONSTANTS);
		} else
			f.mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(i);
		f.mv.visitInsn(AALOAD);
		return VALUE;
	}

	private int constantFor(Object value) {
		int i = f.constants.indexOf(value);
		return i == -1 ? addConstant(value) : i;
	}

	private int addConstant(Object value) {
		f.constants.add((SuValue) value);
		return f.constants.size() - 1;
	}

	private void iconst(int i) {
		if (i <= 5 || i == -1)
			f.mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			f.mv.visitVarInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			f.mv.visitIntInsn(SIPUSH, i);
		else
			f.mv.visitLdcInsn(i);
	}

	public Object identifier(String name) {
		if (name.equals("this"))
			f.mv.visitVarInsn(ALOAD, SELF);
		else if (Character.isLowerCase(name.charAt(0))) {
			localRef(name);
			f.mv.visitInsn(AALOAD);
			return LOCAL;
		} else {
			f.mv.visitLdcInsn(name);
			f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals", "get",
					"(Ljava/lang/String;)Lsuneido/SuValue;");
		}
		return VALUE;
	}

	private void localRef(String name) {
		f.mv.visitVarInsn(ALOAD, LOCALS);
		iconst(addLocal(name));
	}

	private int addLocal(String name) {
		int i = f.locals.indexOf(name);
		if (i == -1) {
			i = f.locals.size();
			f.locals.add(name);
		}
		return i;
	}

	public Object member(Object term, String identifier) {
		f.mv.visitLdcInsn(identifier);
		getMember();
		return VALUE;
	}

	public Object subscript(Object term, Object expr) {
		assert (expr instanceof Stack);
		getSubscript();
		return VALUE;
	}

	public Object self() {
		f.mv.visitVarInsn(ALOAD, SELF);
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
			f.mv.visitLdcInsn(value.id);
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
			binaryMethod(assignOp(op));
		}
		return value.type;
	}

	private void addNullCheck(Object expr) {
		Label label = new Label();
		f.mv.visitInsn(DUP);
		f.mv.visitJumpInsn(IFNONNULL, label);
		f.mv.visitTypeInsn(NEW, "suneido/SuException");
		f.mv.visitInsn(DUP);
		f.mv.visitLdcInsn(expr == LOCAL ? "uninitialized variable"
				: "no return value");
		f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		f.mv.visitInsn(ATHROW);
		f.mv.visitLabel(label);
	}

	private String assignOp(Token op) {
		String s = op.toString();
		return s.substring(0, s.length() - 2).toLowerCase();
	}

	private void valueMethod_v(String method) {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", method,
				"()Lsuneido/SuValue;");
	}
	private void binaryMethod(String method) {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", method,
				method.equals("cat") ? "(Lsuneido/SuValue;)Lsuneido/SuString;"
						: "(Lsuneido/SuValue;)Lsuneido/SuNumber;");
	}
	private void getMember() {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "get",
				"(Ljava/lang/String;)Lsuneido/SuValue;");
	}

	private void putMember() {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "put",
				"(Ljava/lang/String;Lsuneido/SuValue;)V");
	}

	private void getSubscript() {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "get",
				"(Lsuneido/SuValue;)Lsuneido/SuValue;");
	}

	private void putSubscript() {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "put",
				"(Lsuneido/SuValue;Lsuneido/SuValue;)V");
	}

	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		f.mv.visitInsn(AALOAD);
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
		f.mv.visitInsn(DUP_X2);
		store(expr);
	}

	private void store(Object expression) {
		if (expression == IDENTIFIER)
			f.mv.visitInsn(AASTORE);
		else if (expression == MEMBER)
			putMember();
		else if (expression == SUBSCRIPT)
			putSubscript();
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		binaryMethod(op.toString().toLowerCase());
		return VALUE;
	}

	public Object unaryExpression(Token op, Object expression) {
		switch (op) {
		case SUB:
			f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "uminus",
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
			f.mv.visitLdcInsn(value.id);
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
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "invokeN", "("
				+ args[i] + ")Lsuneido/SuValue;");
	}
	private void invokeMethod(int i) {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "invokeN",
				"(Ljava/lang/String;" + args[i] + ")Lsuneido/SuValue;");
	}

	public Object argumentList(Object list, String keyword, Object expression) {
		int n = (list == null ? 0 : (Integer) list);
		return n + 1;
	}

	public void argumentName(String name) {
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass", "NAMED",
				"Lsuneido/SuString;");
	}

	public void atArgument(String n) {
		assert "0".equals(n) || "1".equals(n);
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass",
				n.charAt(0) == '1' ? "EACH1" : "EACH", "Lsuneido/SuString;");
	}
	public Object atArgument(String n, Object expr) {
		return null;
	}

	// complex constants

	public Object classConstant(String base, Object members) {
		// TODO Auto-generated method stub
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
			f.mv.visitInsn(POP);
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
