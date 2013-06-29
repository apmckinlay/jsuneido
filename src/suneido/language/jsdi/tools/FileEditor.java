package suneido.language.jsdi.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.google.common.io.Files;

// TODO: docs
class FileEditor {
	private final File file;
	private final LineEditor lineEditor;
	private final ArrayList<String> before, during, after;
	private String whitespaceBefore;
	private String afterLine;

	public FileEditor(File file, LineEditor lineEditor) {
		this.file = file;
		this.lineEditor = lineEditor;
		this.before = new ArrayList<String>();
		this.during = new ArrayList<String>();
		this.after = new ArrayList<String>();
		this.whitespaceBefore = null;
		this.afterLine = null;
	}

	public void edit() throws Exception {
		readFile();
		if (null == whitespaceBefore) {
			// DIDN'T find the search token, so just stop.
			return;
		}
		ArrayList<String> newDuring = lineEditor.makeLines();
		if (during.equals(newDuring)) {
			// NO CHANGE in the auto-generated lines, so don't touch the
			// file.
			return;
		}
		writeFile(newDuring);
	}

	private void readFile() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);
		try {
			String line;
			// Read lines before the BEGIN token.
			Pattern BEGIN = Pattern.compile("(\\s*)//\\s+\\[BEGIN:"
					+ lineEditor.matchToken + "[^\\]]*\\]\\s*");
			while (null != (line = br.readLine())) {
				Matcher m = BEGIN.matcher(line);
				if (m.matches()) {
					whitespaceBefore = m.group(1);
					break;
				}
				before.add(line);
			}
			// Read lines before the END token.
			Pattern END = Pattern.compile("\\s*//\\s+\\[END:"
					+ lineEditor.matchToken + "\\]\\s*");
			while (null != (line = br.readLine())) {
				Matcher m = END.matcher(line);
				if (m.matches()) {
					afterLine = m.group(0);
					break;
				}
				during.add(line);
			}
			// Read lines after the END token, to end of file.
			while (null != (line = br.readLine())) {
				after.add(line);
			}
		} finally {
			br.close();
		}
	}

	public void writeFile(ArrayList<String> newDuring)
			throws Exception {
		File tmp = File.createTempFile(file.getName(), ".tmp");
		FileOutputStream fos = new FileOutputStream(tmp);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		try {
			writeLines(before, bw);
			if (null != whitespaceBefore) {
				writeLine(whitespaceBefore + "// [BEGIN:"
						+ lineEditor.matchToken + " last updated "
						+ new Date() + ']', bw);
				writeLines(newDuring, bw);
				if (null != afterLine) {
					writeLine(afterLine, bw);
					writeLines(after, bw);
				}
			}
		} finally {
			bw.close();
		}
		Files.move(tmp, file);
	}

	private static void writeLine(String line, BufferedWriter bw)
			throws Exception {
		bw.write(line.toString());
		bw.write(System.getProperty("line.separator"));
	}

	private static void writeLines(ArrayList<String> lines,
			BufferedWriter bw) throws Exception {
		for (String line : lines)
			writeLine(line, bw);
	}
}