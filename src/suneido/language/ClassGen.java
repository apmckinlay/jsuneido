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

public class ClassGen {
	private static final int THIS = 0;
	private static final int SELF = 1;
	private static final int ARGS = 2;
	private static final int CONSTANTS = 3;
	private static final Object[] arrayObject = new Object[0];
	private static final String[] arrayString = new String[0];
	private static final String[] args = new String[99];
	static {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; ++i) {
			args[i] = sb.toString();
			sb.append("Ljava/lang/Object;");
		}
	}
	private final String name;
	private final String base;
	private final ClassWriter cw;
	private final ClassVisitor cv;
	private final MethodVisitor mv;
	private final Label startLabel = new Label();
	private int nParams;
	private int nDefaults = 0;
	private boolean atParam = false;
	private boolean auto_it_param = false;
	private boolean it_param_used = false;
	private final int iBlockParams; // where block params start in locals
	private final List<Object> constants = new ArrayList<Object>();
	final List<String> locals;
	private int temp = CONSTANTS + 1;
	private Object blockReturnCatcher = null;

	ClassGen(String base, String name, String method, List<String> locals,
			PrintWriter pw) {
		this.base = "suneido/language/" + base;
		this.name = name;
		this.locals = locals == null ? new ArrayList<String>() : locals;
		iBlockParams = this.locals.size();
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cv = classVisitor(pw);
		genInit();
		mv = methodVisitor(ACC_PUBLIC, method,
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitCode();
		mv.visitLabel(startLabel);
		if (! isBlock())
			massage();
		loadConstants();
	}

	public boolean isBlock() {
		return base.contains("SuCallable");
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
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitFieldInsn(GETFIELD, "suneido/language/" + name,
				"params", "Lsuneido/language/FunctionSpec;");
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Args",
				"massage",
				"(Lsuneido/language/FunctionSpec;[Ljava/lang/Object;)[Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, ARGS);
	}

	private void loadConstants() {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitFieldInsn(GETFIELD, "suneido/language/" + name,
				"constants", "[Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, CONSTANTS);
	}

	public void param(String name, Object defaultValue) {
		atParam = name.startsWith("@");
		if (atParam)
			name = name.substring(1, name.length());
		locals.add(name);
		if (defaultValue != null) {
			int i = addConstant(defaultValue);
			assert i == nDefaults;
			++nDefaults;
		}
		++nParams;
	}

	public void itParam() {
		param("it", null);
		auto_it_param = true;
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

	private int iconst(int i) {
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

	public void pop() {
		mv.visitInsn(POP);
	}

	public void returnValue() {
		if (isBlock())
			blockReturn();
		else
			areturn();
	}

	public void areturn() {
		mv.visitInsn(ARETURN);
	}

	// note: can't use preconstructed exception
	// because we have to include the return value
	// TODO use pre-constructed if return value is null (and maybe true/false)
	private void blockReturn() {
		// stack: value
		mv.visitTypeInsn(NEW, "suneido/language/BlockReturnException");
		// stack: exception, value
		mv.visitInsn(DUP_X1);
		// stack: exception, value, exception
		mv.visitInsn(SWAP);
		// stack: value, exception, exception
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESPECIAL,
				"suneido/language/BlockReturnException",
				"<init>",
				"(Ljava/lang/Object;[Ljava/lang/Object;)V");
		// stack: exception
		mv.visitInsn(ATHROW);
	}

	public void aconst_null() {
		mv.visitInsn(ACONST_NULL);
	}

	public void unaryMethod(String method, String type) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", method,
				"(Ljava/lang/Object;)Ljava/lang/" + type + ";");
	}
	public void unaryMethod(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;)" + op.resultType.type);
	}
	public void binaryMethod(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type);
	}

	public void localLoad(String name) {
		if (name.equals("this"))
			mv.visitVarInsn(ALOAD, SELF);
		else {
			localRef(name);
			localLoad();
		}
	}

	public void localLoad() {
		mv.visitInsn(AALOAD);
	}

	public int localRef(String name) {
		mv.visitVarInsn(ALOAD, ARGS);
		return iconst(local(name));
	}

	public void localStore() {
		mv.visitInsn(AASTORE);
	}

	public int local(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = locals.lastIndexOf(name);
		if (i == -1) {
			i = locals.size();
			locals.add(name);
		} else if (name.equals("it"))
			it_param_used = true;
		return i;
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

	public void dup2() {
		mv.visitInsn(DUP2);
	}

	public void dup_x2() {
		mv.visitInsn(DUP_X2);
	}

	public boolean neverNull(String name) {
		return name.equals("this") || local(name) < nParams;
	}

	public void addNullCheck(String error) {
		Label label = new Label();
		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNONNULL, label);
		mv.visitLdcInsn(error);
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "thrower",
				"(Ljava/lang/Object;)V");
		mv.visitLabel(label);
	}

	public void invokeFunction(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "callN",
				"(Ljava/lang/Object;" + args[nargs] + ")Ljava/lang/Object;");
	}

	public void invokeMethod(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "invokeN",
				"(Ljava/lang/Object;Ljava/lang/String;" + args[nargs]
						+ ")Ljava/lang/Object;");
	}

	public void invokeDirect(String name, int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/ArgArray",
				"buildN",
				"(" + args[nargs] + ")[Ljava/lang/Object;");
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/builtin/ObjectClass",
				"create",
				"([Ljava/lang/Object;)Ljava/lang/Object;");
	}

	public void superCallTarget(String method) {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn(method);
	}

	public void invokeSuper(int nargs) {
		mv.visitMethodInsn(INVOKEVIRTUAL, "suneido/language/SuCallable",
				"superInvokeN", "(Ljava/lang/Object;Ljava/lang/String;"
						+ args[nargs] + ")Ljava/lang/Object;");
	}

	public void superInit() {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn("New");
		invokeSuper(0);
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
		mv.visitMethodInsn(INVOKESTATIC, "suneido/language/Ops", "thrower",
				"(Ljava/lang/Object;)V");
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
		mv.visitVarInsn(ASTORE, temp);
		return temp++;
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

	static void tmp(List<Object> list) {
		for (Object x : list)
			x.getClass();
	}

	private void saveTopInVar(String var) {
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitInsn(SWAP);
		iconst(local(var));
		mv.visitInsn(SWAP);
		mv.visitInsn(AASTORE);
	}

	public void block(int iBlockDef) {
		// new SuBlock(block, self, locals)
		mv.visitTypeInsn(NEW, "suneido/language/SuBlock");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, CONSTANTS);
		iconst(iBlockDef);
		mv.visitInsn(AALOAD);				// block
		mv.visitVarInsn(ALOAD, SELF); 	// self
		mv.visitVarInsn(ALOAD, ARGS); 	// locals
		mv.visitMethodInsn(INVOKESPECIAL, "suneido/language/SuBlock",
				"<init>",
				"(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)V");
		if (!isBlock() && blockReturnCatcher  == null)
			blockReturnCatcher = tryCatch("suneido/language/BlockReturnException");
	}

	private void finishBlockReturnCatcher() {
		TryCatch tc = (TryCatch) blockReturnCatcher;
		mv.visitLabel(tc.label1);
		mv.visitLabel(tc.label2);

		mv.visitInsn(DUP);
		mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"locals", "[Ljava/lang/Object;");
		mv.visitVarInsn(ALOAD, ARGS);
		Label label = new Label();
		mv.visitJumpInsn(IF_ACMPEQ, label);
		mv.visitInsn(ATHROW); // not ours so just re-throw
		mv.visitLabel(label);
		mv.visitFieldInsn(GETFIELD,
				"suneido/language/BlockReturnException",
				"returnValue", "Ljava/lang/Object;");
		mv.visitInsn(ARETURN);
	}

	public SuCallable end() {
		if (blockReturnCatcher != null)
			finishBlockReturnCatcher();

		Label endLabel = new Label();
		mv.visitLabel(endLabel);
		mv.visitLocalVariable("this", "Lsuneido/language/" + name + ";",
				null, startLabel, endLabel, THIS);
		mv.visitLocalVariable("self", "Ljava/lang/Object;", null,
				startLabel, endLabel, SELF);
		mv.visitLocalVariable("args", "[Ljava/lang/Object;", null,
				startLabel, endLabel, ARGS);
		mv.visitLocalVariable("constants", "[Ljava/lang/Object;", null,
				startLabel, endLabel, CONSTANTS);
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
				? null : constants.toArray(arrayObject);

		if (auto_it_param && ! it_param_used)
			nParams = 0;

		FunctionSpec fspec;
		if (isBlock()) {
			fspec = new BlockSpec(name, blockLocals(), nParams, atParam, iBlockParams);
			hideBlockParams();
		} else
			fspec = new FunctionSpec(name, locals.toArray(arrayString),
					nParams, constantsArray, nDefaults, atParam);

		callable.params = fspec;
		callable.constants = constantsArray;

		return callable;
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
