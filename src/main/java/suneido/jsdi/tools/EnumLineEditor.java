/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.tools;

import java.util.ArrayList;

/**
 * Contains {@link LineEditor} classes for automatically generating C++ code to
 * support Java {@code enum}'s via JNI. The {@link EnumLineEditor.Header} class
 * generates lines for the {@code .h} file, while the
 * {@link EnumLineEditor.Source} class generates lines for the {@code .cpp}
 * file.
 * 
 * @author Victor Schappert
 * @since 20130701
 * 
 * @param <E>
 *            The Java enumeration to generate C++ code for.
 */
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

	protected final String getEnumJavaTypeName() {
		return enumType.getCanonicalName();
	}

	protected final String getEnumCPPTypeName() {
		return getEnumJavaTypeName().replace('.', '_');
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
	protected static final String ORDINAL_TO_CPP_FUNC_NAME = "ordinal_enum_to_cpp";

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
		sig.append(", jobject");
		if (!header)
			sig.append(' ').append(ENUM_VAR_NAME);
		sig.append(')');
		if (header)
			sig.append(';');
		add(sig);
	}

	protected final void addToJNISignature(boolean header) {
		add(indent(0).append("template <>"));
		StringBuilder sig = indent(0);
		sig.append("jobject cpp_to_jni_enum(JNIEnv *");
		if (!header)
			sig.append(' ').append(JNIENV_VAR_NAME);
		sig.append(", ").append(getEnumCPPTypeName());
		if (!header)
			sig.append(' ').append(ENUM_VAR_NAME);
		sig.append(')');
		if (header)
			sig.append(';');
		add(sig);
	}

	protected final void addFromOrdinalSignature(boolean header) {
		add(indent(0).append("template <>"));
		StringBuilder sig = indent(0);
		sig.append(getEnumCPPTypeName());
		sig.append(' ').append(ORDINAL_TO_CPP_FUNC_NAME).append("(int");
		if (!header)
			sig.append(' ').append(ENUM_VAR_NAME);
		sig.append(')');
		if (header)
			sig.append(';');
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

	protected final void addOrdinalCheck(String varName) {
		add(indent(1).append("if (! (0 <= ").append(varName)
				.append(" && ").append(ENUM_VAR_NAME).append(" < ")
				.append(getOrdinalLimit()).append("))"));
		add(indent(1).append('{'));
		add(indent(2).append("throw_out_of_range("));
		add(indent(3).append("__FUNCTION__,"));
		add(indent(3).append(varName + ','));
		add(indent(3).append('"').append(getEnumJavaTypeName()).append('"'));
		add(indent(2).append(");"));
		add(indent(1).append('}'));
	}

	//
	// INTERNAL CLASSES
	//

	/**
	 * Generates C++ header file lines to support the Java enum {@code F}.
	 * 
	 * @author Victor Schappert
	 * @since 20130701
	 * @see EnumLineEditor.Source
	 * 
	 * @param <F>
	 *            The Java enum to generate C++ code for.
	 */
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
			if (wantFromJNIConverter) {
				addFromJNIConverter();
				addFromOrdinalConverter();
			}
			if (wantStreamInsertion)
				addStreamInsertion();
			return lines;
		}

		private void addEnum() {
			// Doxygen documentation
			add(indent(0).append("/**"));
			add(indent(0)
					.append(" * \\brief C++ enumeration corresponding to the Java enumeration ")
					.append(doxygenCode(enumType)).append('.'));
			add(indent(0).append(" * \\author ").append(
					getGeneratorClass().getSimpleName()));
			add(indent(0).append(" *"));
			add(indent(0).append(" * Auto-generated by ")
					.append(doxygenCode(getGeneratorClass())).append('.'));
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

		private void addToJNIConverter() {
			// Suppress Doxygen documentation
			add(indent(0).append("/** \\cond internal */"));
			// Actual function declaration
			addToJNISignature(true);
			// End suppress Doxygen documentation
			add(indent(0).append("/** \\endcond */"));
			add("");
		}

		private void addFromJNIConverter() {
			// Suppress Doxygen documentation
			add(indent(0).append("/** \\cond internal */"));
			// Actual function declaration
			addFromJNISignature(true);
			// End suppress Doxygen documentation
			add(indent(0).append("/** \\endcond */"));
			add("");
		}

		private void addFromOrdinalConverter() {
			// Suppress Doxygen documentation
			add(indent(0).append("/** \\cond internal */"));
			// Actual enum declaration
			addFromOrdinalSignature(true);
			// End suppress Doxygen documentation
			add(indent(0).append("/** \\endcond */"));
			add("");
		}

		private void addStreamInsertion() {
			// Doxygen documentation
			add(indent(0).append("/**"));
			add(indent(0)
					.append(" * \\brief Stream insertion operator for \\link ")
					.append(getEnumCPPTypeName()).append("\\endlink."));
			add(indent(0).append(" * \\author ").append(
					getGeneratorClass().getSimpleName()));
			add(indent(0).append(" * \\param o Stream to insert into"));
			add(indent(0).append(" * \\param ").append(ENUM_VAR_NAME)
					.append(" Enumerator to insert"));
			add(indent(0).append(" *"));
			add(indent(0).append(" * Auto-generated by ")
					.append(doxygenCode(getGeneratorClass())).append('.'));
			add(indent(0).append(" */"));

			// Function declaration
			addOstreamInsertionSignature(true);
			lines.add("");
		}
	}

	/**
	 * Generates C++ source ({@code .cpp}) file lines to support the Java enum
	 * {@code F}.
	 * 
	 * @author Victor Schappert
	 * @since 20130701
	 * @see EnumLineEditor.Header
	 * 
	 * @param <F>
	 *            The Java enum to generate C++ code for.
	 */
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
			if (wantFromJNIConverter) {
				addFromJNIConverter();
				addFromOrdinalConverter();
			}
			if (wantStreamInsertion) {
				addNameArray();
				addStreamInsertion();
			}
			return lines;
		}

		private void addToJNIConverter() {
			// Note for this to work, the specific enum type's values() static
			// method has to be added to the global references
			addToJNISignature(false);
			add(indent(0).append('{'));
			add(indent(1)
					.append("jmethodID method_id = GLOBAL_REFS->" +
							getEnumCPPTypeName() + "__m_values();"));
			add(indent(1).append("jobjectArray values = static_cast<jobjectArray>(")
					.append(JNIENV_VAR_NAME)
					.append("->CallStaticObjectMethod(GLOBAL_REFS->")
					.append(getEnumCPPTypeName() + "(), method_id));"));
			add(indent(1).append(JNIEXCEPTION_CHECK_MACRO).append('(')
					.append(JNIENV_VAR_NAME).append(");"));
			add(indent(1).append("assert(values || !\"got null from JNI\");"));
			add(indent(1)
					.append("jobject result = ")
					.append(JNIENV_VAR_NAME)
					.append("->GetObjectArrayElement(values, static_cast<jsize>(")
					.append(ENUM_VAR_NAME).append("));"));
			add(indent(1).append(JNIEXCEPTION_CHECK_MACRO).append('(')
					.append(JNIENV_VAR_NAME).append(");"));
			add(indent(1).append("assert(result || !\"got null from JNI\");"));
			add(indent(1).append(JNIENV_VAR_NAME).append(
					"->DeleteLocalRef(values);"));
			add(indent(1).append("return result;"));
			add(indent(0).append('}'));
			add("");
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
			add(indent(1).append("return ").append(ORDINAL_TO_CPP_FUNC_NAME)
					.append('<').append(getEnumCPPTypeName())
					.append(">(ordinal);"));
			add(indent(0).append('}'));
			add("");
		}

		private void addFromOrdinalConverter() {
			addFromOrdinalSignature(false);
			add(indent(0).append('{'));
			addOrdinalCheck(ENUM_VAR_NAME);
			add(indent(1).append("return static_cast<")
					.append(getEnumCPPTypeName()).append(">(")
					.append(ENUM_VAR_NAME).append(");"));
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
			addOrdinalCheck(ENUM_VAR_NAME);
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
