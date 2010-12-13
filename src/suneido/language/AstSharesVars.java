/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.HashSet;
import java.util.Set;

import suneido.util.Stack;

/**
 * Determine if a function shares variables with blocks.
 * Used by {@link AstCompile} to choose between args array and java locals.
 */
public class AstSharesVars {

	public static boolean check(AstNode ast) {
		Visitor hsv = new Visitor(ast);
		ast.depthFirst(hsv);
		return hsv.hasSharedVars;
	}

	private static class Visitor extends AstNode.Visitor {
		private final AstNode root;
		public boolean hasSharedVars;
		private int blockNest = 0; // > 0 means in block
		private final Set<String> outerVars = new HashSet<String>();
		private final Stack<Set<String>> blockParams = new Stack<Set<String>>();
		private final Set<String> blockVars = new HashSet<String>();
		private boolean inBlockParams;

		Visitor(AstNode root) {
			this.root = root;
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
				inBlockParams = true;
				blockParams.push(new HashSet<String>());
				++blockNest;
				break;
			case IDENTIFIER:
				String id = ast.value;
				if (blockNest == 0) {
					if (blockVars.contains(id))
						hasSharedVars = true;
					else
						outerVars.add(id);
				} else if (inBlockParams)
					blockParams.top().add(id);
				else if ("this".equals(id))
					hasSharedVars = true;
				else if (! blockParams.top().contains(id)) {
					if (outerVars.contains(id))
						hasSharedVars = true;
					else
						blockVars.add(id);
				}
				break;
			case SELFREF:
			case SUPER:
				if (blockNest > 0)
					hasSharedVars = true;
				break;
			}
			return ! hasSharedVars; // stop when you know
		}

		@Override
		void bottomUp(AstNode ast) {
			switch (ast.token) {
			case LIST:
				inBlockParams = false;
				break;
			case BLOCK:
				blockParams.pop();
				--blockNest;
				break;
			}
		}
	}

}
