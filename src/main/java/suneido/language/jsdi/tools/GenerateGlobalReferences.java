/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.tools;

import java.io.File;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;

import org.objectweb.asm.Type;

import suneido.SuValue;
import suneido.language.Numbers;
import suneido.language.jsdi.ThunkManager;
import suneido.language.jsdi.com.COMException;
import suneido.language.jsdi.com.COMobject;
import suneido.language.jsdi.type.Callback;

/**
 * Automatic C++ code generator for the JSDI {@code global_refs} translation
 * unit.
 * 
 * @author Victor Schappert
 * @since 20130630
 */
@SuppressWarnings("rawtypes")
public final class GenerateGlobalReferences {

	private static final class Ref {
		public final Class clazz;
		public final ArrayList<Constructor> constructors;
		public final ArrayList<Method> methods;
		public final ArrayList<Field> fields;

		public Ref(Class clazz, Member... members) {
			this.clazz = clazz;
			this.constructors = new ArrayList<>();
			this.methods = new ArrayList<>();
			this.fields = new ArrayList<>();
			for (Member member : members) {
				if (member instanceof Constructor)
					constructors.add((Constructor) member);
				else if (member instanceof Method)
					methods.add((Method) member);
				else
					fields.add((Field) member);
			}
		}
	}

	private static Ref[] makeRefs() throws Exception {
		return new Ref[] {
				new Ref(Object.class,
						Object.class.getMethod("toString")),
				new Ref(Boolean.class, Boolean.class.getField("TRUE"),
						Boolean.class.getField("FALSE"),
						Boolean.class.getMethod("booleanValue")),
				new Ref(Number.class),
				new Ref(Integer.class,
						Integer.class.getConstructor(Integer.TYPE),
						Integer.class.getMethod("intValue")),
				new Ref(Long.class,
						Long.class.getConstructor(Long.TYPE),
						Long.class.getMethod("longValue")),
				new Ref(BigDecimal.class,
						// NOTE: The order of the members matters because they
						//       are just getting auto-generated suffixes i.e.
						//       __init, __init1, __init2...
						BigDecimal.class.getConstructor(Double.TYPE, MathContext.class),
						BigDecimal.class.getMethod("doubleValue")),
				new Ref(CharSequence.class),
				new Ref(Enum.class, Enum.class.getMethod("ordinal")),
				new Ref(byte[].class),
				new Ref(Date.class,
						Date.class.getConstructor(Long.TYPE),
						Date.class.getMethod("getTime")),
				new Ref(ThunkManager.class,
						ThunkManager.class.getField("THUNK_FUNC_ADDR_INDEX"),
						ThunkManager.class.getField("THUNK_OBJECT_ADDR_INDEX")),
				new Ref(Callback.class, Callback.class.getMethod("invoke",
						SuValue.class, byte[].class),
						Callback.class.getMethod("invokeVariableIndirect",
								SuValue.class, byte[].class, Object[].class)),
				new Ref(COMobject.class,
						COMobject.class.getDeclaredConstructor(
							String.class, Long.TYPE, Boolean.TYPE),
						COMobject.class.getDeclaredField("ptr"),
						COMobject.class.getDeclaredMethod("isDispatch"),
						COMobject.class.getDeclaredMethod("verifyNotReleased")),
						// NOTE: getDeclaredConstructor(), unlike
						//       getConstructor(), can return non-public
						//       constructors...
				new Ref(COMException.class,
						COMException.class.getConstructor(String.class)),
				new Ref(Numbers.class,
						Numbers.class.getField("MC"),
						Numbers.class.getMethod("narrow", Number.class))
		};
	}

	private static String className(Class clazz) {
		String name = clazz.getCanonicalName();
		return name.replace("[]", "_ARRAY").replace('.', '_');
	}

	private static String methodName(Method method) {
		return className(method.getDeclaringClass()) + "__m_"
				+ method.getName();
	}

	private static String constructorName(Constructor constructor,
			int overloadIndex) {
		String result = className(constructor.getDeclaringClass()) + "__init";
		if (0 < overloadIndex) {
			result += overloadIndex;
		}
		return result;
	}

	private static String fieldName(Field field) {
		return className(field.getDeclaringClass()) + "__f_" + field.getName();
	}

	private static final class HeaderLineEditor extends LineEditor {
		private final Ref[] refs;

		public HeaderLineEditor(Ref[] refs) {
			super("GENERATED CODE");
			this.refs = refs;
		}

		public final ArrayList<String> makeLines() {
			for (Ref ref : refs) {
				addClassMember(ref.clazz);
				int k = 0;
				for (Constructor constructor : ref.constructors)
					addConstructorMember(constructor, k++);
				for (Method method : ref.methods)
					addMethodMember(method);
				for (Field field : ref.fields)
					addFieldMember(field);
			}
			return lines;
		}

		private static String getterFuncName(String type, String name) {
			return type + ' ' + name + "() const";
		}

		private void addClassMember(Class<?> clazz) {
			add("jclass", className(clazz),
					"Returns a global reference to the class "
							+ doxygenDfn(clazz) + '.', doxygenDfn(clazz), null);
		}

		private void addConstructorMember(Constructor constructor,
				int overloadIndex) {
			add("jmethodID",
					constructorName(constructor, overloadIndex),
					"Returns a global reference to the constructor "
							+ doxygenDfn(constructor.toString()) + '.',
					doxygenDfn(constructor.toString()),
					getterFuncName("jclass",
							className(constructor.getDeclaringClass())));
		}

