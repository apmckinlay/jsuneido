/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Ops.toStr;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.Iterators;

import suneido.SuContainer;
import suneido.SuDate;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.runtime.Sequence;
import suneido.util.Dnum;

public class Dir {
	@Params("path = '*', files = false, details = false")
	public static Object Dir(Object p, Object f, Object d) {
		boolean files = Ops.toBoolean_(f);
		boolean details = Ops.toBoolean_(d);
		String dir = toStr(p);
		String glob;
		int i = dir.lastIndexOf('/');
		if (i == -1) {
			glob = dir;
			dir = ".";
		} else {
			glob = dir.substring(i + 1);
			dir = dir.substring(0, i);
		}
		if (glob.endsWith("*.*")) // *.* only matches if there is a literal '.'
			glob = glob.substring(0, glob.length() - 2);
		Path path = FileSystems.getDefault().getPath(dir); // Path.of for 11
		return new Sequence(new SuDir(path, glob, files, details));
	}

	private static class SuDir implements Iterable<Object> {
		final Path dir;
		final String glob;
		final boolean files;
		final boolean details;

		SuDir(Path dir, String glob, boolean files, boolean details) {
			this.dir = dir;
			this.glob = glob;
			this.files = files;
			this.details = details;
		}

		@Override
		public Iterator<Object> iterator() {
			DirectoryStream<Path> ds;
			Iterator<Path> iter;
			try {
				ds = Files.newDirectoryStream(dir, glob);
				iter = ds.iterator();
			} catch (IOException e) {
				return Collections.emptyIterator();
			}
			if (files)
				iter = Iterators.filter(iter,
						(Path p) -> !p.toFile().isDirectory());
			return new Iter(ds, iter);
		}

		class Iter implements Iterator<Object> {
			final DirectoryStream<Path> ds;
			final Iterator<Path> iter;

			Iter(DirectoryStream<Path> ds, Iterator<Path> iter) {
				this.ds = ds;
				this.iter = iter;
			}

			@Override
			public boolean hasNext() {
				if (iter.hasNext())
					return true;
				try {
					ds.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				return false;
			}

			@Override
			public Object next() {
				File file = iter.next().toFile();
				return details ? detailsOf(file) : nameOf(file);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
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
