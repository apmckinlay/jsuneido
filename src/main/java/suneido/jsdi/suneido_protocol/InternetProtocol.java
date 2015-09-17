/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.suneido_protocol;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import suneido.SuContainer;
import suneido.SuException;
import suneido.Suneido;
import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.runtime.Except;
import suneido.runtime.Ops;
import suneido.util.Errlog;

/**
 * <p>
 * Handles <code>suneido://</code> protocol requests by passing them on to the
 * Suneido-language <code>SuneidoAPP</code> function, if it exists.
 * </p>
 *
 * <p>
 * The architecture is as follows.
 * <ul>
 * <li>
 * When it is loaded, the JSDI library registers an instance of
 * <code>IInternetProtocol</code> to handle URLs beginning with the
 * <code>suneido://</code> protocol.
 * </li>
 * <li>
 * Suneido code may create an embedded Microsoft browser (Internet Explorer),
 * for example by using the Active Template Library (ATL) support for
 * embedding ActiveX controls in regular windows.
 * </li>
 * <li>
 * If the embedded Internet Explorer encounters the <code>suneido://</code>
 * protocol in a URL, it will invoke
 * <a href="http://msdn.microsoft.com/en-us/library/aa914217.aspx">
 * <code>IInternetRootProtocol::start(...)</code></a> on the registered
 * <code>IInternetProtocol</code> instance (in the JSDI library layer).
 * </li>
 * <li>
 * This will cause the JSDI library to call into Java and invoke the
 * {@link #start(String)} method, passing in the URL to load.
 * </li>
 * <li>
 * The {@link #start(String)} will then attempt to invoke a Suneido-language
 * function, passing it the URL to load.
 * </li>
 * </ul>
 * </p>
 * <p> This process allows Suneido code to load the data associated with
 * <code>suneido://</code> URLs.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140210
 */
@DllInterface
public final class InternetProtocol {

	/** Name of the callable object that #start(String) should invoke. */
	private static final String CALLABLE_NAME = "SuneidoAPP";

	/**
	 * <p>
	 * Invoked from JSDI layer.
	 * </p>
	 * <p>
	 * This function never throws an exception. The return value is one of the
	 * following:
	 * <ul>
	 * <li>
	 * If the Suneido language object does not exist, or is not callable, this
	 * function behaves as if the relevant exception was thrown (see below).
	 * </li>
	 * <li>
	 * If the Suneido language object throws an exception, this function
	 * returns a UTF-8 string containing a simple XHTML page with the stack
	 * trace of the error message.
	 * </li>
	 * <li>
	 * If the Suneido language object returns {@code false} or has no return
	 * value at all, this function returns {@code null}.
	 * </li>
	 * <li>
	 * Otherwise, this function returns the stringization of the value returned
	 * by calling the Suneido language object.
	 * </li>
	 * </ul>
	 *
	 * </p>
	 *
	 * @param url Resource that the Suneido code should load.
	 * @return Data suitable for display in a web browser.
	 */
	public static byte[] start(String url) {
		try {
			final Object object = Suneido.context.get(CALLABLE_NAME);
			final Object result = Ops.call(object, url);
			if (null == result || Boolean.FALSE == result)
				return null;
			else if (result instanceof Buffer) {
				return ((Buffer)result).getInternalData();
			} else {
				final String str = Ops.toStr(result);
				final Buffer buf = new Buffer(str.length(), str);
				return buf.getInternalData();
			}
		} catch (SuException e) {
			return makeStackTraceHTMLPage(new Except(e));
		}
	}

	private static byte[] makeStackTraceHTMLPage(Except e) {
		// NOTE: This works adequately mainly because Internet Explorer is
		//       pretty good at parsing badly-formed HTML. But because a lot of
		//       characters that could appear in the error message may require
		//       escaping to avoid breaking the HTML parser, we should really be
		//       outputting XHTML and using a proper XML escaping library. 
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os,
				Charset.forName("ISO-8859-1")));
		try {
			addLines(w, HTML_BEFORE);
			addLine(w, "<p><strong>Message:</strong> &quot;" + e.toString() + "&quot;</p>");
			addLines(w, HTML_MIDDLE);
			final SuContainer callstack = (SuContainer)Except.Callstack(e);
			final int N = callstack.vecSize();
			for (int k = 0; k < N; ++k) {
				SuContainer callob = (SuContainer)callstack.get(k);
				addLine(w, callob.get("fn").toString());
			}
			addLines(w, HTML_AFTER);
			w.close();
		} catch (Exception e2) {
			Errlog.error("Can't make HTML stack trace", e2);
		}
		return os.toByteArray();
	}

	private static void addLine(BufferedWriter w, String line)
			throws IOException {
		w.append(line);
		w.newLine();
	}

	private static void addLines(BufferedWriter w, String[] lines)
			throws IOException {
		for (String line : lines) {
			addLine(w, line);
		}
	}

	private static final String[] HTML_BEFORE = new String[] {
		"<html>",
		"",
		"<head>",
		"<title>Exception invoking '" + CALLABLE_NAME + "'</title>",
		"</head>",
		"",
		"<body>",
		"<p>An exception occurred invoking the Suneido callable <code>" + CALLABLE_NAME + "</code>:</p>",
	};
	private static final String[] HTML_MIDDLE = new String[] {
		"<p><strong>Stack Trace:</strong></p>",
		"<pre>"
	};
	private static final String[] HTML_AFTER = new String[] {
		"</pre>",
		"</body>",
		"",
		"</html>"
	};
}
