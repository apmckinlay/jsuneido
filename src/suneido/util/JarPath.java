package suneido.util;

import java.net.URL;

public class JarPath {

	public static String jarPath() {
		URL uri = JarPath.class.getProtectionDomain().getCodeSource().getLocation();
		String path = uri.getPath();
		if (path.matches("/[a-zA-Z]:.*"))
			path = path.substring(1);
		return path;
	}

	public static void main(String[] args) {
		System.out.println(jarPath());
	}

}
