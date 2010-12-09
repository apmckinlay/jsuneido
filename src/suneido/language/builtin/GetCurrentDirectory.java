package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.language.SuFunction0;

public class GetCurrentDirectory extends SuFunction0 {

	@Override
	public Object call0() {
		try {
			return new File(".").getCanonicalPath();
		} catch (IOException e) {
			throw new SuException("GetCurrentDirectory", e);
		}
	}

}
