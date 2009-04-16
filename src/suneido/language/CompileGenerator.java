package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.MType.CLASS;
import static suneido.language.Generator.MType.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;

import java.io.*;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.*;
import suneido.database.query.TreeQueryGenerator.MemDef;
import suneido.language.ParseExpression.Value;

public class CompileGenerator implements Generator<Object> {
	private final String globalName;
    private PrintWriter pw = null;
	private ClassWriter cw;
	private ClassVisitor cv;
	private boolean isFunction = false;
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
	final int THIS = 0;
	private static class Function {
		Function(String name) {
			this.name = name;
			if (name.equals("call")) {
				SELF = -1; // not used
				ARGS = 1;
				CONSTANTS = 2;
			} else {
				SELF = 1;
				ARGS = 2;
				CONSTANTS = 3;
			}
		}

		final String name;
		MethodVisitor mv;
		Label startLabel;
		List<String> locals;
		List<Object> constants;
		int ndefaults = 0;
		int iConstants;
		int nparams = 0;
		boolean atParam;
		int iparams; // where block params start in locals
		int iFspecs;
		boolean auto_it_param = false;
		boolean it_param_used = false;
		TryCatch blockReturnCatcher = null;
		boolean isBlock;
		final int ARGS;
		final int SELF;
		final int CONSTANTS;
	}
	static class Loop {
		public Label continueLabel = new Label();
		public Label breakLabel = new Label();
	}
	static final Object Block = new Object();

	public CompileGenerator(String name) {
		this.globalName = name;
	}

	public CompileGenerator(String name, PrintWriter pw) {
		this(name);
		this.pw = pw;
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

	public Object object(MType which, Object members) {
		return members;
	}

	@SuppressWarnings("unchecked")
	public Object memberList(MType which, Object list, Object member) {
		MemDef m = (MemDef) member;
		if (which == CLASS) {
			Map<String, Object> vars = (list == null
					? new HashMap<String, Object>()
					: (Map<String,Object>) list);
			vars.put((String) m.name, m.value);
			return vars;
		} else {
			SuContainer c = (list == null
					? which == OBJECT ? new SuContainer() : new SuRecord()
					: (SuContainer) list);
			if (m.name == null)
				c.append(m.value);
			else
				c.put(m.name, m.value);
			return c;
		}
	}

	public Object memberDefinition(Object name, Object value) {
		return new MemDef(name, value);
	}

	// TODO super.meth()
	// TODO new must call super.new, implicitly or explicitly
	// TODO nested classes
	// TODO getters & setters

	public void startClass() {
		startClass(null);
	}

	private void startClass(String base) {
		fstack = new ArrayDeque<Function>();
		fspecs = new ArrayList<FunctionSpec>();

		cv = cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		if (pw != null)
			cv = new TraceClassVisitor(cw, pw);

		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER,
				"suneido/language/" + globalName, null,
				"suneido/language/SuClass", null);

		cv.visitSource("", null);

		gen_init();
	}

	public Object classConstant(String base, Object members) {
		return finishClass(base, members);
	}

	private Object finishClass(String base, Object members) {
		genInvoke(fspecs);

		cv.visitEnd();

		dump(cw.toByteArray());

		Loader loader = new Loader();
		Class<?> c =
				loader.defineClass("suneido.language." + globalName,
						cw.toByteArray());
		SuCallable callable;
		try {
			callable = (SuCallable) c.newInstance();
		} catch (InstantiationException e) {
			throw new SuException("newInstance error: " + e);
		} catch (IllegalAccessException e) {
			throw new SuException("newInstance error: " + e);
		}
		callable.params = fspecs.toArray(arrayFunctionSpec);
		callable.constants = linkConstants(callable);
		if (callable instanceof SuClass) {
			((SuClass) callable).baseGlobal = base;
			if (members != null)
				((SuClass) callable).vars = (Map<String, Object>) members;
		}
		return callable;
	}

	private void dump(byte[] buf) {
		try {
			FileOutputStream f = new FileOutputStream(globalName + ".class");
			f.write(buf);
			f.close();
		} catch (IOException e) {
			throw new SuException("dump error");
		}
	}

	private Object[][] linkConstants(SuCallable sc) {
		if (constants == null)
			return null;
		for (Object[] cs : constants)
			if (cs != null)
				for (Object x : cs)
					if (x instanceof SuMethod) {
						SuMethod m = (SuMethod) x;
						if (m.instance == null)
							m.instance = sc;
					}
		return constants.toArray(arrayConstants);
	}

