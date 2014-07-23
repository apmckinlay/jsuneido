/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.Suneido.dbpkg;
import static suneido.util.Verify.verify;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import suneido.SuException;
import suneido.intfc.database.Record;

class DumpReader {
	private final InputStream fin;
	private final Processor proc;
	private byte[] recbuf = new byte[100];

	DumpReader(String filename, Processor proc) {
		try {
			fin = new BufferedInputStream(new FileInputStream(filename));
			this.proc = proc;
		} catch (IOException e) {
			throw new SuException("DumpReader", e);
		}
	}

	void process() {
		try {
			checkHeader();
			readSchema();
			readRecords();
			proc.end();
		} catch (IOException e) {
			throw new SuException("DumpReader", e);
		}
	}

	private void checkHeader() throws IOException {
		String s = getline();
		if (!s.startsWith("Suneido dump"))
			throw new SuException("invalid file");
	}

	private void readSchema() throws IOException {
		String s = getline();
		verify(s.startsWith("====== "));
		proc.schema(s.substring(6));
	}

	private void readRecords() throws IOException {
		while (true) {
			int n = getRecordSize();
			if (n == 0)
				break;
			Record rec = getRecord(n);
			proc.record(rec);
		}
	}

	private int getRecordSize() throws IOException {
		byte[] buf = new byte[4];
		verify(fin.read(buf) == buf.length);
		int n = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
		return n;
	}

	Record getRecord(int n) throws IOException {
		if (n > recbuf.length)
			recbuf = new byte[Math.max(n, 2 * recbuf.length)];
		verify(fin.read(recbuf, 0, n) == n);
		return dbpkg.record(ByteBuffer.wrap(recbuf, 0, n));
	}

	private String getline() throws IOException {
		StringBuilder sb = new StringBuilder();
		char c;
		while ('\n' != (c = (char) fin.read()))
			sb.append(c);
		return sb.toString();
	}

	interface Processor {
		void schema(String s);

		void record(Record rec);

		void end();
	}

}
