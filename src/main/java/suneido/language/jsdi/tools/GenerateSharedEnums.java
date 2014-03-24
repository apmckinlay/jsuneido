/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.tools;

import java.io.File;
import java.util.ArrayList;

/**
 * Automatic C++ code generator for the JSDI {@code java_enum} translation unit.
 * 
 * @author Victor Schappert
 * @since 20130630
 */
public final class GenerateSharedEnums {

	private static final class Ref {
		public final EnumLineEditor.Header<?> header;
		public final EnumLineEditor.Source<?> source;

		public <E extends Enum<E>> Ref(Class<E> clazz,
				boolean wantToJNIConverter, boolean wantFromJNIConverter,
				boolean wantStreamInsertion) {
			this.header = new EnumLineEditor.Header<E>(clazz,
					GenerateSharedEnums.class, wantToJNIConverter,
					wantFromJNIConverter, wantStreamInsertion);
			this.source = new EnumLineEditor.Source<E>(clazz,
					GenerateSharedEnums.class, wantToJNIConverter,
					wantFromJNIConverter, wantStreamInsertion);
		}
	}

	private static final Ref[] REFS = { new Ref(
			suneido.language.jsdi.VariableIndirectInstruction.class, false, true, true) };

	private static void error(String message) {
		System.err.println(message);
		System.exit(1);
	}

	private static final class HeaderLineEditor extends LineEditor {
		private final Ref[] refs;

		public HeaderLineEditor(Ref[] refs) {
			super("GENERATED CODE");
			this.refs = refs;
		}

		@Override
		public ArrayList<String> makeLines() {
			for (Ref ref : refs)
				lines.addAll(ref.header.makeLines());
			return lines;
		}
	}

	private static final class SourceLineEditor extends LineEditor {
		private final Ref[] refs;

		public SourceLineEditor(Ref[] refs) {
			super("GENERATED CODE");
			this.refs = refs;
		}

		@Override
		public ArrayList<String> makeLines() {
			for (Ref ref : refs)
				lines.addAll(ref.source.makeLines());
			return lines;
		}
	}

	public static void main(String[] args) {
		if (2 != args.length)
			error("usage: <header-file> <cpp-file>");
		final File headerFile = new File(args[0]);
		if (!headerFile.exists())
			error("No such file: " + args[0]);
		final File sourceFile = new File(args[1]);
		if (!sourceFile.exists())
			error("No such file: " + args[1]);
		String action = "";
		try {
			action = "make references array";
			action = "edit header file '" + headerFile + "'";
			new FileEditor(headerFile, new HeaderLineEditor(REFS)).edit();
			action = "edit source file '" + sourceFile + "'";
			new FileEditor(sourceFile, new SourceLineEditor(REFS)).edit();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			error("Failed to " + action);
		}
	}
}
