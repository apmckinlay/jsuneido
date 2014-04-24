/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.net.URISyntaxException;
import java.net.URL;

public class JarPath {

	public static String jarPath() {
		URL url = JarPath.class.getProtectionDomain().getCodeSource().getLocation();
		String path = null;
		try // try to use URI.getPath() to get unescaped reserved chars (URL.getPath() escapes them).
			{ path = url.toURI().getPath(); }
		catch (URISyntaxException e)
			{ path = url.getPath(); }
		if (path.matches("/[a-zA-Z]:.*"))
			path = path.substring(1);
		return path;
	}

	public static void main(String[] args) {
		System.out.println(jarPath());
	}

}
