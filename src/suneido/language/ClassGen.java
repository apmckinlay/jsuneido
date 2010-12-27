package suneido.language;

import static org.objectweb.asm.Opcodes.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.SuException;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Used by {@link AstCompile} to generate Java classes.
 * Wraps use of ASM.
 */
public class ClassGen {
	private static final int THIS = 0;
	private int SELF = -1;
	private int ARGS = -2;
	private int CONSTANTS = -3;
	private static final Object[] arrayOfObject = new Object[0];
	private static final String[] arrayOfString = new String[0];
	private final String name;
	private final String base;
	private final ClassWriter cw;
	private final ClassVisitor cv;
	private final MethodVisitor mv;
	private final Label startLabel = new Label();
	private final int firstParam;
	private int nParams;
	private int nDefaults = 0;
	private boolean atParam = false;
	private final int iBlockParams; // where block params start in locals
	private final List<Object> constants = new ArrayList<Object>();
	final List<String> locals;
	final BiMap<String,Integer> javaLocals = HashBiMap.create();
	private int nextJavaLocal;
	private Object blockReturnCatcher = null;
	public final boolean useArgsArray;
	public final boolean isBlock;
	public final boolean trueBlock;
//	private List<String> javaLocalsNames = new ArrayList<String>();
//	{ Collections.addAll(javaLocalsNames, "java$this", "this", "$args", "$constants"); }

	ClassGen(String base, String name, String method, List<String> locals,
			boolean useArgsArray, boolean isBlock, int nParams, PrintWriter pw) {
		if (! useArgsArray)
			base += nParams;
		this.base = "suneido/language/" + base;
		this.name = name;
		this.locals = locals == null ? new ArrayList<String>() : locals;
		this.useArgsArray = useArgsArray;
		this.isBlock = isBlock;
		trueBlock = isBlock && base.equals("SuCallable");
		iBlockParams = this.locals.size();
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cv = classVisitor(pw);
		genInit();
		javaLocals.put("this", nextJavaLocal++);
		if (useArgsArray) {
			if (method.equals("call")) {
				mv = methodVisitor(ACC_PUBLIC, method,
						"([Ljava/lang/Object;)Ljava/lang/Object;");
				javaLocals.put("_args_", ARGS = nextJavaLocal++);
			} else {
				assert method.equals("eval");
				mv = methodVisitor(ACC_PUBLIC, method,
						"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
				javaLocals.put("_self_", SELF = nextJavaLocal++);
				javaLocals.put("_args_", ARGS = nextJavaLocal++);
			}
		} else {
			if (method.equals("call")) {
				mv = methodVisitor(ACC_PUBLIC, "call" + nParams,
						"(" + directArgs[nParams] + ")Ljava/lang/Object;");
			} else {
				assert method.equals("eval");
				mv = methodVisitor(ACC_PUBLIC, "eval" + nParams,
						"(Ljava/lang/Object;" + directArgs[nParams] + ")Ljava/lang/Object;");
				javaLocals.put("_self_", SELF = nextJavaLocal++);
			}
		}
		firstParam = nextJavaLocal;
		mv.visitCode();
		mv.visitLabel(startLabel);
		if (! trueBlock && useArgsArray)
			massage();
	}

	private ClassVisitor classVisitor(PrintWriter pw) {
		ClassVisitor cv = cw;
		if (pw != null)
			cv = new TraceClassVisitor(cw, pw);
		cv = new CheckClassAdapter(cv, false);
		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "suneido/language/" + name,
				null, base, null);
		cv.visitSource("", null);
		return cv;
	}

