/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import suneido.SuDate;
import suneido.SuException;
import suneido.SuObject;
import suneido.runtime.BlockFlowException;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Dnum;
import suneido.util.Errlog;

public class Dir {
	private static final int MAXFILES = 10000;

	@Params("path = '*', files = false, details = false, block = false")
	public static Object Dir(Object p, Object f, Object d, Object block) {
		boolean files = Ops.toBoolean_(f);
		boolean details = Ops.toBoolean_(d);
		String dir = toStr(p).replace('\\',  '/');
		String glob;
		int i = dir.lastIndexOf('/');
		if (i == -1) {
			glob = dir;
			dir = ".";
		} else {
			glob = dir.substring(i + 1);
			dir = dir.substring(0, i +1);
		}
		if (glob.endsWith("*.*")) // *.* only matches if there is a literal '.'
			glob = glob.substring(0, glob.length() - 2);
		Path path = FileSystems.getDefault().getPath(dir); // Path.of for 11
		SuObject ob = (block == Boolean.FALSE) ? new SuObject() : null;
		try (var ds = Files.newDirectoryStream(path, glob)) {
			for (var x : ds) {
				File file = x.toFile();
				if (files && file.isDirectory())
					continue;
				Object value = details ? detailsOf(file) : nameOf(file);
				if (ob != null) {
					ob.add(value);
					if (ob.size() > MAXFILES)
						throw new SuException("Dir: too many files (>" + MAXFILES + ")");
				} else
					try {
					Ops.call(block, value);
					} catch (BlockFlowException e) {
						if (e == BlockFlowException.BREAK_EXCEPTION)
							break;
						// else continue
					}
			}
		} catch (IOException e) {
			if (! (e instanceof NoSuchFileException))
				Errlog.info("Dir failed: " + e);
			return ob == null ? null : new SuObject();
		}
		return ob;
	}

	private static String nameOf(File f) {
		String s = f.getName();
		if (f.isDirectory())
			s += "/";
		return s;
	}

	private static SuObject detailsOf(File f) {
		SuObject ob = new SuObject();
		ob.put("name", nameOf(f));
		ob.put("size", Dnum.from(f.length()));
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

}
