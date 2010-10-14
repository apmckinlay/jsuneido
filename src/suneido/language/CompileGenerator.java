package suneido.language;

import static org.objectweb.asm.Opcodes.*;
import static suneido.language.Generator.MType.CLASS;
import static suneido.language.Generator.MType.OBJECT;

import java.io.*;
import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.*;
import suneido.database.query.TreeQueryGenerator.MemDef;

import com.google.common.base.Objects;

@ThreadSafe
public class CompileGenerator extends Generator<Object> {
	private final String globalName;
	private final Deque<String> suClassName = new ArrayDeque<String>();
	private String name = null;
    private PrintWriter pw = null;
	private static final String[] arrayString = new String[0];
	private static final Object[] arrayObject = new Object[0];
	private static final int IFTRUE = IFNE;
	private static final int IFFALSE = IFEQ;
	private static final int THIS = 0;
	private static final int SELF = 1;
	private static final int ARGS = 2;
	private static final int CONSTANTS = 3;
	private ClassGen c = null;
	/** java classes (suneido functions/method/blocks) currently being generated */
	private Deque<ClassGen> cstack = null;
	private static class ClassGen {
		ClassGen(String baseClass, String name, boolean isMethod) {
			this.baseClass = baseClass;
			this.name = name;
			this.isMethod = isMethod;
		}
		final String baseClass;
		final String name;
		final boolean isMethod;
		ClassWriter cw;
		ClassVisitor cv;
		FunctionSpec fspec = null;
		List<Object> constants = null;

		MethodVisitor mv;
		Label startLabel;
		List<String> locals;
		int ndefaults = 0;
		int nparams = 0;
		boolean atParam;
		int iparams; // where block params start in locals
		boolean auto_it_param = false;
		boolean it_param_used = false;
		TryCatch blockReturnCatcher = null;
		int forInTmp = CONSTANTS + 1;
		boolean superInitCalled = false;
		final List<Stack> stack = new ArrayList<Stack>();

		boolean isBlock() {
			return baseClass.equals("suneido/language/SuCallable");
		}
	}
	static class Loop {
		public Label continueLabel = new Label();
		public Label breakLabel = new Label();
		public Label doLabel = new Label(); // used by do-while
		public int var;
	}
	static final Object Block = new Object();
	public static final char METHOD_SEPARATOR = '\u00A3';

	public CompileGenerator(String name) {
		this.globalName = name;
	}

	public CompileGenerator(String name, PrintWriter pw) {
		this(name);
		this.pw = pw;
	}

	// constants

	@Override
	public Object bool(boolean value) {
		return value ? Boolean.TRUE : Boolean.FALSE;
	}

	@Override
	public Object number(String value) {
		return Ops.stringToNumber(value);
	}

	@Override
	public Object string(String value) {
		return value;
	}

	@Override
	public Object symbol(String value) {
		return value; // same as string for now
	}

	@Override
	public Object date(String value) {
		return Ops.stringToDate(value);
	}

	@Override
	public void objectBegin() {
	}

