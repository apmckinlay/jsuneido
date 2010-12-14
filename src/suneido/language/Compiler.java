/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Compiler {

	public static Object compile(String name, String src) {
		return compile(name, src, null);
	}

	public static Object compile(String name, String src, PrintWriter pw) {
		Lexer lexer = new Lexer(src);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		if (pw != null)
			pw.append(ast.toString() + "\n\n");
		return new AstCompile(name, pw).fold(ast);	}

	private static final Object[] noArgs = new Object[0];

	public static Object eval(String s) {
		Object f = compile("eval", "function () { " + s + " }");
		return Ops.call(f, noArgs);
	}

	public static void main(String[] args) throws IOException {
//		String s = Files.toString(new java.io.File("tmp.txt"), Charsets.UTF_8);
		String s = "function () { Date() }";
		PrintWriter pw = new PrintWriter(System.out);
Object f =
		compile("Test", s, pw);
		Object x = Ops.call0(f);
//		Object x = Ops.call1(f, 1);
		System.out.println(" => " + x);
	}

}
