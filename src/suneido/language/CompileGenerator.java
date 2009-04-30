package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.CompileGenerator.Stack.*;
import static suneido.language.Generator.MType.CLASS;
import static suneido.language.Generator.MType.OBJECT;
import static suneido.language.ParseExpression.Value.Type.*;

import java.io.*;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.*;
import suneido.database.query.TreeQueryGenerator.MemDef;
import suneido.language.ParseExpression.Value;
import suneido.language.ParseExpression.Value.ThisOrSuper;

public class CompileGenerator implements Generator<Object> {
	private final String globalName;
    private PrintWriter pw = null;
	enum Stack { VALUE, LOCAL, PARAMETER, CALLRESULT };
	private static final String[] arrayString = new String[0];
	private static final Object[] arrayObject = new Object[0];
	private static final Object[][] arrayConstants = new Object[0][0];
	private static final FunctionSpec[] arrayFunctionSpec = new FunctionSpec[0];
	private static final int IFTRUE = IFNE;
	private static final int IFFALSE = IFEQ;
	final int THIS = 0;
	private ClassGen c = null;
	private Deque<ClassGen> cstack = null;
	private static class ClassGen {
		String name;
		ClassWriter cw;
		ClassVisitor cv;
		boolean isFunction;
		String baseClass;
		List<FunctionSpec> fspecs = null;
		Function f = null; // the current function
		Deque<Function> fstack = null; // functions nested around f
		List<Object[]> constants = null;
		int iClass = 0;
		int iBlock = 0;
		int iFunction = 0;
		boolean hasGetters = false;
	}
	private static class Function {
		Function(String name) {
			this.name = name.intern();
			if (name.equals("call")) {
				SELF = 0; // default to "this"
				ARGS = 1;
				CONSTANTS = 2;
			} else {
				SELF = 1;
				ARGS = 2;
				CONSTANTS = 3;
			}
			forInTmp = CONSTANTS + 1;
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
		int forInTmp;
	}
	static class Loop {
		public Label continueLabel = new Label();
		public Label breakLabel = new Label();
		public int var;
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

	public void startClass() {
		startClass(false);
	}

	private void startClass(boolean isFunction) {
		if (c != null) {
			if (cstack == null)
				cstack = new ArrayDeque<ClassGen>();
			cstack.push(c);
		}
		c = new ClassGen();
		c.isFunction = isFunction;
		c.baseClass =
				"suneido/language/Su" + (isFunction ? "Function" : "Class");
		c.fstack = new ArrayDeque<Function>();
		c.fspecs = new ArrayList<FunctionSpec>();

		c.cv = c.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		if (pw != null)
			c.cv = new TraceClassVisitor(c.cw, pw);

c.cv = new CheckClassAdapter(c.cv);

		c.name = javify(className());
		c.cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "suneido/language/" + c.name,
				null, c.baseClass, null);

		c.cv.visitSource("", null);

		gen_init();
	}

	private String className() {
		if (cstack == null || cstack.isEmpty())
			return globalName;
		else
			return cstack.peek().name + "_c" + c.iClass++;
	}

	public Object classConstant(String base, Object members) {
		return finishClass(base, members);
	}

	@SuppressWarnings("unchecked")
	private Object finishClass(String base, Object members) {
		genInvoke(c.fspecs);

		c.cv.visitEnd();

		if (pw != null)
			dump(c.cw.toByteArray());

		Loader loader = new Loader();
		Class<?> sc =
				loader.defineClass("suneido.language." + c.name,
				c.cw.toByteArray());
		SuCallable callable;
		try {
			callable = (SuCallable) sc.newInstance();
		} catch (InstantiationException e) {
			throw new SuException("newInstance error: " + e);
		} catch (IllegalAccessException e) {
			throw new SuException("newInstance error: " + e);
		}
		callable.params = c.fspecs.toArray(arrayFunctionSpec);
		callable.constants = linkConstants(callable);
		if (callable instanceof SuClass) {
			((SuClass) callable).baseGlobal = base;
			((SuClass) callable).vars = members != null
					? (Map<String, Object>) members
					: new HashMap<String, Object>(0);
			((SuClass) callable).hasGetters = c.hasGetters;
		}

		if (cstack != null && !cstack.isEmpty())
			c = cstack.pop();

		return callable;
	}

