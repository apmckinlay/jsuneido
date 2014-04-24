/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.suneido_protocol;

import suneido.SuException;
import suneido.Suneido;
import suneido.language.Ops;

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
	 * function behaves as if the relevant excception was thrown (see below).
	 * </li>
	 * <li>
	 * If the Suneido language object throws an exception, this function
	 * returns a UTF-8 string with the following format:
	 * "{@code fn(url) => error}", where {@code fn} is the Suneido language
	 * function name, {@code url} is the argument to this function, and
	 * {@code error} is the exception message.
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
	public static String start(String url) {
		// NOTE: At the moment returning a String is fine, but we have to keep
		//       in mind that the data returned to the web browser may be binary
		//       data, e.g. a PNG image. This raises two problems. First, it's
		//       inefficient to constantly be "packing" 1 byte of binary data
		//       into two-byte Java UTF-16 characters. Second, it means the
		//       consumer of the return value--the JSDI layer--must assume that
		//       it can always eliminate the high-order byte of the characters
		//       in the string. In other words, it is impossible to actually
		//       return a UTF-16 string because the JSDI layer will always kill
		//       the high-order byte.
		try {
			final Object object = Suneido.context.get(CALLABLE_NAME);
			final Object result = Ops.call(object, url);
			return null == result || Boolean.FALSE == result ? null : Ops.toStr(result);
		} catch (SuException e) {
			return Ops.toStr(CALLABLE_NAME + '(' + url + ") => " + e.getMessage());
		}
	}
}
