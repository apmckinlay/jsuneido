/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

// FIXME: This class has 0 users in the project. Is it being used indirectly,
//        or should it be removed...?
@ThreadSafe
public class Log {
	private static final LinkedList<String> list = new LinkedList<>();
	private static final int MAXSIZE = 100000;
	private static int size = 0;
	private static boolean disabled = false;

	public static synchronized void xadd(String s) {
		if (disabled)
			return;
		if (size >= MAXSIZE)
			list.removeFirst();
		else
			++size;
		list.addLast(s);
	}

	public static synchronized void print() {
		for (String s : list)
			System.out.println(s);
	}

	public static synchronized void save() {
		try (RandomAccessFile f = new RandomAccessFile("log.txt", "rw")) {
			for (String s : list) {
				f.writeBytes(s);
				f.writeBytes("\n");
			}
		} catch (IOException e) {
			throw new SuException("dump error");
		}
	}
}
