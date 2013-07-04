package suneido.language.jsdi.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.Files;

/**
 * Injects generated source code lines into a file. The {@link LineEditor}
 * supplied on construction determines the insertion point (via
 * {@link LineEditor#getMatchToken()}) and the content (via
 * {@link LineEditor#makeLines()}). In order to prevent the source code version
 * control system, if any, from committing useless changes, this class does not
 * actually modify the given file unless the content of the generated lines has
 * changed.
 * 
 * @author Victor Schappert
 * @since 20130628
 */
class FileEditor {
	private final File file;
	private final LineEditor lineEditor;
	private final ArrayList<String> before, during, after;
	private String beginPrefix;
	private String afterLine;

	/**
	 * Constructs an editor to edit a given file.
	 * 
	 * @param file
	 *            Target file (must exist and be readable/writeable).
	 * @param lineEditor
	 *            Line editor specifying the location and content of the edit.
	 */
	public FileEditor(File file, LineEditor lineEditor) {
		this.file = file;
		this.lineEditor = lineEditor;
		this.before = new ArrayList<String>();
		this.during = new ArrayList<String>();
		this.after = new ArrayList<String>();
		this.beginPrefix = null;
		this.afterLine = null;
	}

	/**
	 * Edits the target file.
	 */
	public void edit() throws Exception {
		readFile();
		if (null == beginPrefix) {
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
			Pattern BEGIN = Pattern.compile("(.*\\s)\\[BEGIN:"
					+ lineEditor.getMatchToken() + "[^\\]]*\\]\\s*");
			while (null != (line = br.readLine())) {
				Matcher m = BEGIN.matcher(line);
				if (m.matches()) {
					beginPrefix = m.group(1);
					break;
				}
				before.add(line);
			}
			// Read lines before the END token.
			Pattern END = Pattern.compile(Pattern.quote(beginPrefix) + "\\[END:"
					+ lineEditor.getMatchToken() + "\\]\\s*");
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

	public void writeFile(ArrayList<String> newDuring) throws Exception {
		File tmp = File.createTempFile(file.getName(), ".tmp");
		FileOutputStream fos = new FileOutputStream(tmp);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		try {
			writeLines(before, bw);
			if (null != beginPrefix) {
				writeLine(
						beginPrefix + "[BEGIN:" + lineEditor.getMatchToken()
								+ " last updated " + new Date() + ']', bw);
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

	private static void writeLines(ArrayList<String> lines, BufferedWriter bw)
			throws Exception {
		for (String line : lines)
			writeLine(line, bw);
	}
}