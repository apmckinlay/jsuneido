/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP_X1;
import static org.objectweb.asm.Opcodes.DUP_X2;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.V1_5;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.runtime.Args;
import suneido.runtime.BlockReturnException;
import suneido.runtime.BlockSpec;
import suneido.runtime.ContextLayered;
import suneido.runtime.Dynamic;
import suneido.runtime.Except;
import suneido.runtime.FunctionSpec;
import suneido.runtime.Ops;
import suneido.runtime.Range;
import suneido.runtime.SuCallable;
import suneido.runtime.SuClass;
import suneido.runtime.builtin.ObjectClass;
import suneido.runtime.builtin.RecordClass;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

/**
 * Used by {@link AstCompile} to generate Java classes.
 * Wraps use of ASM.
 */
public class ClassGen {
	private static final String COMPILED_CODE_PACKAGE_SLASHES = "suneido/code/";
	private static final String COMPILED_CODE_PACKAGE_DOTS = COMPILED_CODE_PACKAGE_SLASHES.replace('/', '.');
	private static final String CLASS_GEN_INTERNAL_NAME = Type.getInternalName(ClassGen.class);
	private static final String OPS_INTERNAL_NAME = Type.getInternalName(Ops.class);
	private static final String DYNAMIC_INTERNAL_NAME = Type.getInternalName(Dynamic.class);
	private static final String RANGE_DESCRIPTOR = Type.getDescriptor(Range.class);
	private static final String EXCEPT_DESCRIPTOR = Type.getDescriptor(Except.class);
	private static final String BLOCK_RETURN_EXCEPTION_INTERNAL_NAME = Type.getInternalName(BlockReturnException.class);
	private static final String BLOCK_RETURN_EXCEPTION_DESCRIPTOR = Type.getDescriptor(BlockReturnException.class);
	private static final String OBJECT_BUILTIN_CLASS_INTERNAL_NAME = Type.getInternalName(ObjectClass.class);
	private static final String RECORD_BUILTIN_CLASS_INTERNAL_NAME = Type.getInternalName(RecordClass.class);
	private static final String ARGS_SPECIAL_INTERNAL_NAME = Type.getInternalName(Args.Special.class);
	private static final String ARGS_SPECIAL_DESCRIPTOR = Type.getDescriptor(Args.Special.class);
	private static final int THIS = 0;
	private int SELF = -1;
	private int ARGS = -2;
	private final ContextLayered context;
	private final String name;
	private final String base;
	private final String className;
	private final ClassWriter cw;
	private final ClassVisitor cv;
	private final MethodVisitor mv;
	private final Label startLabel;
	private final int firstParam;
	private int nParams;
	private int nDefaults = 0;
	private boolean atParam = false;
	private final int iBlockParams; // where block params start in locals
	private final List<Object> constants = Lists.newArrayList();
	final List<String> locals;
	final BiMap<String,Integer> javaLocals = HashBiMap.create();
	private int nextJavaLocal;
	private TryCatch blockReturnCatcher = null;
	private TryCatch dynamicFinally = null;
	final boolean useArgsArray;
	final boolean isBlock;
	final boolean closure;
	final int parentId;
	private final List<String> dynParams = Lists.newArrayList();
	private int lastLineNumber;
	private Label lastLabelNotUsedForLineNumber;

