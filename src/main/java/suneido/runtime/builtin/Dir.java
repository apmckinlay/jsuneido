/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;
import java.io.FileFilter;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Dir {

	@Params("path = '*.*', files = false, details = false")
	public static SuContainer Dir(Object p, Object f, Object d) {
		boolean files = Ops.toBoolean_(f);
		boolean details = Ops.toBoolean_(d);
		File path = new File(toStr(p));
		String dir = path.getParent();
		if (dir == null)
			dir = ".";
		String pattern = path.getName();
		FileFilter filter = new Filter(pattern);
		SuContainer ob = new SuContainer();
		File[] listFiles = new File(dir).listFiles(filter);
		if (listFiles != null)
			for (File file : listFiles)
				if (! files || ! file.isDirectory())
					ob.add(details ? detailsOf(file) : nameOf(file));
		return ob;
	}

	private static String nameOf(File f) {
		String s = f.getName();
		if (f.isDirectory())
			s += "/";
		return s;
	}

	private static SuContainer detailsOf(File f) {
		SuContainer ob = new SuContainer();
		ob.put("name", nameOf(f));
		ob.put("size", f.length() < Integer.MAX_VALUE ? (int) f.length()
				: BigDecimal.valueOf(f.length()));
		ob.put("date", SuDate.fromTime(f.lastModified()));
		ob.put("attr", attrOf(f));
		return ob;
	}

	private static final int READONLY = 1;
	private static final int HIDDEN = 2;
	private static final int DIRECTORY = 16;

	private static int attrOf(File f) {
		return (f.canWrite() ? 0 : READONLY)
				| (f.isHidden() ? HIDDEN : 0)
				| (f.isDirectory() ? DIRECTORY : 0);
	}

	private static class Filter implements FileFilter {
		private final Pattern pattern;

		Filter(String s) {
			pattern = Pattern.compile(convert(s), Pattern.CASE_INSENSITIVE);
		}

		private static String convert(String s) {
			return s.replace("*.*", "*") // for compatibility with windows
					.replace(".", "\\.").replace("?", ".").replace("*", ".*");
		}

		@Override
		public boolean accept(File f) {
			return pattern.matcher(f.getName()).matches();
		}

	}

}
