/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

import suneido.SuValue;
import suneido.TheDbms;
import suneido.database.server.Dbms.LibGet;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Pack;

/**
 * <p>
 * Call stack with the data required for "none" debugging. Corresponds to
 * {@link DebugModel#OFF}.
 * </p>
 *
 * <p>
 * This call stack implementation simply regurgitates whatever Java stack frame
 * data is available.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140904
 */
@Immutable
public class CallstackNone extends Callstack {

	//
	// CONSTANTS
	//

	static final LocalVariable[] EMPTY_LOCAL_ARRAY = new LocalVariable[0];

	//
	// CONSTRUCTORS
	//

	CallstackNone(Throwable throwable) {
		super(throwable);
	}

	//
	// TYPES
	//

	@Immutable
	private static final class StackTraceElementWrapper extends Frame {
		private final StackTraceElement ste;

		StackTraceElementWrapper(StackTraceElement ste) {
			super(CallstackNone.EMPTY_LOCAL_ARRAY);
			this.ste = ste;
		}

		//
		// ANCESTOR CLASS: Frame
		//

		@Override
		public Object getFrame() {
			return new PseudoFunction(ste);
		}

		@Override
		public int getLineNumber() {
			return ste.getLineNumber();
		}

		//
		// ANCESTOR CLASS: Object
		//

		@Override
		public String toString() {
			return ste.toString();
		}
	}

	@Immutable
	public static final class PseudoFunction extends SuValue {

		private final StackTraceElement ste;

		private PseudoFunction(StackTraceElement ste) {
			this.ste = ste;
		}

		// private static final Pattern LIBPAT = Pattern.compile(
		// "library\\[([^\\]]+)\\]->(.+)");
		private Object sourceCode() {
			final Pattern LIBPAT = Pattern
					.compile("library\\[([^\\]]+)\\]->(.+)");
			Matcher m = LIBPAT.matcher(ste.getFileName());
			if (m.matches()) {
				final String library = m.group(1);
				final String item = m.group(2);
				List<LibGet> libgets = TheDbms.dbms().libget(item);
				for (LibGet libget : libgets) {
					if (library.equals(libget.library)) {
						return Pack.unpack(libget.text);
					}
				}
			}
			return Boolean.FALSE;
		}

		//
		// ANCESTOR CLASS: SuValue
		//

		@Override
		public String typeName() {
			return "aPseudoFunction";
		}

		@Override
		public String display() {
			StringBuilder sb = new StringBuilder(128);
			sb.append(ste.getClassName()).append('.')
					.append(ste.getMethodName()).append(" (")
					.append(ste.getFileName());
			if (0 < ste.getLineNumber()) {
				sb.append(':').append(ste.getLineNumber());
			}
			return sb.append(')').toString();
		}

		private static final BuiltinMethods builtins = new BuiltinMethods(
				PseudoFunction.class);

		@Override
		public SuValue lookup(String method) {
			return builtins.lookup(method);
		}

		//
		// BUILT-IN METHODS
		//

		public static Object Source(Object self) {
			return ((PseudoFunction) self).sourceCode();
		}
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected List<Frame> makeFrames() {
		final StackTraceElement[] stackTrace = throwable.getStackTrace();
		final Frame[] frames = new Frame[stackTrace.length];
		for (int k = 0; k < stackTrace.length; ++k) {
			frames[k] = new StackTraceElementWrapper(stackTrace[k]);
		}
		return Arrays.asList(frames);
	}
}
