package suneido.language.builtin;

import static suneido.language.Ops.toStr;
import static suneido.util.Util.array;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.regex.Pattern;

import suneido.SuContainer;
import suneido.language.*;

public class Dir extends BuiltinFunction {

	private static final FunctionSpec fs =
		new FunctionSpec(array("path", "files", "details"),
				"*.*", Boolean.FALSE, Boolean.FALSE);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		boolean files = args[1] == Boolean.TRUE;
		boolean details = args[2] == Boolean.TRUE;
		File path = new File(toStr(args[0]));
		String dir = path.getParent();
		if (dir == null)
			dir = ".";
		String pattern = path.getName();
		FileFilter filter = new Filter(pattern);
		SuContainer ob = new SuContainer();
		for (File f : new File(dir).listFiles(filter))
			if (! files || ! f.isDirectory())
				ob.append(details ? detailsOf(f) : nameOf(f));
		return ob;
	}

	private String nameOf(File f) {
		String s = f.getName();
		if (f.isDirectory())
			s += "/";
		return s;
	}

	private SuContainer detailsOf(File f) {
		SuContainer ob = new SuContainer();
		ob.put("name", f.getName());
		ob.put("size", f.length());
		ob.put("date", new Date(f.lastModified()));
		ob.put("attr", attrOf(f));
		return ob;
	}

	private static final int READONLY = 1;
	private static final int HIDDEN = 2;
	private static final int DIRECTORY = 16;

	private int attrOf(File f) {
		return (f.canWrite() ? 0 : READONLY)
				| (f.isHidden() ? HIDDEN : 0)
				| (f.isDirectory() ? DIRECTORY : 0);
	}

	private static class Filter implements FileFilter {
		private final Pattern pattern;

		Filter(String s) {
			pattern = Pattern.compile(convert(s), Pattern.CASE_INSENSITIVE);
		}

		private String convert(String s) {
			return s.replace(".", "\\.").replace("?", ".").replace("*", ".*");
		}

		@Override
		public boolean accept(File f) {
			return pattern.matcher(f.getName()).matches();
		}

	}

}