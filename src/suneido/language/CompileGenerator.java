package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;
import static suneido.language.Token.EQ;
import static suneido.language.Token.INC;

import java.io.*;
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
	enum Stack { VALUE, LOCAL, PARAMETER, CALLRESULT };
	private List<FunctionSpec> functions = null;
	private static final String[] arrayString = new String[0];
	private static final SuValue[] arraySuValue = new SuValue[0];
	private static final SuValue[][] arrayConstants = new SuValue[0][0];
	private static final FunctionSpec[] arrayFunctionSpec = new FunctionSpec[0];
	private Function f = null; // the current function
	private Deque<Function> fstack = null; // functions nested around f
	List<SuValue[]> constants = null;
	private static final int IFTRUE = IFNE;
	private static final int IFFALSE = IFEQ;
	static class Loop {
		public Label continueLabel = new Label();
		public Label breakLabel = new Label();
	};

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
		int ndefaults = 0;
		int iConstants;
		int nparams = 0;
		boolean atParam;
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

			cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "suneido/language/MyFunc",
					null, "suneido/language/SuFunction", null);

			cv.visitSource("function.suneido", null);

			gen_params();
			gen_constants();
			gen_init();
			gen_setup();
			gen_toString("MyFunc");

			f = new Function();

			f.name = "invoke";
		} else {
			fstack.push(f);
			f = new Function();
			f.name = ("f" + functions.size()).intern();
		}
		f.mv = cv.visitMethod(ACC_PRIVATE, f.name,
				"([Lsuneido/SuValue;)Lsuneido/SuValue;", null, null);
		f.mv.visitCode();
		f.startLabel = new Label();
		f.mv.visitLabel(f.startLabel);

		loadConstants();

		f.locals = new ArrayList<String>();
		f.constants = new ArrayList<SuValue>();
	}

	private void gen_constants() {
		FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "constants",
				"[[Lsuneido/SuValue;", null, null);
		fv.visitEnd();
	}

	private void gen_params() {
		FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "params",
				"[Lsuneido/language/FunctionSpec;", null, null);
		fv.visitEnd();
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		if (f.atParam = name.startsWith("@"))
			name = name.substring(1, name.length());
		f.locals.add(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert (i == f.ndefaults);
			++f.ndefaults;
		}
		++f.nparams;
		return null;
	}

	private void gen_init() {
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
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void gen_toString(String name) {
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
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void gen_setup() {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setup",
				"([Lsuneido/language/FunctionSpec;[[Lsuneido/SuValue;)V",
				null, null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLabel(start);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(PUTSTATIC, "suneido/language/MyFunc",
				"params", "[Lsuneido/language/FunctionSpec;");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitFieldInsn(PUTSTATIC, "suneido/language/MyFunc",
				"constants", "[[Lsuneido/SuValue;");
		mv.visitInsn(RETURN);
		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, start, end, 0);
		mv.visitLocalVariable("p", "[Lsuneido/language/FunctionSpec;", null,
				start, end, 1);
		mv.visitLocalVariable("c", "[[Lsuneido/SuValue;", null, start, end, 2);
		mv.visitMaxs(1, 3);
		mv.visitEnd();
	}

	private void loadConstants() {
		if (constants == null)
			constants = new ArrayList<SuValue[]>();
		f.iConstants = constants.size();
		constants.add(null); // to increase size, set correctly in function
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/MyFunc", "constants",
				"[[Lsuneido/SuValue;");
		iconst(f.mv, f.iConstants);
		f.mv.visitInsn(AALOAD);
		f.mv.visitVarInsn(ASTORE, CONSTANTS);
	}

	public Object returnStatement(Object expr) {
		if (expr == null)
			f.mv.visitInsn(ACONST_NULL);
		else if (expr == LOCAL)
			addNullCheck(expr);
		else if (expr == PARAMETER || expr == VALUE || expr == CALLRESULT)
			; // return it
		else
			dupAndStore(expr);
		f.mv.visitInsn(ARETURN);
		return "return";
	}

	/** function is called at the end of the function */
	public Object function(Object params, Object compound) {

		if (compound != "return")
			returnStatement(compound);

		Label endLabel = new Label();
		f.mv.visitLabel(endLabel);
		f.mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, f.startLabel, endLabel, 0);
		f.mv.visitLocalVariable("args", "[Lsuneido/SuValue;",
				null, f.startLabel, endLabel, 1);
		f.mv.visitLocalVariable("constants", "[Lsuneido/SuValue;",
				null, f.startLabel, endLabel, 2);
		f.mv.visitMaxs(0, 0);
		f.mv.visitEnd();

		SuValue[] constantsArray = f.constants.toArray(arraySuValue);
		constants.set(f.iConstants, constantsArray);

		FunctionSpec fs = new FunctionSpec(f.name, f.locals.toArray(arrayString),
				f.nparams, constantsArray, f.ndefaults, f.atParam);
		functions.add(fs);

		if (fstack.isEmpty()) {
			genDispatcher(functions);

			cv.visitEnd();

			dump(cw.toByteArray());

			Loader loader = new Loader();
			Class<?> c = loader.defineClass("suneido.language.MyFunc",
					cw.toByteArray());
			SuClass sc;
			try {
				sc = (SuClass) c.newInstance();
			} catch (InstantiationException e) {
				throw new SuException("newInstance error: " + e);
			} catch (IllegalAccessException e) {
				throw new SuException("newInstance error: " + e);
			}
			sc.setup(functions.toArray(arrayFunctionSpec), linkConstants(sc));
			return sc;
		} else {
			String method = f.name;
			f = fstack.pop();
			return new SuMethod(null, method);
		}
	}

	private static void dump(byte[] buf) {
		try {
			FileOutputStream f = new FileOutputStream("MyFunc.class");
			f.write(buf);
			f.close();
		} catch (IOException e) {
			throw new SuException("dump error");
		}
	}

	private SuValue[][] linkConstants(SuClass sc) {
		if (constants == null)
			return null;
		for (SuValue[] cs : constants)
			for (SuValue x : cs)
				if (x instanceof SuMethod) {
					SuMethod m = (SuMethod) x;
					if (m.instance == null)
						m.instance = sc;
				}
		return constants.toArray(arrayConstants);
	}

	private void genDispatcher(List<FunctionSpec> functions) {
		final int THIS = 0;
		final int METHOD = 1;
		final int ARGS = 2;

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "invoke",
				"(Ljava/lang/String;[Lsuneido/SuValue;)Lsuneido/SuValue;",
				null, null);
		mv.visitCode();
		Label begin = new Label();
		mv.visitLabel(begin);

		for (int i = 0; i < functions.size(); ++i) {
			FunctionSpec f = functions.get(i);
			mv.visitVarInsn(ALOAD, METHOD);
			mv.visitLdcInsn(f.name.equals("invoke") ? "call" : f.name);
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ACMPNE, l1);
			mv.visitVarInsn(ALOAD, THIS);
			mv.visitFieldInsn(GETSTATIC, "suneido/language/MyFunc",
					"params", "[Lsuneido/language/FunctionSpec;");
			iconst(mv, i);
			mv.visitInsn(AALOAD);
			mv.visitVarInsn(ALOAD, ARGS);
			mv.visitMethodInsn(INVOKESTATIC, "suneido/language/MyFunc",
					"massage",
					"(Lsuneido/language/FunctionSpec;[Lsuneido/SuValue;)[Lsuneido/SuValue;");
			mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/MyFunc",
					f.name, "([Lsuneido/SuValue;)Lsuneido/SuValue;");
			mv.visitInsn(ARETURN);
			mv.visitLabel(l1);
		}

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
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, begin, end, 0);
		mv.visitLocalVariable("method", "Ljava/lang/String;",
				null, begin, end, 1);
		mv.visitLocalVariable("args", "[Lsuneido/SuValue;",
				null, begin, end, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	static class Loader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	// expressions

	public Object constant(Object value) {
		if (value == SuBoolean.TRUE || value == SuBoolean.FALSE) {
			f.mv.visitFieldInsn(GETSTATIC, "suneido/SuBoolean",
					value == SuBoolean.TRUE ? "TRUE" : "FALSE",
					"Lsuneido/SuBoolean;");
			return VALUE;
		}
		int i = constantFor(value);
		f.mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(f.mv, i);
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

	private void iconst(MethodVisitor mv, int i) {
		if (-1 <= i && i <= 5)
			mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			mv.visitVarInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, i);
		else
			mv.visitLdcInsn(i);
	}

	public Object identifier(String name) {
		if (name.equals("this"))
			f.mv.visitVarInsn(ALOAD, SELF);
		else if (Character.isLowerCase(name.charAt(0))) {
			int i = localRef(name);
			f.mv.visitInsn(AALOAD);
			return i < f.nparams ? PARAMETER : LOCAL;
		} else {
			f.mv.visitLdcInsn(name);
			f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals", "get",
					"(Ljava/lang/String;)Lsuneido/SuValue;");
		}
		return VALUE;
	}

	private int localRef(String name) {
		f.mv.visitVarInsn(ALOAD, LOCALS);
		int i = addLocal(name);
		iconst(f.mv, i);
		return i;
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
		member(identifier);
		return VALUE;
	}

	private void member(String identifier) {
		f.mv.visitLdcInsn(identifier);
		getMember();
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
			if (!op.commutative())
				f.mv.visitInsn(SWAP);
			binaryMethod(op);
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

	private void valueMethod_v(String method) {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", method,
				"()Lsuneido/SuValue;");
	}
	private void binaryMethod(Token op) {
		f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue",
				op.method, op.resultType.type);
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

	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		// stack: i args (or: member object)
		f.mv.visitInsn(DUP2);
		// stack: i args i args
		load(value.type);
		// stack: v i args
		valueMethod_v(incdec == INC ? "add1" : "sub1");
		// stack: v+1 i args
		f.mv.visitInsn(DUP_X2);
		// stack: v+1 i args v+1
		store(value.type);
		// stack: v+1
		return VALUE;
	}

	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		// stack: i args
		f.mv.visitInsn(DUP2);
		// stack: i args i args
		load(value.type);
		// stack: v i args
		f.mv.visitInsn(DUP_X2);
		// stack: v i args v
		valueMethod_v(incdec == INC ? "add1" : "sub1");
		// stack: v+1 i args v
		store(value.type);
		// stack: v
		return VALUE;
	}

	/**
	 * assignments are delayed because store's lose the expression value so if
	 * we need the value we have to dup before storing but we don't know if we
	 * need the value until later, thus the delay
	 */
	private void dupAndStore(Object expr) {
		if (!(expr instanceof Value.Type))
			return;
		f.mv.visitInsn(DUP_X2);
		store(expr);
	}

	private void load(Object type) {
		if (type == IDENTIFIER)
			f.mv.visitInsn(AALOAD);
		else if (type == MEMBER)
			getMember();
		else if (type == SUBSCRIPT)
			getSubscript();
		else
			throw new SuException("unknown load type: " + type);
	}

	private void store(Object type) {
		if (type == IDENTIFIER)
			f.mv.visitInsn(AASTORE);
		else if (type == MEMBER)
			putMember();
		else if (type == SUBSCRIPT)
			putSubscript();
		else
			throw new SuException("unknown store type: " + type);
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		binaryMethod(op);
		return VALUE;
	}

	public Object unaryExpression(Token op, Object expression) {
		switch (op) {
		case SUB:
			f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuValue", "uminus",
					"()Lsuneido/SuValue;");
			break;
		case ADD:
			// should have a uplus, but cSuneido doesn't
			break;
		default:
			throw new SuException("invalid unaryExpression op: " + op);
		}
		return VALUE;
	}

	public void preFunctionCall(Value<Object> value) {
		if (value.member()) {
			f.mv.visitLdcInsn(value.id);
		}
	}
	public Object functionCall(Object function, Value<Object> value,
			Object arguments) {
		int nargs = arguments == null ? 0 : (Integer) arguments;
		if (value.id == null)
			invokeFunction(nargs);
		else
			invokeMethod(nargs);
		return CALLRESULT;
	}

	private static final String[] args = new String[] {
		"",
		"Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
		"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
					"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
					"Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;Lsuneido/SuValue;",
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
		return n + (keyword == null ? 1 : 3);
	}

	public void argumentName(String name) {
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass", "NAMED",
				"Lsuneido/SuString;");
		constant(string(name));
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
		// TODO class constant
		return null;
	}

	public Object and(Object prev) {
		f.mv.visitFieldInsn(GETSTATIC, "suneido/SuBoolean", "FALSE",
				"Lsuneido/SuBoolean;");
		Label label = (prev == null ? new Label() : (Label) prev);
		f.mv.visitJumpInsn(IF_ACMPEQ, label);
		return label;
	}
	public void andEnd(Object label) {
		if (label == null)
			return;
		Label l0 = new Label();
		f.mv.visitJumpInsn(GOTO, l0);
		f.mv.visitLabel((Label) label);
		f.mv.visitFieldInsn(GETSTATIC, "suneido/SuBoolean", "FALSE",
				"Lsuneido/SuBoolean;");
		f.mv.visitLabel(l0);
	}
	public Object and(Object expr1, Object expr2) {
		return VALUE;
	}

	public Object or(Object prev) {
		f.mv.visitFieldInsn(GETSTATIC, "suneido/SuBoolean", "TRUE",
				"Lsuneido/SuBoolean;");
		Label label = (prev == null ? new Label() : (Label) prev);
		f.mv.visitJumpInsn(IF_ACMPEQ, label);
		return label;
	}
	public void orEnd(Object label) {
		if (label == null)
			return;
		Label l0 = new Label();
		f.mv.visitJumpInsn(GOTO, l0);
		f.mv.visitLabel((Label) label);
		f.mv.visitFieldInsn(GETSTATIC, "suneido/SuBoolean", "TRUE",
				"Lsuneido/SuBoolean;");
		f.mv.visitLabel(l0);
	}
	public Object or(Object expr1, Object expr2) {
		return VALUE;
	}

	public Object conditionalTrue(Object label, Object first) {
		dupAndStore(first);
		return ifElse(label);
	}
	public Object conditional(Object primaryExpression, Object first,
			Object second, Object label) {
		dupAndStore(second);
		f.mv.visitLabel((Label) label);
		return VALUE;
	}

	public Object in(Object expression, Object constant) {
		// TODO in
		return null;
	}

	public Object newExpression(Object term, Object arguments) {
		// TODO new
		return null;
	}

	/**
	 * pop any value left on the stack complete delayed assignment
	 */
	public void afterStatement(Object list) {
		if (list instanceof Stack)
			f.mv.visitInsn(POP);
		else if (list instanceof Value.Type)
			store(list);
	}

	// statements

	public Object statementList(Object list, Object next) {
		return next;
	}

	public Object expressionStatement(Object expression) {
		return expression;
	}

	public Object ifExpr(Object expr) {
		toBool(expr);
		Label label = new Label();
		f.mv.visitJumpInsn(IFEQ, label);
		return label;
	}

	private void toBool(Object expr) {
		dupAndStore(expr);
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/MyFunc", "bool",
				"(Lsuneido/SuValue;)Z");
	}
	public void ifThen(Object label, Object t) {
		afterStatement(t);
	}
	public Object ifElse(Object pastThen) {
		Label pastElse = new Label();
		f.mv.visitJumpInsn(GOTO, pastElse);
		f.mv.visitLabel((Label) pastThen);
		return pastElse;
	}
	public Object ifStatement(Object expr, Object t, Object e, Object afterIf) {
		afterStatement(e);
		f.mv.visitLabel((Label) afterIf);
		return null;
	}

	public Object loop() {
		Loop loop = new Loop();
		f.mv.visitLabel(loop.continueLabel);
		return loop;
	}

	public void whileExpr(Object expr, Object loop) {
		toBool(expr);
		gotoBreak(IFFALSE, loop);
	}

	public Object whileStatement(Object expr, Object statement, Object loop) {
		afterStatement(statement);
		gotoContinue(GOTO, loop);
		setBreak(loop);
		return null;
	}

	public Object block(Object params, Object statements) {
		// TODO blocks
		return null;
	}

	public Object breakStatement(Object loop) {
		gotoBreak(GOTO, loop);
		return null;
	}

	public Object continueStatement(Object loop) {
		gotoContinue(GOTO, loop);
		return null;
	}

	public Object dowhileStatement(Object body, Object expr, Object loop) {
		toBool(expr);
		gotoContinue(IFTRUE, loop);
		setBreak(loop);
		return null;
	}

	private void gotoContinue(int op, Object loop) {
		f.mv.visitJumpInsn(op, ((Loop) loop).continueLabel);
	}
	private void gotoBreak(int op, Object loop) {
		f.mv.visitJumpInsn(op, ((Loop) loop).breakLabel);
	}
	private void setBreak(Object loop) {
		f.mv.visitLabel(((Loop) loop).breakLabel);
	}

	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement) {
		// TODO for classic
		return null;
	}
	public Object expressionList(Object list, Object expression) {
		return null;
	}

	public Object forInStatement(String var, Object expr, Object statement) {
		// TODO for in
		return null;
	}

	public Object foreverStatement(Object statement, Object loop) {
		afterStatement(statement);
		gotoContinue(GOTO, loop);
		setBreak(loop);
		return null;
	}

	public Object switchStatement(Object expression, Object cases) {
		// TODO switch
		return null;
	}
	public Object switchCases(Object cases, Object values, Object statements) {
		return null;
	}
	public Object caseValues(Object values, Object expression) {
		return null;
	}

	public Object throwStatement(Object expression) {
		// TODO throw
		return null;
	}

	public Object tryStatement(Object tryStatement, Object catcher) {
		// TODO try-catch
		return null;
	}
	public Object catcher(String variable, String pattern, Object statement) {
		return null;
	}

}
