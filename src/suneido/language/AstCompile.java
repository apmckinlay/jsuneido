package suneido.language;

import static org.objectweb.asm.Opcodes.*;

import java.io.PrintWriter;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import suneido.SuException;

public class AstCompile {
	private static final int THIS = 0;
	private static final int SELF = 1;
	private static final int ARGS = 2;
	private static final int CONSTANTS = 3;

	public static Object compile(String name, AstNode ast) {
		switch (ast.token) {
		case FUNCTION :
			return compileFunction(name, ast);
		default :
			throw new SuException("unhandled: " + ast.token);
		}
	}

	private static Object compileFunction(String name, AstNode ast) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor cv = cw;
		cv = new TraceClassVisitor(cw, new PrintWriter(System.out));
		cv = new CheckClassAdapter(cv, false);
		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "suneido/language/" + name,
				null, "suneido/lang/SuFunction", null);
		cv.visitSource("", null);

		MethodVisitor mv = methodVisitor(cv, ACC_PUBLIC, name,
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
		mv.visitCode();
		Label startLabel = new Label();
		mv.visitLabel(startLabel);

//		c.constants = new ArrayList<Object>();

		AstNode statementList = ast.children.get(1);
		for (AstNode statement : statementList.children)
			compileStatement(mv, statement);

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

		return null;
	}

	private static MethodVisitor methodVisitor(
			ClassVisitor cv, int access, String name, String desc) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, null, null);
		mv = new TryCatchBlockSorter(mv, access, name, desc, null, null);
		return mv;
	}
	private static void compileStatement(MethodVisitor mv, AstNode ast) {
		switch (ast.token) {
		case RETURN :
			compileReturn(mv, ast);
			break;
		default:
			throw new SuException("unhandled: " + ast.token);
		}

	}

	private static void compileReturn(MethodVisitor mv, AstNode ast) {
		if (ast.children.get(0) == null)
			mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
	}

	public static void main(String[] args) {
		Lexer lexer = new Lexer("function () { return }");
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		compile("Test", ast);
	}

}
