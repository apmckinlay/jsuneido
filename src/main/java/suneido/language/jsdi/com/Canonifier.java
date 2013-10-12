package suneido.language.jsdi.com;

import suneido.language.jsdi.DllInterface;

/**
 * <p>
 * This class is responsible for marshalling between the canonical types which
 * are used by the JSDI native COM routines and the canonical types which are
 * used in Java to back Suneido types. 
 * </p>
 * <p>
 * The following are the type equivalences.
 * <table cols="2">
 * <tr>
 * <th>JSuneido backing type</th>
 * <th>Used by native COM routines</th>
 * </tr>
 * <tr>
 * <td>{@link Boolean}</td><td>{@link Boolean}</td>
 * </tr>
 * <tr>
 * <td>&lt;Integral number in range of {@link Integer}&gt;</td>
 * <td>{@link Int32}</td>
 * </tr>
 * <tr>
 * <td>&lt;Integral number not in range of {@link Integer} but representable as an unsigned 32-bit integer&gt;</td>
 * <td>{@link Int32}</td>
 * </tr>
 * <tr>
 * <td>&lt;Integral number in range of {@link Long} but not in range of {@link Integer}&gt;</td>
 * <td>{@link Int64}</td>
 * </tr>
 * <tr>
 * <td>&lt;Integral number not in range of {@link Long} but representable as an unsigned 64-bit integer&gt;</td>
 * <td>{@link Int64}</td>
 * </tr>
 * <tr>
 * <td>&lt;Non-integral number in range of {@link Double}&gt;</td>
 * <td>{@link Double}</td>
 * </tr>
 * <tr>
 * <td>&lt;Non-integral number not in range of {@link Double}&gt;</td>
 * <td>throw {@link COMMarshallingException}</td>
 * </tr>
 * <tr>
 * <td>{@link java.util.Date}</td><td>{@link java.util.Date}</td>
 * </tr>
 * <tr>
 * <td>{@link String}</td><td>{@link String}</td>
 * </tr>
 * <tr>
 * <td>{@link suneido.language.Concats}</td><td>{@link String}</td>
 * </tr>
 * <tr>
 * <td>{@link suneido.language.jsdi.Buffer}<sup><strong>1</strong></sup></td>
 * <td>throw {@link COMMarshallingException}</td>
 * </tr>
 * <tr>
 * <td>{@link SuCOMobject}</td>
 * <td>{@link SuCOMobject}</td>
 * </tr>
 * </table>
 * <sup><strong>1</strong></sup>: <strong>NOTE</strong> that at some future
 * time it might be convenient to be able to marshall between Buffers and
 * C-style byte arrays. 
 * </p>
 * @author Victor Schappert
 * @since 20131012
 */
@DllInterface
final class Canonifier {

}
