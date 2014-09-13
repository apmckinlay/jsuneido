/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.compiler.AstCompile;

/**
 * <p>
 * Base class for class methods as well as stand-alone functions and blocks that
 * reference "{@code this}". Callable entities derived from this class define
 * {@code eval}.
 * </p>
 * 
 * <p>
 * Standalone functions and blocks derive from {@link SuCallBase} and define
 * {@code call}.
 * </p>
 *
 * <p>
 * For simple args {@link SuEvalBase0} ... {@link SuEvalBase4} are used.
 * </p>
 *
 * @author Andrew McKinlay, Victor Schappert
 * @see SuBoundMethod
 */
public abstract class SuEvalBase extends SuCallable {

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String display() {
		if (CallableType.METHOD == callableType) {
			String javaClassName = getClass().getSimpleName();
			final int N = javaClassName.length();
			StringBuilder sb = new StringBuilder(N + 32);
			int i = javaClassName.indexOf(AstCompile.METHOD_SEPARATOR);
			assert 0 < i : "Method class name must include method separator";
			if ("eval".equals(name)) {
				sb.append("/* ").append(callableType.displayString())
						.append(' ').append(javaClassName, i + 1, N);
			} else {
				sb.append(javaClassName, 0, i).append('.')
						.append(javaClassName, i + 1, N).append(" /* ");
				appendLibrary(sb);
				sb.append(callableType.displayString());
			}
			return sb.append(" */").toString();
		} else {
			return super.display();
		}
	}

	@Override
	public abstract Object eval(Object self, Object... args);

	@Override
	public Object call(Object... args) {
		return eval(this, args);
	}

	@Override
	public Object call0() {
		return eval0(this);
	}

	@Override
	public Object call1(Object a) {
		return eval1(this, a);
	}

	@Override
	public Object call2(Object a, Object b) {
		return eval2(this, a, b);
	}

	@Override
	public Object call3(Object a, Object b, Object c) {
		return eval3(this, a, b, c);
	}

	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		return eval4(this, a, b, c, d);
	}

}
