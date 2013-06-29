package suneido.language.jsdi.tools;

import java.io.File;
import java.lang.reflect.*;
import java.util.ArrayList;

import org.objectweb.asm.Type;

// TODO: docs
// TODO: exclude from JAR
@SuppressWarnings("rawtypes")
public final class GenerateGlobalReferences {

	private static final class Ref {
		public final Class clazz;
		public final ArrayList<Constructor> constructors;
		public final ArrayList<Method> methods;
		public final ArrayList<Field> fields;

		public Ref(Class clazz, Member... members) {
			this.clazz = clazz;
			this.constructors = new ArrayList<Constructor>();
			this.methods = new ArrayList<Method>();
			this.fields = new ArrayList<Field>();
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
				new Ref(Boolean.class, Boolean.class.getField("TRUE"),
						Boolean.class.getField("FALSE")),
				new Ref(Integer.class,
						Integer.class.getConstructor(Integer.TYPE),
						Integer.class.getMethod("intValue")),
				new Ref(Enum.class, Enum.class.getMethod("ordinal")),
				new Ref(suneido.language.jsdi.type.BasicType.class) };
	}

	private static String className(Class clazz) {
		return clazz.getCanonicalName().replace('.', '_');
	}

	private static String methodName(Method method) {
		return className(method.getDeclaringClass()) + "__m_"
				+ method.getName();
	}

	private static String constructorName(Constructor constructor) {
		return className(constructor.getDeclaringClass()) + "__init";
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
				for (Constructor constructor : ref.constructors)
					addConstructorMember(constructor);
				for (Method method : ref.methods)
					addMethodMember(method);
				for (Field field : ref.fields)
					addFieldMember(field);
			}
			return lines;
		}

		private void addClassMember(Class clazz) {
			add("jclass", className(clazz));
		}

		private void addConstructorMember(Constructor constructor) {
			add("jmethodID", constructorName(constructor));
		}

		private void addMethodMember(Method method) {
			add("jmethodID", methodName(method));
		}

		private void addFieldMember(Field field) {
			add("jfieldID", fieldName(field));
		}

		private void add(String type, String name) {
			add(indent(1).append("private:"));
			add(indent(2).append(type).append(' ').append(name).append("_;"));
			add(indent(1).append("public:"));
			add(indent(2).append(type).append(' ').append(name)
					.append("() const"));
			add(indent(2).append("{ return ").append(name).append("_; }"));
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
				for (Constructor constructor : ref.constructors)
					addConstructorMember(constructor);
				for (Method method : ref.methods)
					addMethodMember(method);
				for (Field field : ref.fields)
					addFieldMember(field);
			}
			return lines;
		}

		private void addClassMember(Class clazz) {
			add(className(clazz) + '_', "get_global_class_ref",
					quote(Type.getDescriptor(clazz)));
		}

		private void addMethodMember(Method method) {
			add(methodName(method) + '_', "get_method_id", MEMBER_PREFIX
					+ className(method.getDeclaringClass()) + '_',
					quote(method.getName()),
					quote(Type.getMethodDescriptor(method)));
		}

		private void addConstructorMember(Constructor constructor) {
			add(constructorName(constructor) + "_", "get_method_id",
					MEMBER_PREFIX + className(constructor.getDeclaringClass())
							+ "_", quote("<init>"),
					quote(Type.getConstructorDescriptor(constructor)));
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
