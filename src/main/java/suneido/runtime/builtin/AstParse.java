/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.compiler.AstNode;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class AstParse {

	@Params("string")
	public static Object AstParse(Object a) {
		String src = Ops.toStr(a);
		AstNode ast = suneido.compiler.Compiler.parse(src);
		return new AstWrapper(ast);
	}

	private static class AstWrapper extends SuValue {
		AstNode node;

		public AstWrapper(AstNode node) {
			this.node = node;
		}

		@Override
		public Object get(Object member) {
			switch (Ops.toStr(member)) {
			case "Value":
				return node.value == null ? "" : node.value;
			case "Token":
				return node.token.toString();
			case "Line":
				return node.lineNumber;
			case "Children":
				SuContainer children = new SuContainer(node.children.size());
				if (node.children != null)
					for (AstNode child : node.children)
						if (child != null)
							children.add(new AstWrapper(child));
				return children;
			}
			throw new SuException("AstNode unknown member: " + member);
		}

		@Override
		public String toString() {
			return "AstNode";
		}

		@Override
		public String typeName() {
			return "AstNode";
		}

	}

}
