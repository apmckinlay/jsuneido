/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

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
		private final Set<String> vars = new HashSet<String>();

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
					String var = ast.value;
					if (var.charAt(0) == '@')
						var = var.substring(1);
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


}