	private MethodVisitor methodVisitor(int access, String name, String desc) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
		mv = new TryCatchBlockSorter(mv, access, name, desc, null, null);
		return mv;
	}

	private void massage() {
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitFieldInsn(GETFIELD, "suneido/language/" + name,
				"params", "Lsuneido/language/FunctionSpec;");
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Args",
				"massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, ARGS);
	}

	public void loadConstants() {
		javaLocals.put("_constants_", CONSTANTS = nextJavaLocal++);
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitFieldInsn(GETFIELD, "suneido/language/" + name,
				"constants", "[Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, CONSTANTS);
	}

	public void param(String name, Object defaultValue) {
		atParam = name.startsWith("@");
		if (atParam)
			name = name.substring(1, name.length());
		if (useArgsArray)
			locals.add(name);
		else
			javaLocal(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert i == nDefaults;
			++nDefaults;
		}
		++nParams;
	}

	public void constant(Object value) {
		if (value == Boolean.TRUE || value == Boolean.FALSE)
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean",
					value == Boolean.TRUE ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
		else if (value instanceof String)
			mv.visitLdcInsn(value);
		else if (value instanceof Integer) {
			iconst((Integer) value);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
					"(I)Ljava/lang/Integer;");
		} else {
			int i = constantFor(value);
			mv.visitVarInsn(ALOAD, CONSTANTS);
			iconst(i);
			mv.visitInsn(AALOAD);
		}
	}

	private int constantFor(Object value) {
		int i = constants.indexOf(value);
		return i == -1 || value.getClass() != constants.get(i).getClass()
				? addConstant(value) : i;
	}

	public int addConstant(Object value) {
		constants.add(value);
		return constants.size() - 1;
	}

	public int iconst(int i) {
		if (-1 <= i && i <= 5)
			mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			mv.visitIntInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, i);
		else
			mv.visitLdcInsn(i);
		return i;
	}

	public void bool(boolean x, boolean intBool) {
		if (intBool)
			mv.visitInsn(ICONST_0 + (x ? 1 : 0));
		else
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean",
					x  ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
	}

	public void pop() {
		mv.visitInsn(POP);
	}

	public void returnValue() {
		if (isBlock)
			blockReturn();
		else
			areturn();
	}

	public void areturn() {
		mv.visitInsn(ARETURN);
	}

	// note: can't use pre-constructed exception
	// because we have to include the return value
	// COULD use pre-constructed if return value is null (and maybe true/false)
	private void blockReturn() {
		// stack: value
		mv.visitTypeInsn(NEW, "suneido/language/BlockReturnException");
		// stack: exception, value
		mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		mv.visitInsn(SWAP);
		// stack: value, exception, exception
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitMethodInsn(INVOKESPECIAL,
				"suneido/language/BlockReturnException",
				"<init>",
				"(Ljava/lang/Object;Ljava/lang/Object;)V");
		// stack: exception
		mv.visitInsn(ATHROW);
	}

	public void aconst_null() {
		mv.visitInsn(ACONST_NULL);
	}

	public void unaryOp(String method, String type) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", method,
				"(Ljava/lang/Object;)Ljava/lang/" + type + ";");
	}
	public void unaryOp(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;)" + op.resultType.type);
	}
	public void binaryOp(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type);
	}

	public void localLoad(String name) {
		if (name.equals("this"))
			mv.visitVarInsn(ALOAD, SELF);
		else {
			int ref = localRef(name);
			localRefLoad(ref);
		}
	}

	public final static int ARGS_REF = -1;
	public final static int MEMBER_REF = -2;
	// >= 0 means java local index

	public int localRef(String name) {
		if (! useArgsArray)
			return javaLocal(name);
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, ARGS);
		iconst(local(name));
		return ARGS_REF;
	}

	public void localRefLoad(int ref) {
		if (ref == ARGS_REF)
			mv.visitInsn(AALOAD);
		else
			mv.visitVarInsn(ALOAD, ref);
	}

	public void localRefStore(int ref) {
		if (ref == ARGS_REF)
			mv.visitInsn(AASTORE);
		else
			mv.visitVarInsn(ASTORE, ref);
	}

	public int local(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = locals.lastIndexOf(name);
		if (i == -1) {
			i = locals.size();
			locals.add(name);
		}
		return i;
	}

	public int javaLocal(String name) {
		// don't javify here because FunctionSpec needs original names
		Integer i = javaLocals.get(name);
		if (i != null)
			return i;
		javaLocals.put(name, nextJavaLocal);
		return nextJavaLocal++;
	}

	public static String javify(String name) {
		return name.replace("?", "_Q_").replace("!", "_X_");
	}

	public void globalLoad(String name) {
		if (name.startsWith("_") && Character.isUpperCase(name.charAt(1)))
			name = Globals.overload(name);
		mv.visitLdcInsn(name);
		globalLoad();
	}

	public void globalLoad() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals",
			"get", "(Ljava/lang/String;)Ljava/lang/Object;");
	}

	public void memberLoad() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "get",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void memberStore() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "put",
				"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
	}

	public void dup() {
		mv.visitInsn(DUP);
	}

	public void dupLvalue(int ref) {
		if (ref < 0)
			mv.visitInsn(DUP2);
		// else lvalue is java local
		// 		nothing to dup
	}

	public void dupUnderLvalue(int ref) {
		if (ref >= 0) // lvalue is java local
			mv.visitInsn(DUP);
		else
			mv.visitInsn(DUP_X2);
	}

	public boolean neverNull(String name) {
		return name.equals("this") || local(name) < nParams;
	}

	public void addNullCheck(String error) {
		Label label = new Label();
		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNONNULL, label);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				"throw" + error, "()V");
		mv.visitLabel(label);
	}

	public void invokeFunction() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "call",
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void invokeFunction(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "call" + nargs,
				"(" + directArgs[nargs + 1] + ")Ljava/lang/Object;");
	}

	public void invokeGlobal() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals", "invoke",
				"(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
	}
	public void invokeGlobal(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Globals", "invoke" + nargs,
				"(Ljava/lang/String;" + directArgs[nargs] + ")Ljava/lang/Object;");
	}

	public void invokeMethod() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invoke",
			"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void invokeMethod(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invoke" + nargs,
			"(Ljava/lang/Object;Ljava/lang/String;" + directArgs[nargs] + ")Ljava/lang/Object;");
	}

	private static final int MAX_DIRECT_ARGS = 11;
	private static final String[] directArgs = new String[MAX_DIRECT_ARGS];
	static {
		for (int n = 0; n < MAX_DIRECT_ARGS; ++n)
			directArgs[n] = Strings.repeat("Ljava/lang/Object;", n);
	}

	public void anewarray(int size) {
		iconst(size);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
	}

	public void aastore() {
		mv.visitInsn(AASTORE);
	}

	public void invokeDirect(String name) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/builtin/" + name + "Class",
				"create", "([Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void superCallTarget(String method) {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn(method);
	}

	public void invokeSuper() {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/language/SuCallable", "superInvoke",
				"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void superInit() {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn("New");
		anewarray(0);
		invokeSuper();
		mv.visitInsn(POP);
	}

	public void toMethodString() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				"toMethodString", "(Ljava/lang/Object;)Ljava/lang/String;");
	}

	public void specialArg(String which) {
		mv.visitFieldInsn(GETSTATIC, "suneido/language/Args$Special",
				which, "Lsuneido/language/Args$Special;");
	}

	public void thrower() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "exception",
				"(Ljava/lang/Object;)Lsuneido/SuException;");
		mv.visitInsn(ATHROW);
	}

	public void blockThrow(String which) {
		mv.visitFieldInsn(GETSTATIC, "suneido/language/Ops",
				which, "Lsuneido/SuException;");
		mv.visitInsn(ATHROW);
	}

	private static class TryCatch {
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		Label label3 = new Label();
	}

	public Object tryCatch(String exception) {
		TryCatch tc = new TryCatch();
		mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2, exception);
		mv.visitLabel(tc.label0);
		return tc;
	}

	public void startCatch(String var, String pattern, Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		mv.visitLabel(tc.label1);
		mv.visitJumpInsn(GOTO, tc.label3);
		mv.visitLabel(tc.label2);

		// exception is on stack
		if (pattern != null) {
			mv.visitLdcInsn(pattern);
			mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
					"catchMatch",
					"(Lsuneido/SuException;Ljava/lang/String;)Ljava/lang/String;");
		} else if (var != null)
			mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/SuException",
					"toString", "()Ljava/lang/String;");
		if (var == null)
			mv.visitInsn(POP);
		else
			saveTopInVar(var);
	}

	public void endCatch(Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		mv.visitLabel(tc.label3);
	}

	public Object ifFalse() {
		Label label = new Label();
		mv.visitJumpInsn(IFEQ, label);
		return label;
	}

	public void ifFalse(Object label) {
		mv.visitJumpInsn(IFEQ, (Label) label);
	}

	public void ifTrue(Object label) {
		mv.visitJumpInsn(IFNE, (Label) label);
	}

	public Object label() {
		return new Label();
	}

	public Object placeLabel() {
		Label label = new Label();
		mv.visitLabel(label);
		return label;
	}

	public void placeLabel(Object label) {
		mv.visitLabel((Label) label);
	}

	public Object jump() {
		Label label = new Label();
		mv.visitJumpInsn(GOTO, label);
		return label;
	}

	public void jump(Object label) {
		mv.visitJumpInsn(GOTO, (Label) label);
	}

	public void toIntBool() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "toIntBool",
				"(Ljava/lang/Object;)I");
	}

	// for in

	public int iter() {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops",
				"iterator",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		return storeTemp();
	}

	public int storeTemp() {
		mv.visitVarInsn(ASTORE, nextJavaLocal);
		return nextJavaLocal++;
	}

	public void loadTemp(int temp) {
		mv.visitVarInsn(ALOAD, temp);
	}

	/** leaves boolean result on the stack */
	public void hasNext(int temp) {
		mv.visitVarInsn(ALOAD, temp);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "hasNext",
				"(Ljava/lang/Object;)Z");
	}
	public void next(String var, int temp) {
		loadTemp(temp);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "next",
				"(Ljava/lang/Object;)Ljava/lang/Object;");
		saveTopInVar(var);
	}

	private void saveTopInVar(String var) {
		if (! useArgsArray) {
			localRefStore(localRef(var));
			return ;
		}
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitInsn(SWAP);
		iconst(local(var));
		mv.visitInsn(SWAP);
		mv.visitInsn(AASTORE);
	}

	public void block(int iBlockDef, String nParams) {
		// new SuBlock(block, self, locals)
		final String className = "suneido/language/SuBlock" + nParams;
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(iBlockDef);
		mv.visitInsn(AALOAD);			// block
		if (SELF >= 0)
			mv.visitVarInsn(ALOAD, SELF); 	// self
		else
			mv.visitInsn(ACONST_NULL);
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, ARGS); 	// locals
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>",
				"(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)V");
		addBlockReturnCatcher();
	}

	public void addBlockReturnCatcher() {
		if (!isBlock && blockReturnCatcher  == null)
			blockReturnCatcher = tryCatch("suneido/language/BlockReturnException");
	}

	private void finishBlockReturnCatcher() {
		TryCatch tc = (TryCatch) blockReturnCatcher;
		mv.visitLabel(tc.label1);
		mv.visitLabel(tc.label2);

		mv.visitVarInsn(ALOAD, THIS);
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/language/SuCallable",
				"blockReturnHandler",
				"(Lsuneido/language/BlockReturnException;)Ljava/lang/Object;");
		mv.visitInsn(ARETURN);
	}

	public SuCallable end() {
		if (blockReturnCatcher != null)
			finishBlockReturnCatcher();

		Label endLabel = new Label();
		mv.visitLabel(endLabel);
		BiMap<Integer, String> bm = javaLocals.inverse();
		for (int i = 0; i < nextJavaLocal; ++i)
			if (bm.containsKey(i))
				mv.visitLocalVariable(javify(bm.get(i)), "[Ljava/lang/Object;", null,
						startLabel, endLabel, i);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
		cv.visitEnd();

		Loader loader = new Loader();
		Class<?> sc = loader.defineClass("suneido.language." + name,
				cw.toByteArray());
		SuCallable callable;
		try {
			callable = (SuCallable) sc.newInstance();
		} catch (InstantiationException e) {
			throw new SuException("newInstance error: " + e);
		} catch (IllegalAccessException e) {
			throw new SuException("newInstance error: " + e);
		}

		Object[] constantsArray = (constants == null)
				? null : constants.toArray(arrayOfObject);

		callable.params = functionSpec(bm, constantsArray);
		callable.constants = constantsArray;
		callable.isBlock = isBlock;

		return callable;
	}

	private FunctionSpec functionSpec(BiMap<Integer, String> bm,
			Object[] constantsArray) {
		FunctionSpec fspec;
		if (trueBlock) {
			fspec = new BlockSpec(name, blockLocals(), nParams, atParam, iBlockParams);
			hideBlockParams();
		} else if (useArgsArray)
			fspec = new FunctionSpec(name, locals.toArray(arrayOfString),
					nParams, constantsArray, nDefaults, atParam);
		else {
			String[] params = new String[nParams];
			for (int i = 0; i < nParams; ++i)
				params[i] = bm.get(i + firstParam);
			fspec = new FunctionSpec(name, params,
					nParams, constantsArray, nDefaults, atParam);
		}
		return fspec;
	}

	static class Loader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	private String[] blockLocals() {
		String[] blockLocals = new String[locals.size() - iBlockParams];
		for (int i = 0; i < blockLocals.length; ++i)
			blockLocals[i] = locals.get(i + iBlockParams);
		return blockLocals;
	}

	private void hideBlockParams() {
		for (int i = iBlockParams; i < iBlockParams + nParams; ++i)
			locals.set(i, "_" + locals.get(i));
	}

	private void genInit() {
		MethodVisitor mv =
				cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V");
		mv.visitInsn(RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lsuneido/language/" + name + ";",
				null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

}