	ClassGen(ContextLayered context, BaseClassSet baseClassSet,
			String name, String method, List<String> locals,
			boolean useArgsArray, boolean isBlock, int nParams, int parentId,
			PrintWriter pw) {
		this.context = context;
		this.base = useArgsArray ? baseClassSet.getInternalName() : 
			baseClassSet.getInternalName(nParams);
		this.name = name;
		this.className = makeInternalName(name);
		this.locals = locals == null ? new ArrayList<String>() : locals;
		this.useArgsArray = useArgsArray;
		this.isBlock = isBlock;
		this.parentId = parentId;
		closure = isBlock && baseClassSet == BaseClassSet.CALLABLE;
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
				mv = methodVisitor(ACC_PUBLIC, "call" + nParams, "("
						+ directArgs[nParams] + ")Ljava/lang/Object;");
			} else {
				assert method.equals("eval");
				mv = methodVisitor(ACC_PUBLIC, "eval" + nParams,
						"(Ljava/lang/Object;" + directArgs[nParams]
								+ ")Ljava/lang/Object;");
				javaLocals.put("_self_", SELF = nextJavaLocal++);
			}
		}
		firstParam = nextJavaLocal;
		mv.visitCode();
		startLabel = placeLabel();
		lastLineNumber = -1;
		lastLabelNotUsedForLineNumber = startLabel;
		if (!closure && useArgsArray)
			massage();
	}

	private ClassVisitor classVisitor(PrintWriter pw) {
		ClassVisitor cv = cw;
		if (pw != null)
			cv = new TraceClassVisitor(cw, pw);
		cv = new CheckClassAdapter(cv, false);
		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, className,
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
		mv.visitVarInsn(ALOAD, ARGS);
		mv.visitMethodInsn(INVOKESPECIAL, BaseClassSet.CALLABLE.getInternalName(),
				"massage", "([Ljava/lang/Object;)[Ljava/lang/Object;", false);
		mv.visitVarInsn(ASTORE, ARGS);
	}

	void param(String param, Object defaultValue, String privatePrefix) {
		atParam = param.startsWith("@");
		boolean dotParam = param.startsWith(".");
		boolean dynParam = param.startsWith("_") || param.startsWith("._");
		String name = AstVariables.paramToName(param);
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

		if (dotParam) {
			// member ref lvalue
			localLoad("this");
			String afterPrefix = param.substring(dotParam && dynParam ? 2 : 1);
			String prefix = Character.isLowerCase(afterPrefix.charAt(0)) ? privatePrefix : "";
			constant(prefix + afterPrefix);
			// parameter value
			localLoad(name);
			// .name = name
			memberStore();
		}
		if (dynParam)
			dynParams.add(name);
	}

	void constant(Object value) {
		if (value == Boolean.TRUE || value == Boolean.FALSE)
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean",
					value == Boolean.TRUE ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
		else if (value instanceof String)
			mv.visitLdcInsn(value);
		else if (value instanceof Integer) {
			iconst((Integer) value);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
					"(I)Ljava/lang/Integer;", false);
		} else {
			int i = addConstant(value);
			loadConstant(i);
		}
	}

	/**
	 * Create a const<i> field, make it static final so it will be optimized.
	 * @return i
	 */
	int addConstant(Object value) {
		int i = constants.size();
		FieldVisitor fv = cv.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
				"const" + i, "Ljava/lang/Object;", null, null);
		fv.visitEnd();
		constants.add(value);
		return i;
	}

	private void loadConstant(int i) {
		mv.visitFieldInsn(GETSTATIC, className, "const" + i, "Ljava/lang/Object;");
	}

	int iconst(int i) {
		iconst(mv, i);
		return i;
	}

	void iconst(MethodVisitor mv, int i) {
		if (-1 <= i && i <= 5)
			mv.visitInsn(ICONST_0 + i);
		else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE)
			mv.visitIntInsn(BIPUSH, i);
		else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE)
			mv.visitIntInsn(SIPUSH, i);
		else
			mv.visitLdcInsn(i);
	}

	void bool(boolean x, boolean intBool) {
		if (intBool)
			mv.visitInsn(ICONST_0 + (x ? 1 : 0));
		else
			mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean",
					x  ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
	}

	void pop() {
		mv.visitInsn(POP);
	}

	void returnValue() {
		if (isBlock)
			blockReturn();
		else
			areturn();
	}

	void areturn() {
		if (dynamicFinally != null)
			dynamicPop();
		mv.visitInsn(ARETURN);
	}

	private void blockReturn() {
		iconst(parentId);
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "blockReturnException",
				"(Ljava/lang/Object;I)" + BLOCK_RETURN_EXCEPTION_DESCRIPTOR, false);
		mv.visitInsn(ATHROW);
	}

	void aconst_null() {
		mv.visitInsn(ACONST_NULL);
	}

	void unaryOp(String method, String type) {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, method,
				"(Ljava/lang/Object;)Ljava/lang/" + type + ";", false);
	}
	void unaryOp(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, op.method,
				"(Ljava/lang/Object;)" + op.resultType.type, false);
	}
	void binaryOp(Token op, boolean intBool) {
		if (intBool && op.resultType == TokenResultType.B)
			op = op.other;
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, op.method,
				"(Ljava/lang/Object;Ljava/lang/Object;)" + op.resultType.type, false);
	}

	void rangeTo() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "rangeTo",
				"(Ljava/lang/Object;Ljava/lang/Object;)" + RANGE_DESCRIPTOR, false);
	}
	void rangeLen() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "rangeLen",
				"(Ljava/lang/Object;Ljava/lang/Object;)" + RANGE_DESCRIPTOR, false);
	}

	void localLoad(String name) {
		if (name.equals("this"))
			mv.visitVarInsn(ALOAD, SELF);
		else {
			int ref = localRef(name);
			localRefLoad(ref);
		}
	}

	static final int ARGS_REF = -1;
	static final int MEMBER_REF = -2;
	static final int DYNAMIC_REF = -3;
	// >= 0 means java local index

	int localRef(String name) {
		if (! useArgsArray)
			return javaLocal(name);
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, ARGS);
		iconst(local(name));
		return ARGS_REF;
	}

	void localRefLoad(int ref) {
		if (ref == ARGS_REF)
			mv.visitInsn(AALOAD);
		else
			mv.visitVarInsn(ALOAD, ref);
	}

	void localRefStore(int ref) {
		if (ref == ARGS_REF)
			mv.visitInsn(AASTORE);
		else
			mv.visitVarInsn(ASTORE, ref);
	}

	int local(String name) {
		// use lastIndexOf so that block parameters override locals
		int i = locals.lastIndexOf(name);
		if (i == -1) {
			i = locals.size();
			locals.add(name);
		}
		return i;
	}

	int javaLocal(String name) {
		// don't javify here because FunctionSpec needs original names
		Integer i = javaLocals.get(name);
		if (i != null)
			return i;
		javaLocals.put(name, nextJavaLocal);
		return nextJavaLocal++;
	}

	static String javify(String name) {
		return name.replace("?", "_Q_").replace("!", "_X_");
	}

	void globalLoad(int slot) {
		mv.visitVarInsn(ALOAD, THIS);
		iconst(slot);
		mv.visitMethodInsn(INVOKESPECIAL, BaseClassSet.CALLABLE.getInternalName(),
				"contextGet", "(I)Ljava/lang/Object;", false);
	}

	void memberLoad() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "get",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
	}

	void memberStore() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "put",
				"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false);
	}

	int dynamicRef(String name) {
		mv.visitLdcInsn(name);
		return DYNAMIC_REF;
	}

	void dynamicLoad(String name) {
		mv.visitLdcInsn(name);
		dynamicLoad();
	}

	void dynamicLoad() {
		mv.visitMethodInsn(INVOKESTATIC, DYNAMIC_INTERNAL_NAME,
				"get", "(Ljava/lang/String;)Ljava/lang/Object;", false);
	}

	/** name and value should be on the stack already */
	void dynamicStore() {
		mv.visitMethodInsn(INVOKESTATIC, DYNAMIC_INTERNAL_NAME,
				"put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
	}

	void dup() {
		mv.visitInsn(DUP);
	}

	void dupLvalue(int ref) {
		if (ref == DYNAMIC_REF)
			mv.visitInsn(DUP);
		else if (ref < 0)
			mv.visitInsn(DUP2);
		// else lvalue is java local
		// 		nothing to dup
	}

	void dupUnderLvalue(int ref) {
		if (ref >= 0) // lvalue is java local
			mv.visitInsn(DUP);
		else if (ref == DYNAMIC_REF)
			mv.visitInsn(DUP_X1);
		else
			mv.visitInsn(DUP_X2);
	}

	boolean neverNull(String name) {
		return name.equals("this") || local(name) < nParams;
	}

	void addNullCheck(String error) {
		Label label = label();
		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNONNULL, label);
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME,
				"throw" + error, "()V", false);
		placeLabel(label);
	}

	void callFunction() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "call",
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
	}

	void callFunction(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "call" + nargs,
				"(" + directArgs[nargs + 1] + ")Ljava/lang/Object;", false);
	}

	void invokeGlobal() {
		mv.visitMethodInsn(INVOKESPECIAL,
				BaseClassSet.CALLABLE.getInternalName(), "invoke",
				"(I[Ljava/lang/Object;)Ljava/lang/Object;", false);
	}

	void invokeGlobal(int nargs) {
		mv.visitMethodInsn(INVOKESPECIAL,
				BaseClassSet.CALLABLE.getInternalName(), "invoke" + nargs, "(I"
						+ directArgs[nargs] + ")Ljava/lang/Object;", false);
	}

	void invokeMethod() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "invoke",
			"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
			false);
	}

	void invokeMethod(int nargs) {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "invoke" + nargs,
			"(Ljava/lang/Object;Ljava/lang/String;" + directArgs[nargs] + ")Ljava/lang/Object;",
			false);
	}

	private static final int MAX_DIRECT_ARGS = 11;
	private static final String[] directArgs = new String[MAX_DIRECT_ARGS];
	static {
		for (int n = 0; n < MAX_DIRECT_ARGS; ++n)
			directArgs[n] = Strings.repeat("Ljava/lang/Object;", n);
	}

	void anewarray(int size) {
		iconst(size);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
	}

	void aastore() {
		mv.visitInsn(AASTORE);
	}

	void invokeDirect(String name) {
		// Name can only be "Object" or "Record" right now...
		String className = null;
		if ("Object".equals(name)) {
			className = OBJECT_BUILTIN_CLASS_INTERNAL_NAME;
		} else if ("Record".equals(name)) {
			className = RECORD_BUILTIN_CLASS_INTERNAL_NAME;
		} else {
			throw new SuInternalError("unexpected invoke direct name: '" + name + '\'');
		}
		mv.visitMethodInsn(INVOKESTATIC, className, "create",
				"([Ljava/lang/Object;)Ljava/lang/Object;", false);
	}

	void superCallTarget(String method) {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn(method);
	}

	void invokeSuper() {
		mv.visitMethodInsn(
				INVOKESPECIAL,
				BaseClassSet.CALLABLE.getInternalName(),
				"superInvoke",
				"(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
				false);
	}

	void superInit() {
		mv.visitVarInsn(ALOAD, THIS);
		mv.visitVarInsn(ALOAD, SELF);
		mv.visitLdcInsn("New");
		anewarray(0);
		invokeSuper();
		mv.visitInsn(POP);
	}

	void pushThis() {
		mv.visitVarInsn(ALOAD, THIS);
	}

	void toMethodString() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME,
				"toMethodString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
	}

	void specialArg(String which) {
		mv.visitFieldInsn(GETSTATIC, ARGS_SPECIAL_INTERNAL_NAME, which,
				ARGS_SPECIAL_DESCRIPTOR);
	}

	void thrower() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "exception",
				"(Ljava/lang/Object;)Ljava/lang/Throwable;", false);
		mv.visitInsn(ATHROW);
	}

	void blockThrow(String which) {
		mv.visitFieldInsn(GETSTATIC, OPS_INTERNAL_NAME,
				which, "Lsuneido/SuException;");
		mv.visitInsn(ATHROW);
	}

	private static class TryCatch {
		Label label0 = new Label();
		Label label1 = new Label();
		Label label2 = new Label();
		Label label3 = new Label();
	}

	Object tryCatch(String exception) {
		return startTryCatch(exception);
	}

	private TryCatch startTryCatch(String exception) {
		TryCatch tc = new TryCatch();
		mv.visitTryCatchBlock(tc.label0, tc.label1, tc.label2, exception);
		placeLabel(tc.label0);
		return tc;
	}

	void startCatch(String var, String pattern, Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		placeLabel(tc.label1);
		mv.visitJumpInsn(GOTO, tc.label3);
		placeLabel(tc.label2);

		// exception is on stack
		if (pattern != null) {
			mv.visitLdcInsn(pattern);
			mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "catchMatch",
					"(Ljava/lang/Throwable;Ljava/lang/String;)" + EXCEPT_DESCRIPTOR,
					false);
		} else if (var != null)
			mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "catchMatch",
					"(Ljava/lang/Throwable;)" + EXCEPT_DESCRIPTOR, false);
		if (var == null)
			mv.visitInsn(POP);
		else
			saveTopInVar(var);
	}

	void endCatch(Object trycatch) {
		TryCatch tc = (TryCatch) trycatch;
		placeLabel(tc.label3);
	}

	Object ifFalse() {
		Label label = label();
		mv.visitJumpInsn(IFEQ, label);
		return label;
	}

	void ifFalse(Object label) {
		mv.visitJumpInsn(IFEQ, (Label) label);
	}

	void ifTrue(Object label) {
		mv.visitJumpInsn(IFNE, (Label) label);
	}

	Label label() {
		return new Label();
	}

	Label placeLabel() {
		Label label = label();
		placeLabel(label);
		return label;
	}

	void placeLabel(Label label) {
		mv.visitLabel(label);
		lastLabelNotUsedForLineNumber = label;
	}

	Label jump() {
		Label label = label();
		mv.visitJumpInsn(GOTO, label);
		return label;
	}

	void jump(Object label) {
		mv.visitJumpInsn(GOTO, (Label) label);
	}

	void toIntBool() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "toIntBool",
				"(Ljava/lang/Object;)I", false);
	}

	// for in

	int iter() {
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "iterator",
				"(Ljava/lang/Object;)Ljava/lang/Object;", false);
		return storeTemp();
	}

	int storeTemp() {
		mv.visitVarInsn(ASTORE, nextJavaLocal);
		return nextJavaLocal++;
	}

	void loadTemp(int temp) {
		mv.visitVarInsn(ALOAD, temp);
	}

	/** leaves boolean result on the stack */
	void hasNext(int temp) {
		mv.visitVarInsn(ALOAD, temp);
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "hasNext",
				"(Ljava/lang/Object;)Z", false);
	}
	void next(String var, int temp) {
		loadTemp(temp);
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "next",
				"(Ljava/lang/Object;)Ljava/lang/Object;", false);
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

	void block(int iBlockDef, int nParams, boolean useArgsArray) {
		// new SuBlock(block, self, locals)
		final String className = useArgsArray
				? BaseClassSet.BLOCK.getInternalName()
				: BaseClassSet.BLOCK.getInternalName(nParams);
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		loadConstant(iBlockDef);			// block
		if (SELF >= 0)
			mv.visitVarInsn(ALOAD, SELF); 	// self
		else
			mv.visitInsn(ACONST_NULL);
		assert ARGS >= 0;
		mv.visitVarInsn(ALOAD, ARGS); 		// locals
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>",
				"(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)V",
				false);
		addBlockReturnCatcher();
	}

	void addBlockReturnCatcher() {
		if (! isBlock && blockReturnCatcher == null)
			blockReturnCatcher = startTryCatch(BLOCK_RETURN_EXCEPTION_INTERNAL_NAME);
	}

	private void finishBlockReturnCatcher() {
		placeLabel(blockReturnCatcher.label1);
		placeLabel(blockReturnCatcher.label2);
		iconst(parentId);
		mv.visitMethodInsn(INVOKESTATIC, OPS_INTERNAL_NAME, "blockReturnHandler",
				"(" + BLOCK_RETURN_EXCEPTION_DESCRIPTOR + "I)Ljava/lang/Object;",
				false);
		areturn();
	}

	void addDynamicPushPop() {
		dynamicPush();
		dynamicFinally = startTryCatch(null);
	}

	private void dynamicPush() {
		mv.visitMethodInsn(INVOKESTATIC, DYNAMIC_INTERNAL_NAME, "push", "()V",
				false);
	}

	/** catcher that just pops and rethrows */
	private void finishDynamicPushPop() {
		placeLabel(dynamicFinally.label1);
		placeLabel(dynamicFinally.label2);
		dynamicPop();
		mv.visitInsn(ATHROW);
	}

	private void dynamicPop() {
		mv.visitMethodInsn(INVOKESTATIC, DYNAMIC_INTERNAL_NAME, "pop", "()V",
				false);
	}

	SuCallable end(SuClass suClass) {
		if (blockReturnCatcher != null)
			finishBlockReturnCatcher();
		if (dynamicFinally != null)
			finishDynamicPushPop();

		Label endLabel = placeLabel();
		BiMap<Integer, String> bm = javaLocals.inverse();
		for (int i = 0; i < nextJavaLocal; ++i)
			if (bm.containsKey(i))
				mv.visitLocalVariable(javify(bm.get(i)), "[Ljava/lang/Object;", null,
						startLabel, endLabel, i);

		mv.visitMaxs(0, 0);
		mv.visitEnd();

		if (! constants.isEmpty())
			genClinit();

		cv.visitEnd();

		SuCallable callable;
		shareConstants.set(constants);
		try {
			Loader loader = new Loader();
			Class<?> sc = loader.defineClass(COMPILED_CODE_PACKAGE_DOTS + name,
					cw.toByteArray());
			try {
				callable = (SuCallable) sc.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new SuException("newInstance error: " + e);
			}
		} finally {
			shareConstants.set(null);
		}

		return callable.finishInit(suClass, functionSpec(bm), context, isBlock);
	}

	static String[] stringArray = new String[0];

	private FunctionSpec functionSpec(BiMap<Integer,String> bm) {
		FunctionSpec fspec;
		String[] params = new String[nParams];
		if (closure) {
			for (int i = 0; i < nParams; ++i)
				params[i] = locals.get(i + iBlockParams);
			fspec = new BlockSpec(name, params, nParams, atParam, iBlockParams);
			hideBlockParams();
		} else if (useArgsArray) {
			for (int i = 0; i < nParams; ++i)
				params[i] = locals.get(i);
			fspec = new FunctionSpec(name, params, constantsArray(), atParam,
					dynParams.toArray(stringArray), locals.size());
		} else {
			for (int i = 0; i < nParams; ++i)
				params[i] = bm.get(i + firstParam);
			fspec = new FunctionSpec(name, params, constantsArray(), atParam,
					dynParams.toArray(stringArray));
		}
		return fspec;
	}

	private void hideBlockParams() {
		for (int i = iBlockParams; i < iBlockParams + nParams; ++i)
			locals.set(i, "_" + locals.get(i));
	}

	private Object[] constantsArray() {
		Object[] a = new Object[nDefaults];
		for (int i = 0; i < nDefaults; ++i)
			a[i] = constants.get(i);
		return a;
	}

	static class Loader extends ClassLoader {
		public Class<?> defineClass(String name, byte[] b) {
			return defineClass(name, b, 0, b.length);
		}
	}

	public static ThreadLocal<List<Object>> shareConstants =
			new ThreadLocal<>();

	void putLineNumber(int lineNumber) {
		// This method tries to re-use existing labels where possible rather
		// than creating redundant new ones. For example, if you have code that
		// looks like this:
		//     n:  if (expr)
		//     n+1:    {
		//     n+2:    x = 0
		//     n+3:    y = 0
		//     n+4:    }
		// Then the "if" statement will generate an "ifTrue" label that jumps to
		// the first instruction of "x = 0". In this case, the "ifTrue" label
		// will be reused for marking line n+2. However, line n+3 will produce a
		// new label that is only used for marking line n+3.
		if (lineNumber != lastLineNumber) {
			assert 0 < lineNumber;
			Label lineNumberLabel = lastLabelNotUsedForLineNumber;
			if (null == lineNumberLabel) {
				lineNumberLabel = new Label();
				mv.visitLabel(lineNumberLabel);
			} else {
				lastLabelNotUsedForLineNumber = null;
			}
			mv.visitLineNumber(lineNumber, lineNumberLabel);
			lastLineNumber = lineNumber;
		} else {
			// The last label placed before this method was called is only
			// useful as a line number marker if we haven't seen any
			// instructions from a new line number after placing it. 
			lastLabelNotUsedForLineNumber = null;
		}
	}

	/**
	 * generator a constructor that sets const0, const1, etc.
	 * to the values in shareConstants
	 */
	private void genClinit() {
		MethodVisitor mv = cv.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitFieldInsn(GETSTATIC, CLASS_GEN_INTERNAL_NAME, "shareConstants",
				"Ljava/lang/ThreadLocal;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get",
				"()Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, "java/util/List");
		mv.visitVarInsn(ASTORE, 0);
		for (int i = 0; i < constants.size(); ++i) {
			mv.visitVarInsn(ALOAD, 0);
			iconst(mv, i);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List",
					"get", "(I)Ljava/lang/Object;", true);
			mv.visitFieldInsn(PUTSTATIC, className,
					"const" + i, "Ljava/lang/Object;");
		}
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("constants", "Ljava/util/List;",
				"Ljava/util/List<Ljava/lang/Object;>;", l0, l1, 0);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}

	private void genInit() {
		MethodVisitor mv =
				cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V", false);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + className + ";",
				null, l0, l1, 0);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	/**
	 * Returns the fully-qualified "internal name" for the Java class that will
	 * hold the bytecode for the Suneido entity with the given name.
	 * 
	 * @param name
	 *            Suneido entity name
	 * @return Java "internal name" for the class holding the Suneido entity's
	 *         compiled bytecode
	 * @since 20140829
	 */
	public static String makeInternalName(String name) {
		return COMPILED_CODE_PACKAGE_SLASHES + name;
	}
}