	@Override
	public Object objectEnd(MType which, Object members) {
		if (members == null)
			members = which == OBJECT ? new SuContainer() : new SuRecord();
		((SuContainer) members).setReadonly();
		return members;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object memberList(MType which, Object list, Object member) {
		MemDef m = (MemDef) member;
		if (which == CLASS) {
			if (m.value == null)
				return list;
			Map<String, Object> vars = (list == null
					? new HashMap<String, Object>()
					: (Map<String,Object>) list);
			vars.put(privatize2((String) m.name), m.value);
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

	@Override
	public Object memberDefinition(Object name, Object value) {
		return new MemDef(name, value);
	}

	@Override
	public void classBegin(String memberName) {
		nameBegin(memberName, "$c");
		suClassName.push(name);
	}

	private void nameBegin(String memberName, String def) {
		if (name == null)
			name = javify(globalName);
		else if (memberName != null)
			name += def.substring(0, 1) + javify(memberName);
		else
			name += def;
	}

	@Override
	public Object classEnd(String base, Object members) {
		if (base != null && base.startsWith("_"))
			base = Globals.overload(base);
		SuClass c = new SuClass(name, base, members);
		suClassName.pop();
		nameEnd();
		return c;
	}

	private void nameEnd() {
		int i = Math.max(name.lastIndexOf('$'), name.lastIndexOf(METHOD_SEPARATOR));
		name = i == -1 ? "" : name.substring(0, i);
	}

	private void startJavaClass(String base, String name, boolean isMethod) {
		if (c != null) {
			if (cstack == null)
				cstack = new ArrayDeque<ClassGen>();
			cstack.push(c);
		}
		c = new ClassGen(base, name, isMethod);

		//TODO don't use COMPUTE_FRAMES
		c.cv = c.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		if (pw != null)
			c.cv = new TraceClassVisitor(c.cw, pw);
		c.cv = new CheckClassAdapter(c.cv, false);

		c.cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER,
				"suneido/language/" + c.name, null, c.baseClass, null);

		c.cv.visitSource("", null);

		gen_init();
	}

	@Override
	public void finish() {
	}

	private SuCallable finishClass() {

		c.cv.visitEnd();

		if (pw != null)
			dump(c.cw.toByteArray());

		Loader loader = new Loader();
		Class<?> sc = loader.defineClass("suneido.language." + c.name,
				c.cw.toByteArray());

		// TODO use reflection to call constructor taking params and constants
		// so they can be final
		SuCallable callable;
		try {
			callable = (SuCallable) sc.newInstance();
		} catch (InstantiationException e) {
			throw new SuException("newInstance error: " + e);
		} catch (IllegalAccessException e) {
			throw new SuException("newInstance error: " + e);
		}
		callable.params = c.fspec;
		callable.constants = c.constants == null ? null
				: c.constants.toArray(arrayObject);

		c = null;
		if (cstack != null && !cstack.isEmpty())
			c = cstack.pop();

		nameEnd();

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

	static class Loader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	// functions

	@Override
	public void functionBegin(String memberName, boolean isMethod) {
		// name is either null or member if class method
		nameBegin(memberName, isMethod ? METHOD_SEPARATOR + "f" : "$f");
		startJavaClass("suneido/language/SuFunction", name, isMethod);

		startMethod("call");
		massage();
		loadConstants();

		c.locals = new ArrayList<String>();
	}

	@Override
	public Object blockBegin() {
		nameBegin(null, "$b");
		startJavaClass("suneido/language/SuCallable", name, c.isMethod);

		startMethod("eval");
		// massage done in SuBlock.call
		loadConstants();

		c.locals = cstack.peek().locals; // use outer locals
		c.iparams = c.locals.size();
		return Block;
	}

	private void startMethod(String name) {
		c.mv = methodVisitor(ACC_PUBLIC, name,
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");

		// TODO replace OptimizeToBool with defer
		c.mv = new OptimizeToBool(c.mv);
		c.mv.visitCode();
		c.startLabel = new Label();
		c.mv.visitLabel(c.startLabel);

		c.constants = new ArrayList<Object>();
	}

	private MethodVisitor methodVisitor(int access, String name, String desc) {
		MethodVisitor mv = c.cv.visitMethod(access, name, desc, null, null);
		mv = new TryCatchBlockSorter(mv, access, name, desc, null, null);
		return mv;
	}

	public static String javify(String name) {
		return name.replace('?', 'Q').replace('!', 'X');
	}

	private String privatize(String name) {
		return c.isMethod ? privatize2(name) : name;
	}

	private String privatize2(String name) {
		if (name.startsWith("get_") &&
				name.length() > 4 && Character.isLowerCase(name.charAt(4)))
			return "Get_" + suClassName.peek() + name.substring(3);
		if (Character.isLowerCase(name.charAt(0)))
			return suClassName.peek() + "_" + name;
		return name;
	}

	@Override
	public Object parameters(Object list, String name, Object defaultValue) {
		if (c.atParam = name.startsWith("@"))
			name = name.substring(1, name.length());
		c.locals.add(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert (i == c.ndefaults);
			++c.ndefaults;
		}
		++c.nparams;
		return null;
	}

	/** called by startClass to give every class a no arg <init> */
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
		c.mv.visitVarInsn(ALOAD, THIS);
		c.mv.visitFieldInsn(GETFIELD, "suneido/language/" + c.name,
				"params", "Lsuneido/language/FunctionSpec;");
		c.mv.visitVarInsn(ALOAD, ARGS);
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Args",
				"massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		c.mv.visitVarInsn(ASTORE, ARGS);
	}

	private void loadConstants() {
		c.mv.visitVarInsn(ALOAD, THIS);
		c.mv.visitFieldInsn(GETFIELD, "suneido/language/" + c.name,
				"constants", "[Ljava/lang/Object;");
		c.mv.visitVarInsn(ASTORE, CONSTANTS);
	}

	@Override
	public Object returnStatement(Object expr, Object context) {
		if (stackEmpty())
			c.mv.visitInsn(ACONST_NULL);
		else {
			Stack rvalue = pop().forceValue();
			// NOTE: special case, no null check for returning call result
			if (rvalue == RVALUE_LOCAL)
				addNullCheck(rvalue);
		}
		assert stackEmpty();
		if (c.isBlock() && context != normalReturn)
			blockReturn();
		else
			c.mv.visitInsn(ARETURN);
		return "return";
	}

	// note: can't use preconstructed exception
	// because we have to include the return value
	// TODO use pre-constructed if return value is null (and maybe true/false)
	private void blockReturn() {
		// stack: value
		c.mv.visitTypeInsn(NEW, "suneido/language/BlockReturnException");
		// stack: exception, value
		c.mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		c.mv.visitInsn(SWAP);
		// stack: value, exception, exception
		c.mv.visitVarInsn(ALOAD, ARGS);
		c.mv.visitMethodInsn(INVOKESPECIAL,
				"suneido/language/BlockReturnException",
				"<init>",
				"(Ljava/lang/Object;[Ljava/lang/Object;)V");
		// stack: exception
		c.mv.visitInsn(ATHROW);
	}

	@Override
	public Object functionEnd(Object params, Object compound) {

		finishMethod(compound);

		Object[] constantsArray = c.constants.toArray(arrayObject);

		c.fspec = new FunctionSpec(c.name, c.locals.toArray(arrayString),
				c.nparams, constantsArray, c.ndefaults, c.atParam);
		return finishClass();
	}

	private static final Object normalReturn = new Object();

	private void finishMethod(Object compound) {
		if (compound != "return")
			returnStatement(compound, normalReturn);

		finishBlockReturnCatcher();

		Label endLabel = new Label();
		c.mv.visitLabel(endLabel);
		c.mv.visitLocalVariable("this", "Lsuneido/language/" + c.name + ";",
				null, c.startLabel, endLabel, THIS);
		c.mv.visitLocalVariable("self", "Ljava/lang/Object;", null,
				c.startLabel, endLabel, SELF);
		c.mv.visitLocalVariable("args", "[Ljava/lang/Object;", null,
				c.startLabel, endLabel, ARGS);
		c.mv.visitLocalVariable("constants", "[Ljava/lang/Object;", null,
				c.startLabel, endLabel, CONSTANTS);
		c.mv.visitMaxs(0, 0);
		c.mv.visitEnd();
	}

	private void finishBlockReturnCatcher() {
		if (c.blockReturnCatcher == null)
			return;
		TryCatch tc = c.blockReturnCatcher;
		c.mv.visitLabel(tc.label1);
		c.mv.visitLabel(tc.label2);

		c.mv.visitInsn(DUP);
		c.mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"locals", "[Ljava/lang/Object;");
		c.mv.visitVarInsn(ALOAD, ARGS);
		Label label = new Label();
		c.mv.visitJumpInsn(IF_ACMPEQ, label);
		c.mv.visitInsn(ATHROW); // not ours so just re-throw
		c.mv.visitLabel(label);
		c.mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"returnValue", "Ljava/lang/Object;");
		c.mv.visitInsn(ARETURN);
	}

	// expressions

	@Override
	public Object constant(Object value) {
		push(new DeferConstant(value));
		return null;
	}

	private int constantFor(Object value) {
		int i = c.constants.indexOf(value);
		return i == -1 || value.getClass() != c.constants.get(i).getClass()
				? addConstant(value) : i;
	}

	private int addConstant(Object value) {
		c.constants.add(value);
		return c.constants.size() - 1;
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

	@Override
	public Object identifier(String name) {
		push(new DeferIdentifier(name)); // defer in case of assignment
		return null;
	}

	private int localRef(String name) {
		c.mv.visitVarInsn(ALOAD, ARGS);
		int i = addLocal(name);
		iconst(c.mv, i);
		return i;
	}

	private int addLocal(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = c.locals.lastIndexOf(name);
		if (i == -1) {
			i = c.locals.size();
			c.locals.add(name);
		} else if (name.equals("it"))
			c.it_param_used = true;
		return i;
	}

	@Override
	public Object member(Object term, String identifier) {
		force();
		push(new DeferMember(pop(), identifier));
		return null;
	}

	@Override
	public Object selfRef() {
		push(DEFER_SELF);
		return null;
	}

	@Override
	public Object subscript(Object term, Object expr) {
		force();
		pop(2);
		push(new DeferSubscript());
		return null;
	}

	@Override
	public Object lvalueForAssign(Object value, Token op) {
		forceLvalue();
		if (op != Token.EQ) {
			// stack: L1, L2, ...
			c.mv.visitInsn(DUP2);
			// stack: L1, L2, L1, L2, ...
			top().load();
			// stack: v, L1, L2, ...
		}
		return null;
	}

	/**
	 * assignments are deferred because store's lose the expression value so if
	 * we need the value we have to dup before storing but we don't know if we
	 * need the value until later
	 */
	@Override
	public Object assignment(Object left, Token op, Object right) {
		force();
		if (op != Token.EQ)
			binaryMethod(op);
		Stack rvalue = pop();
		Stack lvalue = pop();
		push(new DeferAssignment(lvalue, op, rvalue));
		return null;
	}

	private Stack addNullCheck(Stack expr) {
		if (expr != RVALUE_LOCAL && expr != RVALUE_RETURN)
			return expr;
		assert expr == RVALUE_LOCAL || expr == RVALUE_RETURN;
		Label label = new Label();
		c.mv.visitInsn(DUP);
		c.mv.visitJumpInsn(IFNONNULL, label);
		c.mv.visitLdcInsn(expr == RVALUE_LOCAL ? "uninitialized variable"
				: "no return value");
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "thrower",
				"(Ljava/lang/Object;)V");
		c.mv.visitLabel(label);
		return RVALUE;
	}

	private void unaryMethod(String method, String type) {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", method,
				"(Ljava/lang/Object;)Ljava/lang/" + type + ";");
	}
	private void binaryMethod(Token op) {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type);
	}
	private void getMember() {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "get",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	}

	private void putMember() {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "put",
				"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
	}

	@Override
	public Object preIncDec(Object term, Token incdec) {
		forceLvalue();
		Stack lvalue = pop();
		// stack: i args (or: member object)
		c.mv.visitInsn(DUP2);
		// stack: i args i args
		lvalue.load();
		// stack: v i args
		unaryMethod(incdec == Token.INC ? "add1" : "sub1", "Number");
		push(new DeferAssignment(lvalue, Token.EQ, RVALUE));
		return null;
	}

	@Override
	public Object postIncDec(Object term, Token incdec) {
		forceLvalue();
		Stack lvalue = pop();
		// stack: i args
		c.mv.visitInsn(DUP2);
		// stack: i args i args
		lvalue.load();
		// stack: v i args
		c.mv.visitInsn(DUP_X2);
		// stack: v i args v
		unaryMethod(incdec == Token.INC ? "add1" : "sub1", "Number");
		// stack: v+1 i args v
		lvalue.store();
		// stack: v
		push(RVALUE);
		return null;
	}

	@Override
	public Object binaryExpression(Token op, Object expr1, Object expr2) {
		force();
		pop();
		pop();
		push(RVALUE);
		binaryMethod(op);
		return null;
	}

	@Override
	public Object unaryExpression(Token op, Object expr) {
		force();
		pop();
		push(RVALUE);
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
			unaryMethod("bitnot", "Integer");
			break;
		default:
			throw new SuException("invalid unaryExpression op: " + op);
		}
		return null;
	}

	@Override
	public Object functionCallTarget(Object function) {
		if (top() instanceof DeferMember) {
			forceLvalue();
		} else if (top() instanceof DeferSubscript) {
			c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
						"toMethodString", "(Ljava/lang/Object;)Ljava/lang/String;");
			forceLvalue();
		} else if (top() instanceof DeferIdentifier) {
			DeferIdentifier di = (DeferIdentifier) top();
			if (di.isGlobal() && di.name.equals("Object")) {
				pop();
				push(new DirectCall(di.name));
			}
		}
		force();
		return null;
	}

	@Override
	public Object superCallTarget(String method) {
		if (method.equals("New")) {
			if (c.superInitCalled)
				throw new SuException("call to super must come first");
			else if (!c.name.endsWith(METHOD_SEPARATOR + "New"))
				throw new SuException("super call only allowed in New");
		}
		force();
		c.mv.visitVarInsn(ALOAD, THIS);
		c.mv.visitVarInsn(ALOAD, SELF);
		c.mv.visitLdcInsn(method);
		push(LVALUE_SUPER_MEMBER);
		return null;
	}

	@Override
	public Object functionCall(Object function, Object args) {
		Args a = args == null ? noArgs : (Args) args;
		processConstArgs(a);
		force();

		Stack fn = nth(a.nargs);
		if (fn == LVALUE_SUPER_MEMBER)
			invokeSuper(a.nargs);
		else if (fn == LVALUE_MEMBER)
			invokeMethod(a.nargs);
		else if (fn instanceof DirectCall)
			invokeDirect(((DirectCall) fn).name, a.nargs);
		else
			invokeFunction(a.nargs);
		pop(a.nargs + 1);
		push(RVALUE_RETURN);
		return null;
	}

	private void processConstArgs(Args args) {
		if (args.constArgs == null)
			return;
		if (args.constArgs.size() < 10) {
			for (Map.Entry<Object, Object> e : args.constArgs.mapEntrySet()) {
				argumentName(e.getKey());
				constant(e.getValue());
				args.nargs += 3;
			}
		} else { // more than 10
			specialArg("EACH");
			constant(args.constArgs);
			args.nargs += 2;
		}
	}

	private static final String[] args = new String[99];
	static {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; ++i) {
			args[i] = sb.toString();
			sb.append("Ljava/lang/Object;");
		}
	}
	private void invokeSuper(int nargs) {
		c.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/language/SuCallable",
				"superInvokeN", "(Ljava/lang/Object;Ljava/lang/String;"
						+ args[nargs] + ")Ljava/lang/Object;");
	}
	private void invokeFunction(int nargs) {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "callN",
				"(Ljava/lang/Object;" + args[nargs] + ")Ljava/lang/Object;");
	}
	private void invokeMethod(int nargs) {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invokeN",
				"(Ljava/lang/Object;Ljava/lang/String;" + args[nargs]
						+ ")Ljava/lang/Object;");
	}

	// TODO handle direct calling other functions
	private void invokeDirect(String name, int nargs) {
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/ArgArray",
				"buildN",
				"(" + args[nargs] + ")[Ljava/lang/Object;");
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/builtin/ObjectClass",
				"create",
				"([Ljava/lang/Object;)Ljava/lang/Object;");

	}

	@Override
	public Object argumentList(Object args, Object keyword, Object value) {
		force();
		addNullCheck(top());
		Args a = (args == null ? new Args() : (Args) args);
		a.nargs += keyword == null ? 1 : 3;
		return a;
	}

	@Override
	public void argumentName(Object name) {
		specialArg("NAMED");
		constant(name);
		push(pop().forceValue());
	}

	@Override
	public void atArgument(String n) {
		assert "0".equals(n) || "1".equals(n);
		specialArg(n.charAt(0) == '1' ? "EACH1" : "EACH");
	}

	@Override
	public Object atArgument(String n, Object expr) {
		return new Args(2);
	}
	private void specialArg(String which) {
		force();
		c.mv.visitFieldInsn(GETSTATIC, "suneido/language/Args$Special",
				which,
				"Lsuneido/language/Args$Special;");
		push(RVALUE);
	}
	@Override
	public Object argumentListConstant(Object args, Object keyword, Object value) {
		Args a = (args == null ? new Args() : (Args) args);
		if (a.constArgs == null)
			a.constArgs = new SuContainer();
		a.constArgs.put(keyword, value);
		return a;
	}

	private static class Args {
		int nargs = 0;
		SuContainer constArgs = null;
		Args() {
		}
		Args(int nargs) {
			this.nargs = nargs;
		}
		@Override
		public String toString() {
			return "Args(nargs: " + nargs + ", constArgs: " + constArgs + ")";
		}
	}
	private static final Args noArgs = new Args();

	@Override
	public Object andStart() {
		return new Label();
	}

	@Override
	public Object and(Object olabel, Object expr1, Object expr2) {
		toBoolPop();
		Label label = (Label) olabel;
		c.mv.visitJumpInsn(IFFALSE, label);
		return null;
	}

	@Override
	public void andEnd(Object label) {
		force();
		Label l0 = new Label();
		getBoolean("TRUE");
		c.mv.visitJumpInsn(GOTO, l0);
		c.mv.visitLabel((Label) label);
		getBoolean("FALSE");
		c.mv.visitLabel(l0);
		pop(); // only one of the two results will be on the stack;
	}

	private void genBoolean(String which) {
		c.mv.visitFieldInsn(GETSTATIC,
				"java/lang/Boolean", which,
				"Ljava/lang/Boolean;");
	}

	private void getBoolean(String which) {
		genBoolean(which);
		push(RVALUE);
	}

	@Override
	public Object orStart() {
		return new Label();
	}

	@Override
	public Object or(Object olabel, Object expr1, Object expr2) {
		toBoolPop();
		Label label = (Label) olabel;
		c.mv.visitJumpInsn(IFTRUE, label);
		return null;
	}

	@Override
	public void orEnd(Object label) {
		force();
		Label l0 = new Label();
		getBoolean("FALSE");
		c.mv.visitJumpInsn(GOTO, l0);
		c.mv.visitLabel((Label) label);
		getBoolean("TRUE");
		c.mv.visitLabel(l0);
		pop(); // only one of the two results will be on the stack;
	}

	@Override
	public Object conditionalTrue(Object label, Object first) {
		force();
		return ifElse(label);
	}

	@Override
	public Object conditional(Object primaryExpression, Object first,
			Object second, Object label) {
		force();
		combineTop2();
		c.mv.visitLabel((Label) label);
		return null;
	}
	private void combineTop2() {
		Stack f = pop();
		Stack t = pop();
		if (t == f)
			push(t);
		else if (t == RVALUE)
			push(f);
		else if (f == RVALUE)
			push(t);
		else
			push(RVALUE_LOCAL);
	}

	@Override
	public Object in(Object expression, Object constant) {
		throw new SuException("'in' is only implemented for queries");
	}

	// COULD bypass invoke (like call does)

	@Override
	public void newCall() {
		force();
		c.mv.visitLdcInsn("<new>");
		push(RVALUE);
	}

	@Override
	public Object newExpression(Object term, Object args) {
		Args a = args == null ? noArgs : (Args) args;
		processConstArgs(a);
		force();
		invokeMethod(a.nargs);
		pop(a.nargs + 2);
		push(RVALUE);
		return null;
	}

	/** pop any value left on the stack
	 */
	@Override
	public void afterStatement(Object list) {
		if (! stackEmpty())
			pop().forceVoid();
		assert stackEmpty();
	}

	// statements

	@Override
	public Object statementList(Object list, Object next) {
		return next;
	}

	@Override
	public void addSuperInit() {
		force();
		if (!c.name.endsWith(METHOD_SEPARATOR + "New"))
			return;
		c.mv.visitVarInsn(ALOAD, THIS);
		c.mv.visitVarInsn(ALOAD, SELF);
		c.mv.visitLdcInsn("New");
		invokeSuper(0);
		c.mv.visitInsn(POP);
		c.superInitCalled = true;
		}

	@Override
	public Object expressionStatement(Object expression) {
		return expression;
	}

	@Override
	public Object ifExpr(Object expr) {
		toBoolPop();
		Label label = new Label();
		c.mv.visitJumpInsn(IFFALSE, label);
		return label;
	}

	private void toBoolPop() {
		force();
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "toBool",
				"(Ljava/lang/Object;)I");
		pop();
	}

	@Override
	public void ifThen(Object label, Object t) {
		afterStatement(t);
	}

	@Override
	public Object ifElse(Object pastThen) {
		force();
		Label pastElse = new Label();
		c.mv.visitJumpInsn(GOTO, pastElse);
		c.mv.visitLabel((Label) pastThen);
		return pastElse;
	}

	@Override
	public Object ifStatement(Object expr, Object t, Object e, Object afterIf) {
		afterStatement(e);
		c.mv.visitLabel((Label) afterIf);
		return null;
	}

	@Override
	public Object loop() {
		force();
		Loop loop = new Loop();
		c.mv.visitLabel(loop.continueLabel);
		return loop;
	}

	@Override
	public void whileExpr(Object expr, Object loop) {
		toBoolPop();
		gotoBreak(IFFALSE, loop);
	}

	@Override
	public Object whileStatement(Object expr, Object statement, Object loop) {
		endLoop(statement, loop);
		return null;
	}

	@Override
	public void blockParams() {
		if (c.nparams == 0) {
			c.locals.add("it");
			c.auto_it_param = true;
		}
	}

	@Override
	public Object blockEnd(Object params, Object statements) {
		if (c.auto_it_param && c.it_param_used)
			c.nparams = 1;

		finishMethod(statements);

		c.fspec = new BlockSpec(c.name, blockLocals(),
				c.nparams, c.atParam, c.iparams);
		hideBlockParams();

		Object blockDef = finishClass();
		int iBlockDef = addConstant(blockDef);

		force();

		// new SuBlock(block, self, locals)
		c.mv.visitTypeInsn(NEW, "suneido/language/SuBlock");
		c.mv.visitInsn(DUP);
		c.mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(c.mv, iBlockDef);
		c.mv.visitInsn(AALOAD);				// block
		c.mv.visitVarInsn(ALOAD, SELF); 	// self
		c.mv.visitVarInsn(ALOAD, ARGS); 	// locals
		c.mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuBlock",
				"<init>",
				"(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)V");

		if (!c.isBlock() && c.blockReturnCatcher == null)
			c.blockReturnCatcher = tryCatch("suneido/language/BlockReturnException");

		push(RVALUE);
		return null;
	}

	private String[] blockLocals() {
		String[] locals = new String[c.locals.size() - c.iparams];
		for (int i = 0; i < locals.length; ++i)
			locals[i] = c.locals.get(i + c.iparams);
		return locals;
	}

	private void hideBlockParams() {
		for (int i = c.iparams; i < c.iparams + c.nparams; ++i)
			c.locals.set(i, "_" + c.locals.get(i));
	}

	@Override
	public Object breakStatement(Object loop) {
		if (loop == Block)
			blockThrow("BREAK_EXCEPTION");
		else
			gotoBreak(GOTO, loop);
		return null;
	}

	@Override
	public Object continueStatement(Object loop) {
		if (loop == Block)
			blockThrow("CONTINUE_EXCEPTION");
		else
			gotoContinue(GOTO, loop);
		return null;
	}

	private void blockThrow(String which) {
		c.mv.visitFieldInsn(GETSTATIC, "suneido/language/Ops",
				which, "Lsuneido/SuException;");
		c.mv.visitInsn(ATHROW);
	}

	@Override
	public Object dowhileLoop() {
		force();
		Loop loop = new Loop();
		c.mv.visitLabel(loop.doLabel);
		return loop;
	}
	@Override
	public void dowhileContinue(Object loop) {
		force();
		c.mv.visitLabel(((Loop) loop).continueLabel);
	}
	@Override
	public Object dowhileStatement(Object body, Object expr, Object loop) {
		toBoolPop();
		c.mv.visitJumpInsn(IFTRUE, ((Loop) loop).doLabel);
		setBreak(loop);
		return null;
	}

	private void gotoContinue(int op, Object loop) {
		c.mv.visitJumpInsn(op, ((Loop) loop).continueLabel);
	}
	private void gotoBreak(int op, Object loop) {
		c.mv.visitJumpInsn(op, ((Loop) loop).breakLabel);
	}
	private void setBreak(Object loop) {
		c.mv.visitLabel(((Loop) loop).breakLabel);
	}

	@Override
	public Object forStart() {
		Label label = new Label();
		c.mv.visitJumpInsn(GOTO, label);
		return label;
	}

	@Override
	public void forIncrement(Object label) {
		if (label != null)
			c.mv.visitLabel((Label) label);
	}

	@Override
	public void forCondition(Object cond, Object loop) {
		toBoolPop();
		gotoBreak(IFFALSE, loop);
	}

	@Override
	public Object forClassicStatement(Object expr1, Object expr2, Object expr3,
			Object statement, Object loop) {
		endLoop(statement, loop);
		return null;
	}

	@Override
	public Object expressionList(Object list, Object expression) {
		afterStatement(expression);
		return null;
	}

	@Override
	public Object forInExpression(String var, Object expr) {
		force();
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				"iterator",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		c.mv.visitVarInsn(ASTORE, c.forInTmp);
		pop();

		Object loop = loop();

		c.mv.visitVarInsn(ALOAD, c.forInTmp);
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "hasNext",
				"(Ljava/lang/Object;)Z");
		gotoBreak(IFFALSE, loop);

		c.mv.visitVarInsn(ALOAD, c.forInTmp);
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "next",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		saveTopInVar(var);

		++c.forInTmp;
		return loop;
	}

	private void saveTopInVar(String var) {
		c.mv.visitVarInsn(ALOAD, ARGS);
		c.mv.visitInsn(SWAP);
		iconst(c.mv, addLocal(var));
		c.mv.visitInsn(SWAP);
		c.mv.visitInsn(AASTORE);
	}

	@Override
	public Object forInStatement(String var, Object expr, Object statement,
			Object loop) {
		--c.forInTmp;
		endLoop(statement, loop);
		return null;
	}

	@Override
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
		final int tmp;
		SwitchLabels(int tmp) {
			this.tmp = tmp;
		}
		Label end;
		Label body;
		Label next;
	}

	@Override
	public Object startSwitch() {
		force();
		c.mv.visitVarInsn(ASTORE, c.forInTmp);
		pop();
		return new SwitchLabels(c.forInTmp++);
	}

	@Override
	public void startCase(Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.next != null)
			c.mv.visitLabel(slabels.next);
		slabels.next = null;
		slabels.body = null;
	}

	@Override
	public Object caseValues(Object values, Object expr, Object labels,
			boolean more) {
		force();
		SwitchLabels slabels = (SwitchLabels) labels;
		c.mv.visitVarInsn(ALOAD, ((SwitchLabels) labels).tmp);
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "is_",
				"(Ljava/lang/Object;Ljava/lang/Object;)Z");
		pop();
		if (more) {
			if (slabels.body == null)
				slabels.body = new Label();
			c.mv.visitJumpInsn(IFTRUE, slabels.body);
		} else {
			if (slabels.next == null)
				slabels.next = new Label();
			c.mv.visitJumpInsn(IFFALSE, slabels.next);
		}
		return null;
	}

	@Override
	public void startCaseBody(Object labels) {
		force();
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.body != null)
			c.mv.visitLabel(slabels.body);
	}

	@Override
	public Object switchCases(Object cases, Object values, Object statements,
			Object labels, boolean moreCases) {
		afterStatement(statements);
		if (moreCases) {
			SwitchLabels slabels = (SwitchLabels) labels;
			if (slabels.end == null)
				slabels.end = new Label();
			c.mv.visitJumpInsn(GOTO, ((SwitchLabels) labels).end);
		}
		return null;
	}

	@Override
	public Object switchStatement(Object expr, Object cases, Object labels) {
		SwitchLabels slabels = (SwitchLabels) labels;
		if (slabels.next != null)
			c.mv.visitLabel(((SwitchLabels) labels).next);
		if (slabels.end != null)
			c.mv.visitLabel(((SwitchLabels) labels).end);
		return null;
	}

	@Override
	public Object throwStatement(Object expr) {
		force();
		c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "thrower",
				"(Ljava/lang/Object;)V");
		pop();
		return null;
	}

	static class TryCatch {
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		Label label3 = new Label();
	}

	@Override
	public Object startTry() {
		force();
		return tryCatch("suneido/SuException");
	}

	TryCatch tryCatch(String exception) {
		TryCatch tc = new TryCatch();
		c.mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2, exception);
		c.mv.visitLabel(tc.label0);
		return tc;
	}

	@Override
	public void startCatch(String var, String pattern, Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		c.mv.visitLabel(tc.label1);
		c.mv.visitJumpInsn(GOTO, tc.label3);
		c.mv.visitLabel(tc.label2);

		// exception is on stack
		if (pattern != null) {
			c.mv.visitLdcInsn(pattern);
			c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
					"catchMatch",
					"(Lsuneido/SuException;Ljava/lang/String;)Ljava/lang/String;");
		} else if (var != null)
			c.mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuException",
					"toString", "()Ljava/lang/String;");
		if (var == null)
			c.mv.visitInsn(POP);
		else
			saveTopInVar(var);
	}

	@Override
	public Object catcher(String var, String pattern, Object statement) {
		return null;
	}

	@Override
	public Object tryStatement(Object tryStatement, Object catcher,
			Object trycatch) {
		force();
		TryCatch tc = (TryCatch) trycatch;
		c.mv.visitLabel(tc.label3);
		return null;
	}

	@Override
	public Object rvalue(Object expr) {
		force();
		return expr;
	}

	// Stack ==================================================================

	void push(Stack x) {
		c.stack.add(x);
	}

	Stack pop() {
		return c.stack.remove(c.stack.size() - 1);
	}
	void pop(int n) {
		for (int i = 0; i < n; ++i)
			pop();
	}

	Stack top() {
		return nth(0);
	}

	Stack nth(int n) {
		return c.stack.get(c.stack.size() - (n + 1));
	}

	/** called before emitting any byte code
	 *  to ensure that deferred code is emitted first
	 */
	void force() {
		List<Stack> stack = c.stack;
		for (int i = 0; i < stack.size(); ++i) {
			Stack x = stack.get(i).forceValue();
			stack.set(i, x);
		}
	}

	boolean stackEmpty() {
		return c.stack.isEmpty();
	}

	/** same as force except top is forced to lvalue */
	void forceLvalue() {
		List<Stack> stack = c.stack;
		int i = 0;
		for (; i < stack.size() - 1; ++i) {
			Stack x = stack.get(i).forceValue();
			stack.set(i, x);
		}
		Stack x = stack.get(i).lvalue();
		stack.set(i, x);
	}

	private abstract class Stack {
		abstract Stack forceValue();
		void forceVoid() {
		}
		/** generate code to push lvalue onto stack */
		Stack lvalue() {
			throw new SuException("invalid lvalue");
		}
		/** generate code to load, given lvalue on stack */
		Stack load() {
			throw new SuException("invalid lvalue");
		}
		/** generate code to store, given lvalue and rvalue on stack */
		void store() {
			throw new SuException("invalid lvalue");
		}
		@Override
		public String toString() {
			return this.getClass().getSimpleName();
		}
	}

	/** means that byte code has been generated to push value */
	private final class Rvalue extends Stack {
		String which;
		Rvalue(String which) {
			this.which = which;
		}
		@Override
		Stack forceValue() {
			return this;
		}
		@Override
		void forceVoid() {
			c.mv.visitInsn(POP);
		}
		@Override
		public String toString() {
			return which;
		}
	}
	private final Stack RVALUE = new Rvalue("Rvalue");
	private final Stack RVALUE_SELF = new Rvalue("RvalueSelf");
	private final Stack RVALUE_LOCAL = new Rvalue("RvalueLocal");
	private final Stack RVALUE_RETURN = new Rvalue("RvalueReturn");

	/** means that byte code has been generated to push lvalue for local
	 *  i.e. args and i
	 */
	private final class LvalueLocal extends Stack {
		@Override
		Stack forceValue() {
			return this;
		}
		@Override
		void forceVoid() {
			throw SuException.unreachable();
		}
		@Override
		Stack load() {
			c.mv.visitInsn(AALOAD);
			return RVALUE_LOCAL;
		}
		@Override
		void store() {
			c.mv.visitInsn(AASTORE);
		}
	}
	private final Stack LVALUE_LOCAL = new LvalueLocal();

	/** means that byte code has been generated to push lvalue for member/subscript
	 *  i.e. object and member/subscript
	 */
	private final class LvalueMember extends Stack {
		@Override
		Stack forceValue() {
			return this;
		}
		@Override
		void forceVoid() {
			throw SuException.unreachable();
		}
		@Override
		Stack load() {
			getMember();
			return RVALUE;
		}
		@Override
		void store() {
			putMember();
		}
	}
	private final Stack LVALUE_MEMBER = new LvalueMember();
	private final Stack LVALUE_SUPER_MEMBER = new LvalueMember();

	/** defer assignment to allow dup before assign if value required
	 *  means that stack contains lvalue
	 */
	private final class DeferAssignment extends Stack {
		final Stack lvalue;
		final Token op;
		final Stack rvalue;
		DeferAssignment(Stack lvalue, Token op, Stack rvalue) {
			this.lvalue = lvalue;
			this.op = op;
			this.rvalue = rvalue;
		}
		@Override
		void forceVoid() {
			if (op == Token.EQ)
				addNullCheck(rvalue);
			lvalue.store();
		}
		@Override
		Stack forceValue() {
			Stack result = rvalue;
			if (op == Token.EQ && lvalue == LVALUE_MEMBER)
				result = addNullCheck(rvalue);
			c.mv.visitInsn(DUP_X2);
			lvalue.store();
			return result;
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(lvalue)
					.addValue(op)
					.addValue(rvalue)
					.toString();
		}
	}

	// defer identifier to handle assignments
	// and to allow optimizing calls to built-in globals
	private final class DeferIdentifier extends Stack {
		final String name;
		DeferIdentifier(String name) {
			this.name = name;
			assert ! name.equals("super");
		}
		@Override
		Stack forceValue() {
			if (isGlobal()) {
				String name2 = name;
				if (name.startsWith("_") && Character.isUpperCase(name.charAt(1)))
					name2 = Globals.overload(name);
				c.mv.visitLdcInsn(name2);
				c.mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals",
						"get",
						"(Ljava/lang/String;)Ljava/lang/Object;");
				return RVALUE;
			} else if (name.equals("this")) {
				c.mv.visitVarInsn(ALOAD, SELF);
				return RVALUE;
			} else {
				int i = localRef(name);
				c.mv.visitInsn(AALOAD);
				return i < c.nparams ? RVALUE : RVALUE_LOCAL;
			}
		}
		@Override
		void forceVoid() {
			addNullCheck(forceValue());
			c.mv.visitInsn(POP);
		}
		@Override
		Stack lvalue() {
			if (isGlobal())
				throw new SuException("globals are read-only");
			else if (name.equals("this") || name.equals("super"))
				throw new SuException("this and super are read-only");
			localRef(name);
			return LVALUE_LOCAL;
		}
		private boolean isGlobal() {
			int i = name.startsWith("_") ? 1 : 0;
			return Character.isUpperCase(name.charAt(i));
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(name)
					.toString();
		}
	}

	// used for .member (to privatize)
	private final class DeferSelf extends Stack {
		@Override
		public Stack forceValue() {
			c.mv.visitVarInsn(ALOAD, SELF);
			return RVALUE_SELF;
		}
		@Override
		public String toString() {
			return "<self>";
		}
	}
	private final Stack DEFER_SELF = new DeferSelf();

	// defer constants to allow optimizing constant expressions
	private final class DeferConstant extends Stack {
		final Object value;
		DeferConstant(Object value) {
			this.value = value;
		}
		@Override
		Stack forceValue() {
			if (value == Boolean.TRUE || value == Boolean.FALSE)
				genBoolean(value == Boolean.TRUE ? "TRUE" : "FALSE");
			else if (value instanceof String)
				c.mv.visitLdcInsn(value);
			else if (value instanceof Integer) {
				iconst(c.mv, (Integer) value);
				c.mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer",
						"valueOf",
						"(I)Ljava/lang/Integer;");
			} else {
				int i = constantFor(value);
				c.mv.visitVarInsn(ALOAD, CONSTANTS);
				iconst(c.mv, i);
				c.mv.visitInsn(AALOAD);
			}
			return RVALUE;
		}
		@Override
		void forceVoid() {
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(value)
					.toString();
		}
	}

	// defer member to allow optimizing calling private methods
	private final class DeferMember extends Stack {
		final Stack object;
		final String name;
		DeferMember(Stack object, String name) {
			this.object = object;
			this.name = name;
		}
		@Override
		Stack forceValue() {
			lvalue();
			getMember();
			return RVALUE;
		}
		@Override
		void forceVoid() {
			forceValue();
			c.mv.visitInsn(POP);
		}
		@Override
		Stack lvalue() {
			c.mv.visitLdcInsn(object == RVALUE_SELF ? privatize(name) : name);
			return LVALUE_MEMBER;
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(object)
					.addValue(name)
					.toString();
		}
	}
	private final class DeferSubscript extends Stack {
		@Override
		Stack forceValue() {
			getMember();
			return RVALUE;
		}
		@Override
		void forceVoid() {
			forceValue();
			c.mv.visitInsn(POP);
		}
		@Override
		Stack lvalue() {
			return LVALUE_MEMBER;
		}
	}

	/** there is actually nothing on the stack for this */
	private final class DirectCall extends Stack {
		String name;
		DirectCall(String name) {
			this.name = name;
		}
		@Override
		Stack forceValue() {
			return this;
		}
	}

}
