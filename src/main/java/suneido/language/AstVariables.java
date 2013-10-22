/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.util.Util.uncapitalize;

import java.util.HashSet;
import java.util.Set;

/** get the set of top level variables for a function */
public class AstVariables {

	public static Set<String> vars(AstNode ast) {
		Variables vars = new Variables(ast);
		ast.depthFirst(vars);
		return vars.vars;
	}

	private static class Variables extends AstNode.Visitor {
		private final AstNode root;
		private final Set<String> vars = new HashSet<>();

		Variables(AstNode root) {
			this.root = root;
		}

		@Override
		boolean topDown(AstNode ast) {
			switch (ast.token) {
			// don't process nested classes, functions, or blocks
			case FUNCTION:
			case CLASS:
			case BLOCK:
				return ast == root ? true : false;
			case IDENTIFIER:
			case FOR_IN:
			case CATCH:
				if (ast.value != null) {
					String var = paramToName(ast.value);
					if (Character.isLowerCase(var.charAt(0)))
						vars.add(var);
				}
				return true;
			case SELFREF:
			case SUPER:
				vars.add("this");
				return true;
			default:
				return true;
			}
		}

	}

	/** &#64;name .name .Name _name ._name ._Name => name */
	static String paramToName(String name) {
		int i = 0;
		if (name.startsWith("@"))
			return name.substring(1);
		if (name.startsWith("."))
			i = 1;
		if (name.charAt(i) == '_')
			++i;
		return (i == 0) ? name : uncapitalize(name.substring(i));
	}

	public static void main(String[] args) {
		AstNode ast = Compiler.parse("function (@x) { a + b }");
		System.out.println(vars(ast));
	}

}