		private void addMethodMember(Method method) {
			CharSequence doxygenReturn = doxygenDfn(method.toString());
			StringBuilder doxygenBrief = new StringBuilder(
					"Returns a global reference to the ");
			if (Modifier.isStatic(method.getModifiers()))
				doxygenBrief.append("static");
			else
				doxygenBrief.append("instance");
			doxygenBrief.append(" method ").append(doxygenReturn).append('.');
			String doxygenSee = getterFuncName("jclass",
					className(method.getDeclaringClass()));
			add("jmethodID", methodName(method), doxygenBrief, doxygenReturn,
					doxygenSee);
		}

		private void addFieldMember(Field field) {
			CharSequence doxygenReturn = doxygenDfn(field.toString());
			StringBuilder doxygenBrief = new StringBuilder(
					"Returns a global reference to the ");
			if (Modifier.isStatic(field.getModifiers()))
				doxygenBrief.append("static");
			else
				doxygenBrief.append("instance");
			doxygenBrief.append(" field ").append(doxygenReturn).append('.');
			String doxygenSee = getterFuncName("jclass",
					className(field.getDeclaringClass()));
			add("jfieldID", fieldName(field), doxygenBrief, doxygenReturn,
					doxygenSee);
		}

		private void add(String type, String name, CharSequence doxygenBrief,
				CharSequence doxygenReturn, CharSequence doxygenSee) {
			add(indent(1).append("private:"));
			add(indent(2).append(type).append(' ').append(name).append("_;"));
			add(indent(1).append("public:"));
			add(indent(2).append(getterFuncName(type, name)));
			add(indent(2).append("{ return ").append(name).append("_; }"));
			addDoxygen(doxygenBrief, doxygenReturn, doxygenSee);
		}

		private void addDoxygen(CharSequence doxygenBrief,
				CharSequence doxygenReturn, CharSequence doxygenSee) {
			add(indent(2).append("/**<"));
			add(indent(2).append(" * \\brief ").append(doxygenBrief));
			add(indent(2).append(" * \\return ").append(doxygenReturn));
			if (null != doxygenSee)
				add(indent(2).append(" * \\see ").append(doxygenSee));
			add(indent(2).append(" *"));
			add(indent(2).append(" * Auto-generated by ")
					.append(doxygenDfn(GenerateGlobalReferences.class))
					.append('.'));
			add(indent(2).append(" */"));
		}
	}

	private static final class SourceLineEditor extends LineEditor {
		private final Ref[] refs;

		private static final String MEMBER_PREFIX = "g->";
		private static final String ENV_PARAM = "env";

		public SourceLineEditor(Ref[] refs) {
			super("GENERATED CODE");
			this.refs = refs;
		}

		public final ArrayList<String> makeLines() {
			for (Ref ref : refs) {
				addClassMember(ref.clazz);
				int k = 0;
				for (Constructor constructor : ref.constructors)
					addConstructorMember(constructor, k++);
				for (Method method : ref.methods)
					addMethodMember(method);
				for (Field field : ref.fields)
					addFieldMember(field);
			}
			return lines;
		}

		private void addClassMember(Class clazz) {
			add(className(clazz) + '_', "get_global_class_ref",
					quote(Type.getInternalName(clazz)));
		}

		private void addConstructorMember(Constructor constructor,
				int overloadIndex) {
			add(constructorName(constructor, overloadIndex) + "_",
					"get_method_id",
					MEMBER_PREFIX + className(constructor.getDeclaringClass())
							+ "_", quote("<init>"),
					quote(Type.getConstructorDescriptor(constructor)));
		}

		private void addMethodMember(Method method) {
			String getter_func = Modifier.isStatic(method.getModifiers()) ? "get_static_method_id"
					: "get_method_id";
			add(methodName(method) + '_', getter_func, MEMBER_PREFIX
					+ className(method.getDeclaringClass()) + '_',
					quote(method.getName()),
					quote(Type.getMethodDescriptor(method)));
		}

		private void addFieldMember(Field field) {
			String getter_func = Modifier.isStatic(field.getModifiers()) ? "get_static_field_id"
					: "get_field_id";
			add(fieldName(field) + '_', getter_func, MEMBER_PREFIX
					+ className(field.getDeclaringClass()) + '_',
					quote(field.getName()),
					quote(Type.getDescriptor(field.getType())));
		}

		private void add(String memberName, String func, CharSequence... args) {
			StringBuilder line = indent(1);
			line.append(MEMBER_PREFIX);
			line.append(memberName);
			line.append(" = ");
			line.append(func);
			line.append('(');
			line.append(ENV_PARAM);
			for (int k = 0; k < args.length; ++k) {
				line.append(", ");
				line.append(args[k]);
			}
			line.append(");");
			add(line);
		}
	}

	private static void error(String message) {
		System.err.println(message);
		System.exit(1);
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
			Ref[] refs = makeRefs();
			action = "edit header file '" + headerFile + "'";
			new FileEditor(headerFile, new HeaderLineEditor(refs)).edit();
			action = "edit source file '" + sourceFile + "'";
			new FileEditor(sourceFile, new SourceLineEditor(refs)).edit();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			error("Failed to " + action);
		}
	}
}
