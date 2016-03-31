/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.io.PrintWriter;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuContainer;
import suneido.Suneido;
import suneido.runtime.ContextLayered;
import suneido.runtime.Ops;

/**
 * <p>
 * The components of the compiler are:
 *
 * <ul>
 * <li>
 * {@link Lexer} - breaks source into tokens</li>
 * <li>
 * {@link ParseConstant}</li>
 * <li>
 * {@link ParseFunction} - used by ParseConstant</li>
 * <li>
 * {@link ParseExpression} - used by ParseFunction</li>
 * <li>
 * {@link ParseStruct} - used by ParseConstant</li>
 * <li>
 * {@link ParseDll} - used by ParseConstant</li>
 * <li>
 * {@link ParseCallback} - used by ParseConstant</li>
 * <li>
 * {@link Generator} - interface, called by parsers with results
 * <li>
 * {@link AstGenerator} - implementation of Generator to create AST</li>
 * <li>
 * {@link AstNode} - make up the AST</li>
 * <li>
 * {@link AstCompile} - compiles an AST to Java byte code</li>
 * </ul>
 * </p>
 *
 * @author Andrew McKinlay
 * @see Disassembler
 */
@ThreadSafe
public class Compiler {

	public static Object compile(String name, String src) {
		return compile("", name, src, null, Suneido.context, null, true);
	}

	public static Object compile(String name, String src, SuContainer warnings) {
		return compile("", name, src, null, Suneido.context, warnings, true);
	}

	public static Object compile(String library, String name, String src, ContextLayered context) {
		return compile(library, name, src, null, context, null, true);
	}

	static Object compile(String name, String src, PrintWriter pw,
			boolean wantLineNumbers) {
		return compile("", name, src, pw, Suneido.context, null, wantLineNumbers);
	} // testing only

	private static Object compile(String library, String name, String src,
			PrintWriter pw, ContextLayered context, SuContainer warnings,
			boolean wantLineNumbers) {
		AstNode ast = parse(src);
		if (pw != null)
			pw.append(ast.toString() + "\n\n");
		return AstCompile.fold(library, name, src, pw, context, warnings, wantLineNumbers,
				ast);
	}

	public static AstNode parse(String src) {
		Lexer lexer = new Lexer(src);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		return pc.parse();
	}

	public static Object eval(CharSequence s) {
		return Ops.call0(compile("eval", "function () { " + s + "\n}"));
	}

	public static Object eval(CharSequence s, ContextLayered context) {
		return Ops.call0(compile("", "eval", "function () { " + s + "\n}", context));
	}

//	public static void main(String[] args) /*throws IOException*/ {
////		String s = Files.toString(new java.io.File("tmp.txt"), Charsets.UTF_8);
////		String s = "function () { c = class { New(.P) { } A() { .P } }; i = c(123); i.A() }";
////		String s = "function () { _p = 123; function(_p = 0){ p }(); }";
//		String s = "function () { x=1;Print([:x]) }";
//		PrintWriter pw = new PrintWriter(System.out);
////		Object f =
//				compile("Test", s, pw, false);
//		//System.out.println(" => " + Ops.call0(f));
//		//System.out.println(" => " + Ops.call1(f, "hello"));
//	}

}
