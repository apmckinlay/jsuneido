package suneido.language.jsdi.tools;

import java.util.ArrayList;

abstract class EnumLineEditor<E extends Enum<E>> extends LineEditor {

	//
	// DATA
	//

	protected final Class<E> enumType;
	protected final boolean wantToJNIConverter;
	protected final boolean wantFromJNIConverter;
	protected final boolean wantStreamInsertion;

	//
	// CONSTRUCTORS
	//

	public EnumLineEditor(Class<E> enumType, boolean wantToJNIConverter,
			boolean wantFromJNIConverter, boolean wantStreamInsertion) {
		super("GENERATED ENUM " + enumType.getCanonicalName());
		this.enumType = enumType;
		this.wantToJNIConverter = wantToJNIConverter;
		this.wantFromJNIConverter = wantFromJNIConverter;
		this.wantStreamInsertion = wantStreamInsertion;
	}

	//
	// INTERNALS
	//

	protected final String getEnumCPPTypeName() {
		return enumType.getCanonicalName().replace('.', '_');
	}

	protected final String getNameArrayName() {
		return getEnumCPPTypeName() + "__NAME";
	}

	@SuppressWarnings("unchecked")
	protected final E[] getEnumerators() {
		try {
			return (E[]) enumType.getMethod("values").invoke(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected final int getOrdinalLimit() {
		return getEnumerators().length;
	}

	protected static final String JNIENV_VAR_NAME = "env";
	protected static final String JCLASS_VAR_NAME = "clazz";
	protected static final String ENUM_VAR_NAME = "e";
	protected static final String JNIEXCEPTION_CHECK_MACRO = "JNI_EXCEPTION_CHECK";

	protected final void addFromJNISignature(boolean header) {
		add(indent(0).append("template <>"));
		StringBuilder sig = indent(0);
		sig.append(getEnumCPPTypeName());
		sig.append(" jni_enum_to_cpp(JNIEnv *");
		if (!header)
			sig.append(' ').append(JNIENV_VAR_NAME);
		sig.append(", jclass");
		if (!header)
			sig.append(' ').append(JCLASS_VAR_NAME);
		sig.append(", jobject");
		if (!header)
			sig.append(' ').append(ENUM_VAR_NAME).append(')');
		else
			sig.append(");");
		add(sig);
	}

	protected final void addOstreamInsertionSignature(boolean header) {
		StringBuilder sig = indent(0);
		sig.append("std::ostream& operator<<(std::ostream&");
		if (!header)
			sig.append(" o");
		sig.append(", const ");
		sig.append(getEnumCPPTypeName());
		if (!header)
			sig.append("& ").append(ENUM_VAR_NAME).append(')');
		else
			sig.append("&);");
		add(sig);
	}

	protected final void addOrdinalAssertion(String varName) {
		add(indent(1).append("assert(0 <= ").append(varName).append(" && ")
				.append(varName).append(" < ").append(getOrdinalLimit())
				.append(");"));
	}

	//
	// INTERNAL CLASSES
	//

	public static final class Header<F extends Enum<F>> extends
			EnumLineEditor<F> {
		public Header(Class<F> enumType, boolean wantToJNIConverter,
				boolean wantFromJNIConverter, boolean wantStreamInsertion) {
			super(enumType, wantToJNIConverter, wantFromJNIConverter,
					wantStreamInsertion);
		}

		@Override
		public ArrayList<String> makeLines() {
			addEnum();
			if (wantToJNIConverter)
				addToJNIConverter();
			if (wantFromJNIConverter)
				addFromJNIConverter();
			if (wantStreamInsertion)
				addStreamInsertion();
			return lines;
		}

		private void addEnum() {
			add(indent(0).append("enum ").append(getEnumCPPTypeName()));
			add(indent(0).append('{'));
			for (F enumerator : getEnumerators()) {
				add(indent(1).append(enumerator.name()).append(','));
			}
			add(indent(0).append("};"));
			add("");
		}

		private static void addToJNIConverter() {
			throw new RuntimeException("Not implemented");
		}

		private void addFromJNIConverter() {
			addFromJNISignature(true);
			add("");
		}

		private void addStreamInsertion() {
			StringBuilder line = indent(0);
			addOstreamInsertionSignature(true);
			add(line);
			lines.add("");
		}
	}

	public static final class Source<F extends Enum<F>> extends
			EnumLineEditor<F> {
		public Source(Class<F> enumType, boolean wantToJNIConverter,
				boolean wantFromJNIConverter, boolean wantStreamInsertion) {
			super(enumType, wantToJNIConverter, wantFromJNIConverter,
					wantStreamInsertion);
		}

		@Override
		public ArrayList<String> makeLines() {
			if (wantToJNIConverter)
				addToJNIConverter();
			if (wantFromJNIConverter)
				addFromJNIConverter();
			if (wantStreamInsertion) {
				addNameArray();
				addStreamInsertion();
			}
			return lines;
		}

		private static void addToJNIConverter() {
			throw new RuntimeException("Not implemented");
		}

		private void addFromJNIConverter() {
			addFromJNISignature(false);
			add(indent(0).append('{'));
			add(indent(1)
					.append("jmethodID method_id = GLOBAL_REFS->java_lang_Enum__m_ordinal();"));
			add(indent(1).append("jint ordinal = ").append(JNIENV_VAR_NAME)
					.append("->CallIntMethod(").append(ENUM_VAR_NAME)
					.append(", method_id);"));
			add(indent(1).append(JNIEXCEPTION_CHECK_MACRO).append('(')
					.append(JNIENV_VAR_NAME).append(");"));
			addOrdinalAssertion("ordinal");
			add(indent(1).append("return static_cast<")
					.append(getEnumCPPTypeName()).append(">(ordinal);"));
			add(indent(0).append('}'));
			add("");
		}

		private void addNameArray() {
			StringBuilder line = indent(0);
			line.append("static const char * const ");
			line.append(getNameArrayName()).append("[] =");
			add(line);
			add(indent(0).append('{'));
			for (F enumerator : getEnumerators()) {
				add(indent(1).append('"').append(enumerator.name()).append('"')
						.append(','));
			}
			add(indent(0).append("};"));
			add("");
		}

		private void addStreamInsertion() {
			addOstreamInsertionSignature(false);
			add(indent(0).append('{'));
			addOrdinalAssertion(ENUM_VAR_NAME);
			add(indent(1).append("o << ").append(getNameArrayName())
					.append('[').append(ENUM_VAR_NAME).append(']'));
			add(indent(1).append("  << ")
					.append("'<' << ").append("static_cast<int>(")
					.append(ENUM_VAR_NAME).append(") << '>';"));
			add(indent(1).append("return o;"));
			add(indent(0).append('}'));
			add("");
		}
	}
}
