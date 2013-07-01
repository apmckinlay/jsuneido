package suneido.language.jsdi.tools;

import java.util.ArrayList;

// TODO: Docs
abstract class EnumLineEditor<E extends Enum<E>> extends LineEditor {

	//
	// DATA
	//

	protected final Class<E> enumType;
	private final Class<?> generatorClass;
	protected final boolean wantToJNIConverter;
	protected final boolean wantFromJNIConverter;
	protected final boolean wantStreamInsertion;

	//
	// CONSTRUCTORS
	//

	public EnumLineEditor(Class<E> enumType, Class<?> generatorClass,
			boolean wantToJNIConverter, boolean wantFromJNIConverter,
			boolean wantStreamInsertion) {
		super("GENERATED ENUM " + enumType.getCanonicalName());
		this.enumType = enumType;
		this.generatorClass = generatorClass;
		this.wantToJNIConverter = wantToJNIConverter;
		this.wantFromJNIConverter = wantFromJNIConverter;
		this.wantStreamInsertion = wantStreamInsertion;
	}

	//
	// STATIC HELPERS
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

	protected final Class<?> getGeneratorClass() {
		return null == generatorClass ? getClass() : generatorClass;
	}

	protected static final String JNIENV_VAR_NAME = "env";
	protected static final String JCLASS_VAR_NAME = "clazz";
	protected static final String ENUM_VAR_NAME = "e";
	protected static final String JNIEXCEPTION_CHECK_MACRO = "JNI_EXCEPTION_CHECK";

	//
	// HELPERS
	//

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
		sig.append("std::ostream& operator<<(std::ostream& o")
				.append(", const ").append(getEnumCPPTypeName()).append("& ")
				.append(ENUM_VAR_NAME).append(')');
		if (header)
			sig.append(';');
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
		public Header(Class<F> enumType, Class<?> generatorClass,
				boolean wantToJNIConverter, boolean wantFromJNIConverter,
				boolean wantStreamInsertion) {
			super(enumType, generatorClass, wantToJNIConverter,
					wantFromJNIConverter, wantStreamInsertion);
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
			// Doxygen documentation
			add(indent(0).append("/**"));
			add(indent(0)
					.append(" * \\brief C++ enumeration corresponding to the Java enumeration ")
					.append(doxygenDfn(enumType)).append('.'));
			add(indent(0).append(" * \\author ").append(
					getGeneratorClass().getSimpleName()));
			add(indent(0).append(" *"));
			add(indent(0).append(" * Auto-generated by ")
					.append(doxygenDfn(getGeneratorClass())).append('.'));
			add(indent(0).append(" */"));
			// Actual enum declaration
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
			// Suppress Doxygen documentation
			add(indent(0).append("/** \\cond internal */"));
			// Actual enum declaration
			addFromJNISignature(true);
			// End suppress Doxygen documentation
			add(indent(0).append("/** \\endcond */"));
			add("");
		}

		private void addStreamInsertion() {
			// Doxygen documentation
			add(indent(0).append("/**"));
			add(indent(0).append(" * \\brief Stream insertion operator for \\link ")
					.append(getEnumCPPTypeName()).append("\\endlink."));
			add(indent(0).append(" * \\author ").append(
					getGeneratorClass().getSimpleName()));
			add(indent(0).append(" * \\param o Stream to insert into"));
			add(indent(0).append(" * \\param ").append(ENUM_VAR_NAME)
					.append(" Enumerator to insert"));
			add(indent(0).append(" *"));
			add(indent(0).append(" * Auto-generated by ")
					.append(doxygenDfn(getGeneratorClass())).append('.'));
			add(indent(0).append(" */"));

			// Function declaration
			addOstreamInsertionSignature(true);
			lines.add("");
		}
	}

	public static final class Source<F extends Enum<F>> extends
			EnumLineEditor<F> {
		public Source(Class<F> enumType, Class<?> generatorClass,
				boolean wantToJNIConverter, boolean wantFromJNIConverter,
				boolean wantStreamInsertion) {
			super(enumType, generatorClass, wantToJNIConverter,
					wantFromJNIConverter, wantStreamInsertion);
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
					.append("jmethodID method_id = global_refs::ptr->java_lang_Enum__m_ordinal();"));
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
			add(indent(1).append("  << ").append("'<' << ")
					.append("static_cast<int>(").append(ENUM_VAR_NAME)
					.append(") << '>';"));
			add(indent(1).append("return o;"));
			add(indent(0).append('}'));
			add("");
		}
	}
}
