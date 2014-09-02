/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.util.Set;

import suneido.util.Stack;

import com.google.common.collect.Sets;

/**
 * Determine if a function shares variables with blocks.
 * Used by {@link AstCompile} to choose between args array and java locals.<p>
 * Also marks any child blocks (possibly multiply nested)
 * that share variables with this function
 * and therefore require compiling as closure blocks.
 *
 * Tricky cases:<br>
 * <li>where inner block shares a parameter of its containing block
 * <li>where inner block shares a variable of its containing block
 * <li>where sharing is with a parent several levels up
 */
public class AstSharesVars {

	public static boolean check(AstNode ast) {
		Visitor v = new Visitor(ast);
		ast.depthFirst(v);
		return v.hasSharedVars;
	}

	private static class Visitor extends AstNode.Visitor {
		private final AstNode root;
		public boolean hasSharedVars;
		private boolean needBlocks = false;
		private final Set<String> outerVars;
		private final Stack<BlockInfo> blocks = new Stack<>();

		Visitor(AstNode ast) {
			root = ast;
			outerVars = AstVariables.vars(ast);
			outerVars.add("this");
//System.out.println("\nouter vars: " + vars);
		}

		@Override
		boolean topDown(AstNode ast) {
			switch (ast.token) {
			// don't process nested classes or functions
			case CLASS:
				return false;
			case FUNCTION:
				return ast == root ? true : false;
			case BLOCK:
				blocks.push(new BlockInfo(ast));
				break;
			default:
			}
			return true;
		}

		@Override
		void bottomUp(AstNode ast) {
			if (ast.token == Token.BLOCK) {
				if (! needBlocks) {
					BlockInfo b = blocks.top();
					Set<String> nonParamVars = Sets.difference(b.vars, b.params);
					if (! Sets.intersection(outerVars, nonParamVars).isEmpty())
						hasSharedVars = needBlocks = true;
				}
				if (needBlocks)
					ast.children.set(2, compileAsBlock);
				blocks.pop();
				if (blocks.isEmpty())
					needBlocks = false;
			}
		}

		private static final AstNode compileAsBlock = new AstNode(Token.CLOSURE);

		private static class BlockInfo {
			final Set<String> params;
			final Set<String> vars;

			BlockInfo(AstNode ast) {
				assert ast.token == Token.BLOCK;
				params = AstVariables.vars(ast.first());
				vars = AstVariables.vars(ast);
			}
		}
	}

}