	private void dump(byte[] buf) {
		try {
			FileOutputStream f = new FileOutputStream(c.name + ".class");
			f.write(buf);
			f.close();
		} catch (IOException e) {
			throw new SuException("dump error");
		}
	}

	private Object[][] linkConstants(SuCallable sc) {
		if (c.constants == null)
			return null;
		for (Object[] cs : c.constants)
			if (cs != null)
				for (Object x : cs)
					if (x instanceof SuMethod) {
						SuMethod m = (SuMethod) x;
						if (m.instance == null)
							m.instance = sc;
					}
		return c.constants.toArray(arrayConstants);
	}

	private void genInvoke(List<FunctionSpec> functions) {
		final int SELF = 1;
		final int METHOD = 2;
		final int ARGS = 3;

		if (functions.size() == 0
				|| (functions.size() == 1 && functions.get(0).name == "call"))
			return;

		MethodVisitor mv = c.cv.visitMethod(ACC_PUBLIC, "invoke",
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
			mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/" + c.name,
					javify(f.name),
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
		mv.visitMethodInsn(INVOKESPECIAL, c.baseClass, "invoke",
				"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitInsn(ARETURN);

		Label end = new Label();
		mv.visitLabel(end);
		mv.visitLocalVariable("this", "Lsuneido/language/" + c.name + ";",
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
		if (c == null || c.f == null)
			startTopFunction((String) name);
		else {
			c.fstack.push(c.f);
			c.f = new Function((which == FuncOrBlock.FUNC ? functionName()
					: blockName()));
		}
		if (c.f.name.equals("call"))
			c.f.mv =
					c.cv.visitMethod(ACC_PUBLIC, javify(c.f.name),
					"([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
		else
			c.f.mv =
					c.cv.visitMethod(
							ACC_PRIVATE,
							javify(c.f.name),
					"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
					null, null);

		c.f.mv = new OptimizeToBool(c.f.mv);
		c.f.mv.visitCode();
		c.f.startLabel = new Label();
		c.f.mv.visitLabel(c.f.startLabel);

		c.f.iFspecs = c.fspecs.size();
		c.fspecs.add(null); // to reserve a slot, set correctly later

		if (which == FuncOrBlock.FUNC)
			massage();
		loadConstants();

		c.f.constants = new ArrayList<Object>();
		switch (which) {
		case FUNC:
			c.f.locals = new ArrayList<String>();
			return null;
		case BLOCK:
			c.f.locals = c.fstack.peek().locals; // use outer locals
			c.f.iparams = c.f.locals.size();
			c.f.isBlock = true;
			return Block;
		default:
			throw new SuException("invalid 'which' in startFunction");
		}
	}

	private String functionName() {
		return c.name + "_f" + c.iFunction++;
	}
	private String blockName() {
		return c.name + "_b" + c.iBlock++;
	}

	private String javify(String name) {
		if (name.endsWith("?"))
			name = name.substring(0, name.length() - 1) + "Q";
		else if (name.endsWith("!"))
			name = name.substring(0, name.length() - 1) + "X";
		return name;
	}

	private void startTopFunction(String name) {
		if (c == null)
			startClass(true);
		if (name == null)
			name = "call";
		else if (name.equals("New"))
			name = "_init";
		else
			name = privatize(name);
		if (name.startsWith("Get_"))
			c.hasGetters = true;
		c.f = new Function(name);
	}

	private String privatize(Value<Object> value) {
		return value.thisOrSuper == ThisOrSuper.THIS ? privatize(value.id)
				: value.id;
	}
	private String privatize(String name) {
		if (name.startsWith("get_") &&
				name.length() > 4 && Character.isLowerCase(name.charAt(4)))
			name = "Get_" + c.name + name.substring(3);
		else if (Character.isLowerCase(name.charAt(0)))
			name = c.name + "_" + name;
		return name;
	}

	public Object parameters(Object list, String name, Object defaultValue) {
		if (c.f.atParam = name.startsWith("@"))
			name = name.substring(1, name.length());
		c.f.locals.add(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert (i == c.f.ndefaults);
			++c.f.ndefaults;
		}
		++c.f.nparams;
		return null;
	}

	private void gen_init() {
		MethodVisitor mv =
				c.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, c.baseClass, "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lsuneido/language/" + c.name + ";",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	// massage at start of individual methods rather than in dispatcher
	// so "call" and private methods can be called directly
	// and blocks can do their own handling
	private void massage() {
		c.f.mv.visitVarInsn(ALOAD, THIS);
		c.f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + c.name,
				"params", "[Lsuneido/language/FunctionSpec;");
		iconst(c.f.mv, c.f.iFspecs);
		c.f.mv.visitInsn(AALOAD);
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Args",
				"massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		c.f.mv.visitVarInsn(ASTORE, c.f.ARGS);
	}

	private void loadConstants() {
		if (c.constants == null)
			c.constants = new ArrayList<Object[]>();
		c.f.iConstants = c.constants.size();
		c.constants.add(null); // to increase size, set correctly in function
		c.f.mv.visitVarInsn(ALOAD, THIS);
		c.f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + c.name,
				"constants", "[[Ljava/lang/Object;");
		iconst(c.f.mv, c.f.iConstants);
		c.f.mv.visitInsn(AALOAD);
		c.f.mv.visitVarInsn(ASTORE, c.f.CONSTANTS);
	}

	public Object returnStatement(Object expr, Object context) {
		if (expr == null)
			c.f.mv.visitInsn(ACONST_NULL);
		else if (expr == LOCAL)
			addNullCheck(expr);
		else if (expr == PARAMETER || expr == VALUE || expr == CALLRESULT)
			; // return it
		else
			dupAndStore(expr);
		if (context == Block)
			blockReturn();
		else
			c.f.mv.visitInsn(ARETURN);
		return "return";
	}

	private void blockReturn() {
		// stack: value
		c.f.mv.visitTypeInsn(NEW, "suneido/language/BlockReturnException");
		// stack: exception, value
		c.f.mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		c.f.mv.visitInsn(SWAP);
		// stack: value, exception, exception
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		c.f.mv.visitMethodInsn(INVOKESPECIAL,
				"suneido/language/BlockReturnException",
				"<init>",
				"(Ljava/lang/Object;[Ljava/lang/Object;)V");
		// stack: exception
		c.f.mv.visitInsn(ATHROW);
	}

	/// called at the end of the function
	public Object function(Object params, Object compound) {

		finishMethod(compound);

		Object[] constantsArray = c.f.constants.toArray(arrayObject);
		c.constants.set(c.f.iConstants, constantsArray);

		FunctionSpec fs =
				new FunctionSpec(c.f.name, c.f.locals.toArray(arrayString),
						c.f.nparams, constantsArray, c.f.ndefaults, c.f.atParam);
		c.fspecs.set(c.f.iFspecs, fs);

		if (!c.fstack.isEmpty()) {
			String method = c.f.name;
			c.f = c.fstack.pop();
			return new SuMethod(null, method);
		}
		c.f = null;
		if (c.isFunction)
			return finishClass(null, null);
		else // method
			return null;
	}

	private void finishMethod(Object compound) {
		if (compound != "return")
			returnStatement(compound, null);

		finishBlockReturnCatcher();

		Label endLabel = new Label();
		c.f.mv.visitLabel(endLabel);
		c.f.mv.visitLocalVariable("this",
				"Lsuneido/language/" + c.name + ";",
				null, c.f.startLabel, endLabel, 0);
		if (!c.f.name.equals("call"))
			c.f.mv.visitLocalVariable("self", "Ljava/lang/Object;", null,
					c.f.startLabel, endLabel, c.f.SELF);
		c.f.mv.visitLocalVariable("args", "[Ljava/lang/Object;",
				null,
				c.f.startLabel, endLabel, c.f.ARGS);
		c.f.mv.visitLocalVariable("constants", "[Ljava/lang/Object;",
				null,
				c.f.startLabel, endLabel, c.f.CONSTANTS);
		c.f.mv.visitMaxs(0, 0);
		c.f.mv.visitEnd();
	}

	private void finishBlockReturnCatcher() {
		if (c.f.blockReturnCatcher == null)
			return;
		TryCatch tc = c.f.blockReturnCatcher;
		c.f.mv.visitLabel(tc.label1);
		c.f.mv.visitLabel(tc.label2);
		c.f.mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2,
				"suneido/language/BlockReturnException");

		c.f.mv.visitInsn(DUP);
		c.f.mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"locals", "[Ljava/lang/Object;");
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		Label label = new Label();
		c.f.mv.visitJumpInsn(IF_ACMPEQ, label);
		c.f.mv.visitInsn(ATHROW); // not ours so just re-throw
		c.f.mv.visitLabel(label);
		c.f.mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"returnValue", "Ljava/lang/Object;");
		c.f.mv.visitInsn(ARETURN);
	}

