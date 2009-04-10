package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.ObjectOrRecord.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;

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
	private List<FunctionSpec> fspecs = null;
	private static final String[] arrayString = new String[0];
	private static final Object[] arrayObject = new Object[0];
	private static final Object[][] arrayConstants = new Object[0][0];
	private static final FunctionSpec[] arrayFunctionSpec = new FunctionSpec[0];
	private Function f = null; // the current function
	private Deque<Function> fstack = null; // functions nested around f
	List<Object[]> constants = null;
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
		List<Object> constants;
		int ndefaults = 0;
		int iConstants;
		int nparams = 0;
		boolean atParam;
		int iFspecs;
	}

	// constants

	public Object bool(boolean value) {
		return value ? Boolean.TRUE : Boolean.FALSE;
	}

	public Object number(String value) {
		return Ops.stringToNumber(value);
	}

	public Object string(String value) {
		return value;
	}

	public Object symbol(String value) {
		return value; // same as string for now
	}

	public Object date(String value) {
		return Ops.stringToDate(value);
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
		return new MemDef(name, value);
	}

	// function

	public void startFunction(FuncOrBlock which) {
		if (cv == null) {
			startTopFunction();
		} else {
			fstack.push(f);
			f = new Function();
			f.name = ("f" + fspecs.size()).intern();
		}
		f.mv = cv.visitMethod(ACC_PRIVATE, f.name,
				"([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
		f.mv = new OptimizeToBool(f.mv);
		f.mv.visitCode();
		f.startLabel = new Label();
		f.mv.visitLabel(f.startLabel);

		massage();
		loadConstants();

		if (which == FuncOrBlock.FUNC) {
			f.locals = new ArrayList<String>();
			f.constants = new ArrayList<Object>();
		} else if (which == FuncOrBlock.BLOCK)
			f.locals = fstack.peek().locals; // use outer locals
	}

	private void startTopFunction() {
		fstack = new ArrayDeque<Function>();
		fspecs = new ArrayList<FunctionSpec>();

		cv = cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		if (pw != null)
			cv = new TraceClassVisitor(cw, pw);

		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "suneido/language/MyFunc", null,
				"suneido/language/SuFunction", null);

		cv.visitSource("function.suneido", null);

		gen_params();
		gen_constants();
		gen_init();
		gen_setup();
		gen_toString("MyFunc"); // TODO pass in name

		f = new Function();
		f.name = "invoke";
	}

	private void gen_constants() {
		FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, "constants",
				"[[Ljava/lang/Object;", null, null);
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
				"([Lsuneido/language/FunctionSpec;[[Ljava/lang/Object;)V",
				null, null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLabel(start);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(PUTSTATIC, "suneido/language/MyFunc",
				"params", "[Lsuneido/language/FunctionSpec;");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitFieldInsn(PUTSTATIC, "suneido/language/MyFunc",
				"constants",
				"[[Ljava/lang/Object;");
		mv.visitInsn(RETURN);
		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, start, end, 0);
		mv.visitLocalVariable("p", "[Lsuneido/language/FunctionSpec;", null,
				start, end, 1);
		mv.visitLocalVariable("c", "[[Ljava/lang/Object;", null, start, end, 2);
		mv.visitMaxs(1, 3);
		mv.visitEnd();
	}

	// massage at start of individual methods rather than in dispatcher
	// so "call" and private methods can be called directly
	// and blocks can do their own handling
	private void massage() {
		final int THIS = 0;
		final int ARGS = 1;

		f.iFspecs = fspecs.size();
		fspecs.add(null); // to reserve a slot, set correctly later

		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/MyFunc", "params",
				"[Lsuneido/language/FunctionSpec;");
		iconst(f.mv, f.iFspecs);
		f.mv.visitInsn(AALOAD);
		f.mv.visitVarInsn(ALOAD, ARGS);
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/MyFunc",
				"massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		f.mv.visitVarInsn(ASTORE, ARGS);
	}

	private void loadConstants() {
		if (constants == null)
			constants = new ArrayList<Object[]>();
		f.iConstants = constants.size();
		constants.add(null); // to increase size, set correctly in function
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/MyFunc", "constants",
				"[[Ljava/lang/Object;");
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

		finishMethod(compound);

		Object[] constantsArray = f.constants.toArray(arrayObject);
		constants.set(f.iConstants, constantsArray);

		FunctionSpec fs = new FunctionSpec(f.name, f.locals.toArray(arrayString),
				f.nparams, constantsArray, f.ndefaults, f.atParam);
		fspecs.set(f.iFspecs, fs);

		if (fstack.isEmpty()) {
			genDispatcher(fspecs);

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
			sc.setup(fspecs.toArray(arrayFunctionSpec), linkConstants(sc));
			return sc;
		} else {
			String method = f.name;
			f = fstack.pop();
			return new SuMethod(null, method);
		}
	}

	private void finishMethod(Object compound) {
		if (compound != "return")
			returnStatement(compound);

		Label endLabel = new Label();
		f.mv.visitLabel(endLabel);
		f.mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, f.startLabel, endLabel, 0);
		f.mv.visitLocalVariable("args", "[Ljava/lang/Object;",
				null, f.startLabel, endLabel, 1);
		f.mv.visitLocalVariable("constants", "[Ljava/lang/Object;",
				null, f.startLabel, endLabel, 2);
		f.mv.visitMaxs(0, 0);
		f.mv.visitEnd();
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

	private Object[][] linkConstants(SuClass sc) {
		if (constants == null)
			return null;
		for (Object[] cs : constants)
			for (Object x : cs)
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
				"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
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
			mv.visitVarInsn(ALOAD, ARGS);
			mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/MyFunc",
					f.name, "([Ljava/lang/Object;)Ljava/lang/Object;");
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
				"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitInsn(ARETURN);

		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/MyFunc;",
				null, begin, end, 0);
		mv.visitLocalVariable("method", "Ljava/lang/String;",
				null, begin, end, 1);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;",
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
		if (value == Boolean.TRUE || value == Boolean.FALSE) {
			getBoolean(value == Boolean.TRUE ? "TRUE" : "FALSE");
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
		f.constants.add(value);
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
					"(Ljava/lang/String;)Ljava/lang/Object;");
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
		getMember();
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
		if (op == Token.EQ) {
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

	private void unaryMethod(String method) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", method,
				"(Ljava/lang/Object;)Ljava/lang/Number;");
	}
	private void binaryMethod(Token op) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type);
	}
	private void getMember() {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "get",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	}

	private void putMember() {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "put",
				"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
	}

	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		// stack: i args (or: member object)
		f.mv.visitInsn(DUP2);
		// stack: i args i args
		load(value.type);
		// stack: v i args
		unaryMethod(incdec == Token.INC ? "add1" : "sub1");
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
		unaryMethod(incdec == Token.INC ? "add1" : "sub1");
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
		else if (type == MEMBER || type == SUBSCRIPT)
			getMember();
		else
			throw new SuException("unknown load type: " + type);
	}

	private void store(Object type) {
		if (type == IDENTIFIER)
			f.mv.visitInsn(AASTORE);
		else if (type == MEMBER || type == SUBSCRIPT)
			putMember();
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
			unaryMethod("uminus");
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
		"Ljava/lang/Object;",
		"Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
					"Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;",
	};
	private void invokeFunction(int nargs) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invokeN",
				"(Ljava/lang/Object;" + args[nargs] + ")Ljava/lang/Object;");
	}
	private void invokeMethod(int nargs) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invokeN",
				"(Ljava/lang/Object;Ljava/lang/String;" + args[nargs]
						+ ")Ljava/lang/Object;");
	}

	public Object argumentList(Object list, String keyword, Object expression) {
		int n = (list == null ? 0 : (Integer) list);
		return n + (keyword == null ? 1 : 3);
	}

	public void argumentName(String name) {
		specialArg("NAMED");
		constant(string(name));
	}

	public void atArgument(String n) {
		assert "0".equals(n) || "1".equals(n);
		specialArg(n.charAt(0) == '1' ? "EACH1" : "EACH");
	}
	public Object atArgument(String n, Object expr) {
		return null;
	}
	private void specialArg(String which) {
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/SuClass$SpecialArg",
				which, "Lsuneido/language/SuClass$SpecialArg;");
	}

	// complex constants

	public Object classConstant(String base, Object members) {
		// TODO class constant
		return null;
	}

	public Object and(Object prev) {
		getBoolean("FALSE");
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
		getBoolean("FALSE");
		f.mv.visitLabel(l0);
	}

	private void getBoolean(String which) {
		f.mv.visitFieldInsn(GETSTATIC,
				"java/lang/Boolean", which,
				"Ljava/lang/Boolean;");
	}
	public Object and(Object expr1, Object expr2) {
		return VALUE;
	}

	public Object or(Object prev) {
		getBoolean("TRUE");
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
		getBoolean("TRUE");
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
		throw new SuException("'in' is only implemented for queries");
	}

	public void newCall() {
		f.mv.visitLdcInsn("<new>");
	}
	public Object newExpression(Object term, Object arguments) {
		int nargs = arguments == null ? 0 : (Integer) arguments;
		invokeMethod(nargs);
		return VALUE;
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
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "toBool",
				"(Ljava/lang/Object;)I");
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
		endLoop(statement, loop);
		return null;
	}

	public Object block(Object params, Object statements) {
		finishMethod(statements);



		return null;
	}

	// TODO break & continue in blocks

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

	public Object forStart() {
		Label label = new Label();
		f.mv.visitJumpInsn(GOTO, label);
		return label;
	}
	public void forIncrement(Object label) {
		if (label != null)
			f.mv.visitLabel((Label) label);
	}
	public void forCondition(Object cond, Object loop) {
		toBool(cond);
		gotoBreak(IFFALSE, loop);
	}
	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement, Object loop) {
		endLoop(statement, loop);
		return null;
	}
	public Object expressionList(Object list, Object expression) {
		afterStatement(expression);
		return null;
	}

	public Object forInExpression(String var, Object expr) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "iterator",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		Object loop = loop();
		f.mv.visitInsn(DUP);
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "hasNext",
				"(Ljava/lang/Object;)Z");
		gotoBreak(IFFALSE, loop);
		f.mv.visitInsn(DUP);
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "next",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		f.mv.visitVarInsn(ALOAD, LOCALS);
		f.mv.visitInsn(SWAP);
		iconst(f.mv, addLocal(var));
		f.mv.visitInsn(SWAP);
		f.mv.visitInsn(AASTORE);
		return loop;
	}

	public Object forInStatement(String var, Object expr, Object statement,
			Object loop) {
		endLoop(statement, loop);
		f.mv.visitInsn(POP); // pop iterator
		return null;
	}

	public Object foreverStatement(Object statement, Object loop) {
		endLoop(statement, loop);
		return null;
	}

	private void endLoop(Object statement, Object loop) {
		afterStatement(statement);
		gotoContinue(GOTO, loop);
		setBreak(loop);
	}

	static class SwitchLabels {
		Label end = new Label();
		Label body;
		Label next;
	}
	public Object startSwitch() {
		return new SwitchLabels();
	}
	public void startCase(Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.next != null)
			f.mv.visitLabel(slabels.next);
		slabels.next = null;
		slabels.body = null;
	}
	public void startCaseValue() {
		f.mv.visitInsn(DUP);
	}
	public Object caseValues(Object values, Object expr, Object labels,
			boolean more) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "is_",
				"(Ljava/lang/Object;Ljava/lang/Object;)Z");
		SwitchLabels slabels = (SwitchLabels) labels;
		if (more) {
			if (slabels.body == null)
				slabels.body = new Label();
			f.mv.visitJumpInsn(IFTRUE, slabels.body);
		} else {
			if (slabels.next == null)
				slabels.next = new Label();
			f.mv.visitJumpInsn(IFFALSE, slabels.next);
		}
		return null;
	}
	public void startCaseBody(Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.body != null)
			f.mv.visitLabel(slabels.body);
		f.mv.visitInsn(POP);
	}
	public Object switchCases(Object cases, Object values, Object statements,
			Object labels) {
		afterStatement(statements);
		f.mv.visitJumpInsn(GOTO, ((SwitchLabels) labels).end);
		return null;
	}
	public Object switchStatement(Object expr, Object cases, Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.next != null)
			f.mv.visitLabel(((SwitchLabels) labels).next);
		f.mv.visitInsn(POP);
		f.mv.visitLabel(((SwitchLabels) labels).end);
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
