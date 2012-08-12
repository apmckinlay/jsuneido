package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.language.*;

public class DeleteDir extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.string, args);
		String path = Ops.toStr(args[0]);
		File dir = new File(path);
		if (!dir.isDirectory())
			return false;
		try {
			deleteRecursively(dir);
		} catch (IOException e) {
			throw new SuException("DeleteDir failed", e);
		}
		return true;
	}

	public static void deleteRecursively(File file) throws IOException {
		if (file.isDirectory())
			deleteDirectoryContents(file);
		if (!file.delete())
			throw new IOException("Failed to delete " + file);
	}

	// can't use Guava version because it can fail sometimes
	// when !directory.getCanonicalPath().equals(directory.getAbsolutePath()
	public static void deleteDirectoryContents(File directory)
			throws IOException {
		File[] files = directory.listFiles();
		if (files == null)
			throw new IOException("Error listing files for " + directory);
		for (File file : files)
			deleteRecursively(file);
	}

}