	// expressions

	public Object constant(Object value) {
		if (value == Boolean.TRUE || value == Boolean.FALSE)
			getBoolean(value == Boolean.TRUE ? "TRUE" : "FALSE");
		else if (value instanceof String)
			c.f.mv.visitLdcInsn(value);
		else if (value instanceof Integer) {
			iconst(c.f.mv, (Integer) value);
			c.f.mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer",
					"valueOf",
					"(I)Ljava/lang/Integer;");
		} else {
			int i = constantFor(value);
			c.f.mv.visitVarInsn(ALOAD, c.f.CONSTANTS);
			iconst(c.f.mv, i);
			c.f.mv.visitInsn(AALOAD);
		}
		return VALUE;
	}

	private int constantFor(Object value) {
		int i = c.f.constants.indexOf(value);
		return i == -1 ? addConstant(value) : i;
	}

	private int addConstant(Object value) {
		c.f.constants.add(value);
		return c.f.constants.size() - 1;
	}

	private void iconst(MethodVisitor mv, int i) {
		if (-1 <= i && i <= 5)
			mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			mv.visitIntInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, i);
		else
			mv.visitLdcInsn(i);
	}

	public Object identifier(String name) {
		if (name.equals("this"))
			selfRef();
		else if (Character.isLowerCase(name.charAt(0))) {
			int i = localRef(name);
			c.f.mv.visitInsn(AALOAD);
			return i < c.f.nparams ? PARAMETER : LOCAL;
		} else {
			c.f.mv.visitLdcInsn(name);
			c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals",
					"get",
					"(Ljava/lang/String;)Ljava/lang/Object;");
		}
		return VALUE;
	}

	private int localRef(String name) {
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		int i = addLocal(name);
		iconst(c.f.mv, i);
		return i;
	}

	private int addLocal(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = c.f.locals.lastIndexOf(name);
		if (i == -1) {
			i = c.f.locals.size();
			c.f.locals.add(name);
		} else if (name.equals("it"))
			c.f.it_param_used = true;
		return i;
	}

	public Object member(Object term, Value<Object> value) {
		c.f.mv.visitLdcInsn(privatize(value));
		getMember();
		return VALUE;
	}

	public Object subscript(Object term, Object expr) {
		dupAndStore(expr);
		getMember();
		return VALUE;
	}

	public Object selfRef() {
		c.f.mv.visitVarInsn(ALOAD, c.f.SELF);
		return VALUE;
	}

	public Object superRef() {
		c.f.mv.visitVarInsn(ALOAD, THIS);
		c.f.mv.visitVarInsn(ALOAD, c.f.SELF);
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
			c.f.mv.visitLdcInsn(privatize(value));
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
				c.f.mv.visitInsn(SWAP);
			binaryMethod(op);
		}
		return value.type;
	}

	private void addNullCheck(Object expr) {
		Label label = new Label();
		c.f.mv.visitInsn(DUP);
		c.f.mv.visitJumpInsn(IFNONNULL, label);
		c.f.mv.visitTypeInsn(NEW, "suneido/SuException");
		c.f.mv.visitInsn(DUP);
		c.f.mv.visitLdcInsn(expr == LOCAL ? "uninitialized variable"
				: "no return value");
		c.f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		c.f.mv.visitInsn(ATHROW);
		c.f.mv.visitLabel(label);
	}

	private void unaryMethod(String method, String type) {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", method,
				"(Ljava/lang/Object;)Ljava/lang/" + type + ";");
	}
	private void binaryMethod(Token op) {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type);
	}
	private void getMember() {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "get",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	}

	private void putMember() {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "put",
				"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
	}

	public Object preIncDec(Object term, Token incdec, Value<Object> value) {
		// stack: i args (or: member object)
		c.f.mv.visitInsn(DUP2);
		// stack: i args i args
		load(value.type);
		// stack: v i args
		unaryMethod(incdec == Token.INC ? "add1" : "sub1", "Number");
		return value.type;
	}

	public Object postIncDec(Object term, Token incdec, Value<Object> value) {
		// stack: i args
		c.f.mv.visitInsn(DUP2);
		// stack: i args i args
		load(value.type);
		// stack: v i args
		c.f.mv.visitInsn(DUP_X2);
		// stack: v i args v
		unaryMethod(incdec == Token.INC ? "add1" : "sub1", "Number");
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
	public Object rvalue(Object expr) {
		dupAndStore(expr);
		return VALUE;
	}
	private void dupAndStore(Object expr) {
		if (!(expr instanceof Value.Type))
			return;
		c.f.mv.visitInsn(DUP_X2);
		store(expr);
	}

	private void store(Object type) {
		if (type == IDENTIFIER)
			c.f.mv.visitInsn(AASTORE);
		else if (type == MEMBER || type == SUBSCRIPT)
			putMember();
		else
			throw new SuException("unknown store type: " + type);
	}

	private void load(Object type) {
		if (type == IDENTIFIER)
			c.f.mv.visitInsn(AALOAD);
		else if (type == MEMBER || type == SUBSCRIPT)
			getMember();
		else
			throw new SuException("unknown load type: " + type);
	}

	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		dupAndStore(expr2);
		binaryMethod(op);
		return VALUE;
	}

	public Object unaryExpression(Token op, Object expression) {
		dupAndStore(expression);
		switch (op) {
		case SUB:
			unaryMethod("uminus", "Number");
			break;
		case ADD:
			// should have a uplus, but cSuneido doesn't
			break;
		case NOT:
			unaryMethod("not", "Boolean");
			break;
		case BITNOT:
			unaryMethod("bitnot", "Number");
			break;
		default:
			throw new SuException("invalid unaryExpression op: " + op);
		}
		return VALUE;
	}

	// MAYBE call private methods directly, bypassing invoke
	public void preFunctionCall(Value<Object> value) {
		if (value.isMember())
			c.f.mv.visitLdcInsn(privatize(value));
		else if (value.type == SUBSCRIPT)
			c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
					"toMethodString", "(Ljava/lang/Object;)Ljava/lang/String;");
	}

	public Object functionCall(Object function, Value<Object> value,
			Object arguments) {
		int nargs = arguments == null ? 0 : (Integer) arguments;
		if (value.thisOrSuper == ThisOrSuper.SUPER)
			invokeSuperInit(nargs);
		else if (value.type == MEMBER || value.type == SUBSCRIPT)
			invokeMethod(nargs);
		else
			invokeFunction(nargs);
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
	private void invokeSuperInit(int nargs) {
		c.f.mv.visitMethodInsn(INVOKEVIRTUAL, c.baseClass,
				"superInvokeN", "(Ljava/lang/Object;Ljava/lang/String;"
						+ args[nargs] + ")Ljava/lang/Object;");
	}
	private void invokeFunction(int nargs) {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "callN",
				"(Ljava/lang/Object;" + args[nargs] + ")Ljava/lang/Object;");
	}
	private void invokeMethod(int nargs) {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invokeN",
				"(Ljava/lang/Object;Ljava/lang/String;" + args[nargs]
						+ ")Ljava/lang/Object;");
	}

	public Object argumentList(Object list, String keyword, Object expression) {
		dupAndStore(expression);
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
		dupAndStore(expr);
		return 2;
	}
	private void specialArg(String which) {
		c.f.mv.visitFieldInsn(GETSTATIC, "suneido/language/Args$Special",
				which,
				"Lsuneido/language/Args$Special;");
	}

	public Object and(Object prev) {
		getBoolean("FALSE");
		Label label = (prev == null ? new Label() : (Label) prev);
		c.f.mv.visitJumpInsn(IF_ACMPEQ, label);
		return label;
	}
	public void andEnd(Object label) {
		if (label == null)
			return;
		Label l0 = new Label();
		c.f.mv.visitJumpInsn(GOTO, l0);
		c.f.mv.visitLabel((Label) label);
		getBoolean("FALSE");
		c.f.mv.visitLabel(l0);
	}

	private void getBoolean(String which) {
		c.f.mv.visitFieldInsn(GETSTATIC,
				"java/lang/Boolean", which,
				"Ljava/lang/Boolean;");
	}
	public Object and(Object expr1, Object expr2) {
		return VALUE;
	}

	public Object or(Object prev) {
		getBoolean("TRUE");
		Label label = (prev == null ? new Label() : (Label) prev);
		c.f.mv.visitJumpInsn(IF_ACMPEQ, label);
		return label;
	}
	public void orEnd(Object label) {
		if (label == null)
			return;
		Label l0 = new Label();
		c.f.mv.visitJumpInsn(GOTO, l0);
		c.f.mv.visitLabel((Label) label);
		getBoolean("TRUE");
		c.f.mv.visitLabel(l0);
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
		c.f.mv.visitLabel((Label) label);
		return VALUE;
	}

	public Object in(Object expression, Object constant) {
		throw new SuException("'in' is only implemented for queries");
	}

	// COULD bypass invoke (like call does)
	public void newCall() {
		c.f.mv.visitLdcInsn("<new>");
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
			c.f.mv.visitInsn(POP);
		else if (list instanceof Value.Type)
			store(list);
	}

	// statements

	public Object statementList(Object list, Object next) {
		return next;
	}

	public void addSuperInit() {
		if (!c.f.name.equals("_init"))
			return;
		c.f.mv.visitVarInsn(ALOAD, THIS);
		c.f.mv.visitVarInsn(ALOAD, c.f.SELF);
		c.f.mv.visitLdcInsn("_init");
		invokeSuperInit(0);
		c.f.mv.visitInsn(POP);
	}

	public Object expressionStatement(Object expression) {
		return expression;
	}

	public Object ifExpr(Object expr) {
		toBool(expr);
		Label label = new Label();
		c.f.mv.visitJumpInsn(IFEQ, label);
		return label;
	}

	private void toBool(Object expr) {
		dupAndStore(expr);
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "toBool",
				"(Ljava/lang/Object;)I");
	}
	public void ifThen(Object label, Object t) {
		afterStatement(t);
	}
	public Object ifElse(Object pastThen) {
		Label pastElse = new Label();
		c.f.mv.visitJumpInsn(GOTO, pastElse);
		c.f.mv.visitLabel((Label) pastThen);
		return pastElse;
	}
	public Object ifStatement(Object expr, Object t, Object e, Object afterIf) {
		afterStatement(e);
		c.f.mv.visitLabel((Label) afterIf);
		return null;
	}

	public Object loop() {
		Loop loop = new Loop();
		c.f.mv.visitLabel(loop.continueLabel);
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
		if (c.f.nparams == 0) {
			c.f.locals.add("it");
			c.f.auto_it_param = true;
		}
	}

	public Object block(Object params, Object statements) {
		if (c.f.auto_it_param && c.f.it_param_used)
			c.f.nparams = 1;

		finishMethod(statements);

		Object[] constantsArray = c.f.constants.toArray(arrayObject);
		c.constants.set(c.f.iConstants, constantsArray);

		FunctionSpec fs =
				new BlockSpec(c.f.name, c.f.locals.toArray(arrayString),
						c.f.nparams, c.f.atParam, c.f.iparams);
		c.fspecs.set(c.f.iFspecs, fs);
		int iFspecs = c.f.iFspecs;
		hideBlockParams();
		c.f = c.fstack.pop();

		// new SuBlock(classValue, bspec, locals)
		c.f.mv.visitTypeInsn(NEW, "suneido/language/SuBlock");
		c.f.mv.visitInsn(DUP);
		c.f.mv.visitVarInsn(ALOAD, THIS);
		c.f.mv.visitInsn(DUP);
		c.f.mv.visitFieldInsn(GETFIELD, "suneido/language/" + c.name,
				"params", "[Lsuneido/language/FunctionSpec;");
		iconst(c.f.mv, iFspecs);
		c.f.mv.visitInsn(AALOAD);
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		c.f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuBlock",
				"<init>",
				"(Ljava/lang/Object;Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)V");

		if (!c.f.isBlock && c.f.blockReturnCatcher == null) {
			c.f.blockReturnCatcher = new TryCatch();
			c.f.mv.visitLabel(c.f.blockReturnCatcher.label0);
		}

		return VALUE;
	}

	private void hideBlockParams() {
		for (int i = c.f.iparams; i < c.f.iparams + c.f.nparams; ++i)
			c.f.locals.set(i, "_" + c.f.locals.get(i));
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
		c.f.mv.visitTypeInsn(NEW, "suneido/SuException");
		c.f.mv.visitInsn(DUP);
		c.f.mv.visitLdcInsn(which);
		c.f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/String;)V");
		c.f.mv.visitInsn(ATHROW);
	}

	public Object dowhileStatement(Object body, Object expr, Object loop) {
		toBool(expr);
		gotoContinue(IFTRUE, loop);
		setBreak(loop);
		return null;
	}

	private void gotoContinue(int op, Object loop) {
		c.f.mv.visitJumpInsn(op, ((Loop) loop).continueLabel);
	}
	private void gotoBreak(int op, Object loop) {
		c.f.mv.visitJumpInsn(op, ((Loop) loop).breakLabel);
	}
	private void setBreak(Object loop) {
		c.f.mv.visitLabel(((Loop) loop).breakLabel);
	}

	public Object forStart() {
		Label label = new Label();
		c.f.mv.visitJumpInsn(GOTO, label);
		return label;
	}
	public void forIncrement(Object label) {
		if (label != null)
			c.f.mv.visitLabel((Label) label);
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
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				"iterator",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		c.f.mv.visitVarInsn(ASTORE, c.f.forInTmp);

		Object loop = loop();

		c.f.mv.visitVarInsn(ALOAD, c.f.forInTmp);
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "hasNext",
				"(Ljava/lang/Object;)Z");
		gotoBreak(IFFALSE, loop);

		c.f.mv.visitVarInsn(ALOAD, c.f.forInTmp);
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "next",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		saveTopInVar(var);
		++c.f.forInTmp;
		return loop;
	}

	private void saveTopInVar(String var) {
		c.f.mv.visitVarInsn(ALOAD, c.f.ARGS);
		c.f.mv.visitInsn(SWAP);
		iconst(c.f.mv, addLocal(var));
		c.f.mv.visitInsn(SWAP);
		c.f.mv.visitInsn(AASTORE);
	}

	public Object forInStatement(String var, Object expr, Object statement,
			Object loop) {
		--c.f.forInTmp;
		endLoop(statement, loop);
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
			c.f.mv.visitLabel(slabels.next);
		slabels.next = null;
		slabels.body = null;
	}
	public void startCaseValue() {
		c.f.mv.visitInsn(DUP);
	}
	public Object caseValues(Object values, Object expr, Object labels,
			boolean more) {
		c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "is_",
				"(Ljava/lang/Object;Ljava/lang/Object;)Z");
		SwitchLabels slabels = (SwitchLabels) labels;
		if (more) {
			if (slabels.body == null)
				slabels.body = new Label();
			c.f.mv.visitJumpInsn(IFTRUE, slabels.body);
		} else {
			if (slabels.next == null)
				slabels.next = new Label();
			c.f.mv.visitJumpInsn(IFFALSE, slabels.next);
		}
		return null;
	}
	public void startCaseBody(Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.body != null)
			c.f.mv.visitLabel(slabels.body);
		c.f.mv.visitInsn(POP);
	}
	public Object switchCases(Object cases, Object values, Object statements,
			Object labels) {
		afterStatement(statements);
		c.f.mv.visitJumpInsn(GOTO, ((SwitchLabels) labels).end);
		return null;
	}
	public Object switchStatement(Object expr, Object cases, Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.next != null)
			c.f.mv.visitLabel(((SwitchLabels) labels).next);
		c.f.mv.visitInsn(POP);
		c.f.mv.visitLabel(((SwitchLabels) labels).end);
		return null;
	}

	public Object throwStatement(Object expression) {
		dupAndStore(expression);
		// stack: value
		c.f.mv.visitTypeInsn(NEW, "suneido/SuException");
		// stack: exception, value
		c.f.mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		c.f.mv.visitInsn(SWAP);
		// stack: value, exception, exception
		c.f.mv.visitMethodInsn(INVOKESPECIAL, "suneido/SuException", "<init>",
				"(Ljava/lang/Object;)V");
		// stack: exception
		c.f.mv.visitInsn(ATHROW);
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
		c.f.mv.visitLabel(tc.label0);
		return tc;
	}

	public void startCatch(String var, String pattern, Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		c.f.mv.visitLabel(tc.label1);
		c.f.mv.visitJumpInsn(GOTO, tc.label3);
		c.f.mv.visitLabel(tc.label2);

		// exception is on stack
		if (pattern != null) {
			c.f.mv.visitLdcInsn(pattern);
			c.f.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
					"catchMatch",
					"(Lsuneido/SuException;Ljava/lang/String;)Ljava/lang/String;");
		} else if (var != null)
			c.f.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuException",
					"toString", "()Ljava/lang/String;");
		if (var == null)
			c.f.mv.visitInsn(POP);
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
		c.f.mv.visitLabel(tc.label3);
		c.f.mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2,
				"suneido/SuException");
		return null;
	}

	public List<Object[]> getConstants() {
		return c == null ? null : c.constants;
	}

}