	private void genInvoke(List<FunctionSpec> functions) {
		final int SELF = 1;
		final int METHOD = 2;
		final int ARGS = 3;

		MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "invoke",
				"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
				null, null);
		mv.visitCode();
		Label begin = new Label();
		mv.visitLabel(begin);

		for (int i = 0; i < functions.size(); ++i) {
			FunctionSpec f = functions.get(i);
			if (f.name.equals("call"))
				continue;
			mv.visitVarInsn(ALOAD, METHOD);
			mv.visitLdcInsn(f.name);
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ACMPNE, l1);
			mv.visitVarInsn(ALOAD, THIS);
			mv.visitVarInsn(ALOAD, SELF);
			mv.visitVarInsn(ALOAD, ARGS);
			mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/" + globalName,
					f.name,
					"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitInsn(ARETURN);
			mv.visitLabel(l1);
		}

		// else
		//		super.invoke(self, method, args)
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitVarInsn(ALOAD, METHOD);
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuClass", "invoke",
				"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitInsn(ARETURN);

		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/" + globalName + ";",
				null,
				begin, end, 0);
		mv.visitLocalVariable("self", "Ljava/lang/String;", null,
				begin, end, 1);
		mv.visitLocalVariable("method", "Ljava/lang/String;", null,
				begin, end, 2);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null,
				begin, end, 3);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	static class Loader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	// function

	public Object startMethod(FuncOrBlock which, Object name) {
		if (f == null) {
			startTopFunction((String) name);
		} else {
			fstack.push(f);
			String fname =
					((which == FuncOrBlock.FUNC ? "_f" : "_b")
					+ fspecs.size()).intern();
			f = new Function(fname);
		}
		if (f.name.equals("call"))
			f.mv = cv.visitMethod(ACC_PUBLIC, f.name,
					"([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
		else
			f.mv = cv.visitMethod(ACC_PRIVATE, f.name,
					"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
					null, null);

		f.mv = new OptimizeToBool(f.mv);
		f.mv.visitCode();
		f.startLabel = new Label();
		f.mv.visitLabel(f.startLabel);

		f.iFspecs = fspecs.size();
		fspecs.add(null); // to reserve a slot, set correctly later

		if (which == FuncOrBlock.FUNC)
			massage();
		loadConstants();

		f.constants = new ArrayList<Object>();
		switch (which) {
		case FUNC:
			f.locals = new ArrayList<String>();
			return null;
		case BLOCK:
			f.locals = fstack.peek().locals; // use outer locals
			f.iparams = f.locals.size();
			f.isBlock = true;
			return Block;
		default:
			throw new SuException("invalid 'which' in startFunction");
		}
	}

	private void startTopFunction(String name) {
		if (cv == null) {
			startClass(null);
			isFunction = true;
		}
		if (name == null)
			name = "call";
		else if (name.equals("New"))
			name = "_init";
		else
			name = privatize(name);
		f = new Function(name);
	}

	private String privatize(String name) {
		if (Character.isLowerCase(name.charAt(0)))
			name = globalName + "_" + name;
		return name;
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
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuClass", "<init>",
				"()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lsuneido/language/" + globalName + ";",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	// massage at start of individual methods rather than in dispatcher
	// so "call" and private methods can be called directly
	// and blocks can do their own handling
	private void massage() {
		f.mv.visitVarInsn(ALOAD, THIS);
		f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + globalName,
				"params", "[Lsuneido/language/FunctionSpec;");
		iconst(f.mv, f.iFspecs);
		f.mv.visitInsn(AALOAD);
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Args", "massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		f.mv.visitVarInsn(ASTORE, f.ARGS);
	}

	private void loadConstants() {
		if (constants == null)
			constants = new ArrayList<Object[]>();
		f.iConstants = constants.size();
		constants.add(null); // to increase size, set correctly in function
		f.mv.visitVarInsn(ALOAD, THIS);
		f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + globalName,
				"constants", "[[Ljava/lang/Object;");
		iconst(f.mv, f.iConstants);
		f.mv.visitInsn(AALOAD);
		f.mv.visitVarInsn(ASTORE, f.CONSTANTS);
	}

	public Object returnStatement(Object expr, Object context) {
		if (expr == null)
			f.mv.visitInsn(ACONST_NULL);
		else if (expr == LOCAL)
			addNullCheck(expr);
		else if (expr == PARAMETER || expr == VALUE || expr == CALLRESULT)
			; // return it
		else
			dupAndStore(expr);
		if (context == Block)
			blockReturn();
		else
			f.mv.visitInsn(ARETURN);
		return "return";
	}

	private void blockReturn() {
		// stack: value
		f.mv.visitTypeInsn(NEW, "suneido/language/BlockReturnException");
		// stack: exception, value
		f.mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		f.mv.visitInsn(SWAP);
		// stack: value, exception, exception
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		f.mv.visitMethodInsn(INVOKESPECIAL,	"suneido/language/BlockReturnException",
				"<init>",
				"(Ljava/lang/Object;[Ljava/lang/Object;)V");
		// stack: exception
		f.mv.visitInsn(ATHROW);
	}

	/// called at the end of the function
	public Object function(Object params, Object compound) {

		finishMethod(compound);

		Object[] constantsArray = f.constants.toArray(arrayObject);
		constants.set(f.iConstants, constantsArray);

		FunctionSpec fs = new FunctionSpec(f.name, f.locals.toArray(arrayString),
				f.nparams, constantsArray, f.ndefaults, f.atParam);
		fspecs.set(f.iFspecs, fs);

		if (!fstack.isEmpty()) {
			String method = f.name;
			f = fstack.pop();
			return new SuMethod(null, method);
		}
		f = null;
		if (isFunction)
			return finishClass(null, null);
		else // method
			return null;
	}

	private void finishMethod(Object compound) {
		if (compound != "return")
			returnStatement(compound, null);

		finishBlockReturnCatcher();

		Label endLabel = new Label();
		f.mv.visitLabel(endLabel);
		f.mv.visitLocalVariable("this",
				"Lsuneido/language/" + globalName + ";",
				null, f.startLabel, endLabel, 0);
		if (!f.name.equals("call"))
			f.mv.visitLocalVariable("self", "Ljava/lang/Object;", null,
					f.startLabel, endLabel, f.SELF);
		f.mv.visitLocalVariable("args", "[Ljava/lang/Object;",
				null,
				f.startLabel, endLabel, f.ARGS);
		f.mv.visitLocalVariable("constants", "[Ljava/lang/Object;",
				null,
				f.startLabel, endLabel, f.CONSTANTS);
		f.mv.visitMaxs(0, 0);
		f.mv.visitEnd();
	}

	private void finishBlockReturnCatcher() {
		if (f.blockReturnCatcher == null)
			return;
		TryCatch tc = f.blockReturnCatcher;
		f.mv.visitLabel(tc.label1);
		f.mv.visitLabel(tc.label2);
		f.mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2,
				"suneido/language/BlockReturnException");

		f.mv.visitInsn(DUP);
		f.mv.visitFieldInsn(GETFIELD, "suneido/language/BlockReturnException",
				"locals", "[Ljava/lang/Object;");
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		Label label = new Label();
		f.mv.visitJumpInsn(IF_ACMPEQ, label);
		f.mv.visitInsn(ATHROW); // not ours so just re-throw
		f.mv.visitLabel(label);
		f.mv.visitFieldInsn(GETFIELD, "suneido/language/BlockReturnException",
				"returnValue", "Ljava/lang/Object;");
		f.mv.visitInsn(ARETURN);
	}

	// expressions

	public Object constant(Object value) {
		if (value == Boolean.TRUE || value == Boolean.FALSE)
			getBoolean(value == Boolean.TRUE ? "TRUE" : "FALSE");
		else if (value instanceof String)
			f.mv.visitLdcInsn(value);
		else if (value instanceof Integer) {
			iconst(f.mv, (Integer) value);
			f.mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
					"(I)Ljava/lang/Integer;");
		} else {
			int i = constantFor(value);
			f.mv.visitVarInsn(ALOAD, f.CONSTANTS);
			iconst(f.mv, i);
			f.mv.visitInsn(AALOAD);
		}
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
			self();
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
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		int i = addLocal(name);
		iconst(f.mv, i);
		return i;
	}

	private int addLocal(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = f.locals.lastIndexOf(name);
		if (i == -1) {
			i = f.locals.size();
			f.locals.add(name);
		} else if (name.equals("it"))
			f.it_param_used = true;
		return i;
	}

	public Object member(Object term, String name, boolean thisRef) {
		if (thisRef)
			name = privatize(name);
		f.mv.visitLdcInsn(name);
		getMember();
		return VALUE;
	}

	public Object subscript(Object term, Object expr) {
		assert (expr instanceof Stack);
		getMember();
		return VALUE;
	}

	public Object self() {
		f.mv.visitVarInsn(ALOAD, f.SELF == -1 ? THIS : f.SELF);
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
			String id = value.id;
			if (value.thisRef)
				id = privatize(id);
			f.mv.visitLdcInsn(id);
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

	// TODO call private methods directly, bypassing invoke
	public void preFunctionCall(Value<Object> value) {
		if (value.isMember())
			f.mv.visitLdcInsn(value.thisRef ? privatize(value.id) : value.id);
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

	private static final String[] args = new String[32];
	static {
		String s = "";
		for (int i = 0; i < args.length; ++i) {
			args[i] = s;
			s += "Ljava/lang/Object;";
		}
	}
	private void invokeFunction(int nargs) {
		f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "callN",
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
		f.mv.visitFieldInsn(GETSTATIC, "suneido/language/Args$Special", which,
				"Lsuneido/language/Args$Special;");
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

	// COULD bypass invoke (like call does)
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

	public void blockParams() {
		if (f.nparams == 0) {
			f.locals.add("it");
			f.auto_it_param = true;
		}
	}

	public Object block(Object params, Object statements) {
		if (f.auto_it_param && f.it_param_used)
			f.nparams = 1;

		finishMethod(statements);

		Object[] constantsArray = f.constants.toArray(arrayObject);
		constants.set(f.iConstants, constantsArray);

		FunctionSpec fs = new BlockSpec(f.name, f.locals.toArray(arrayString),
				f.nparams, f.atParam, f.iparams);
		fspecs.set(f.iFspecs, fs);
		int iFspecs = f.iFspecs;
		hideBlockParams();
		f = fstack.pop();

		// new SuBlock(classValue, bspec, locals)
		f.mv.visitTypeInsn(NEW, "suneido/language/SuBlock");
		f.mv.visitInsn(DUP);
		f.mv.visitVarInsn(ALOAD, THIS);
		f.mv.visitInsn(DUP);
		f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + globalName,
				"params", "[Lsuneido/language/FunctionSpec;");
		iconst(f.mv, iFspecs);
		f.mv.visitInsn(AALOAD);
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuBlock", "<init>",
				"(Ljava/lang/Object;Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)V");

		if (!f.isBlock && f.blockReturnCatcher == null) {
			f.blockReturnCatcher = new TryCatch();
			f.mv.visitLabel(f.blockReturnCatcher.label0);
		}

		return VALUE;
	}

	private void hideBlockParams() {
		for (int i = f.iparams; i < f.iparams + f.nparams; ++i)
			f.locals.set(i, "_" + f.locals.get(i));
	}

	public Object breakStatement(Object loop) {
		if (loop == Block)
			blockThrow("block:break");
		else
			gotoBreak(GOTO, loop);
		return null;
	}

	public Object continueStatement(Object loop) {
		if (loop == Block)
			blockThrow("block:continue");
		else
			gotoContinue(GOTO, loop);
		return null;
	}

	private void blockThrow(String which) {
		f.mv.visitTypeInsn(NEW, "suneido/SuException");
		f.mv.visitInsn(DUP);
		f.mv.visitLdcInsn(which);
		f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		f.mv.visitInsn(ATHROW);
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
		saveTopInVar(var);
		return loop;
	}

	private void saveTopInVar(String var) {
		f.mv.visitVarInsn(ALOAD, f.ARGS);
		f.mv.visitInsn(SWAP);
		iconst(f.mv, addLocal(var));
		f.mv.visitInsn(SWAP);
		f.mv.visitInsn(AASTORE);
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
		// stack: value
		f.mv.visitTypeInsn(NEW, "suneido/SuException");
		// stack: exception, value
		f.mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		f.mv.visitInsn(SWAP);
		// stack: value, exception, exception
		f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		// stack: exception
		f.mv.visitInsn(ATHROW);
		return null;
	}

	static class TryCatch {
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		Label label3 = new Label();
	}

	public Object startTry() {
		TryCatch tc = new TryCatch();
		f.mv.visitLabel(tc.label0);
		return tc;
	}

	public void startCatch(String var, String pattern, Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		f.mv.visitLabel(tc.label1);
		f.mv.visitJumpInsn(GOTO, tc.label3);
		f.mv.visitLabel(tc.label2);

		// exception is on stack
		if (pattern != null) {
			f.mv.visitLdcInsn(pattern);
			f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "catchMatch",
					"(Lsuneido/SuException;Ljava/lang/String;)Ljava/lang/String;");
		} else if (var != null)
			f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuException",
					"toString", "()Ljava/lang/String;");
		if (var == null)
			f.mv.visitInsn(POP);
		else
			saveTopInVar(var);
	}

	public Object catcher(String var, String pattern, Object statement) {
		return null;
	}

	// end of try-catch
	public Object tryStatement(Object tryStatement, Object catcher,
			Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		f.mv.visitLabel(tc.label3);
		f.mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2,
				"suneido/SuException");
		return null;
	}

}
