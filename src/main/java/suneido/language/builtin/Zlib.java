/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import suneido.SuException;
import suneido.language.BuiltinClass;
import suneido.language.Ops;
import suneido.language.Params;
import suneido.util.Util;

public class Zlib extends BuiltinClass {
	public static final Zlib singleton = new Zlib();

	private Zlib() {
		super(Zlib.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of Zlib");
	}

	@Params("string")
	public static Object Compress(Object self, Object a) {
		byte[] data = Util.stringToBytes(Ops.toStr(a));
		Deflater deflater = new Deflater();
		deflater.setInput(data);
		deflater.finish();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated code... index
			outputStream.write(buffer, 0, count);
		}
		deflater.end();
		return Util.bytesToString(outputStream.toByteArray());
	}

	@Params("string")
	public static Object Uncompress(Object self, Object a) {
		byte[] data = Util.stringToBytes(Ops.toStr(a));
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count;
			try {
				count = inflater.inflate(buffer);
			} catch (DataFormatException e) {
				throw new RuntimeException("Zlib Compress error", e);
			}
			outputStream.write(buffer, 0, count);
		}
		inflater.end();
		return Util.bytesToString(outputStream.toByteArray());
	}

}
