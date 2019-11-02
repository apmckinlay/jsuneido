/* Copyright 2019 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import com.google.common.base.Splitter;

import suneido.SuObject;

public class OSVersion {
    private static class Lazy {
        private static final SuObject instance = osVersion();

        private static SuObject osVersion() {
    		SuObject ob = new SuObject();
    		String s = System.getProperty("os.version");
    		for (var v : Splitter.on('.').split(s)) {
    			int n = Integer.parseInt(v);
    			ob.add(n);
    		}
    		return ob;
    	}
    }


	public static SuObject OSVersion() {
		return Lazy.instance;
	}

}
