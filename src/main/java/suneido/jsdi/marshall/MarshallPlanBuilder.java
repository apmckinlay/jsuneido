/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import java.util.ArrayList;

import suneido.jsdi.DllInterface;

import com.google.common.primitives.Ints;

/**
 * Class for constructing a {@link MarshallPlan}. An algorithm that traverses
 * a type tree can use the builder to make a marshalling plan.
 *
 * @author Victor Schappert
 * @since 20130724
 */
@DllInterface
public abstract class MarshallPlanBuilder {

	//
	// DATA
	//

	private   final int variableIndirectCount;
	private   final ArrayList<Integer> posList;
	protected final ArrayList<Integer> ptrList;
	private   final ArrayList<Integer> nextPosStack; // indirection
	private   final ArrayList<Integer> alignStack;   // container alignment
	private   final ArrayList<ElementSkipper> skipperStack; // containers
	private   final boolean alignToWordBoundary;
	private         ElementSkipper skipper;
	private         int nextPos;
	private         int nextPtdToPos;
	private         int variableIndirectPos;
	private         int minAlignDirect;
	private         int minAlignIndirect;

	//
	// CONSTANTS
	//

	private static final int INDIRECT_MASK = 0x40000000; // Indicates a position is in the indirect storage part
	private static final int VI_MASK = 0x20000000; // Indicates a variable indirect storage index 

	//
	// CONSTRUCTORS
	//

	protected MarshallPlanBuilder(int variableIndirectCount,
			boolean alignToWordBoundary) {
		this.variableIndirectCount = variableIndirectCount;
		this.posList = new ArrayList<>(24);
		this.ptrList = new ArrayList<>();
		this.nextPosStack = new ArrayList<>();
		this.skipperStack = new ArrayList<>();
		this.alignStack = new ArrayList<>();
		this.alignToWordBoundary = alignToWordBoundary;
		this.skipper = new ElementSkipper(0, 0);
		this.nextPos = 0;
		this.nextPtdToPos = INDIRECT_MASK;
		this.variableIndirectPos = 0;
		this.minAlignDirect = 1;
		this.minAlignIndirect = 1;
	}

	//
	// MUTATORS
	//

	/**
	 * <p>
	 * Allocates space in the plan for a non-aggregate value.
	 * </p>
	 *
	 * @param bytes
	 *            Size, in bytes, of the value that will be stored in the
	 *            allocated space
	 * @param alignTo
	 *            Required alignment, in bytes, of the value that will be stored
	 *            (<em>eg</em> 1, 2, 4, 8)
	 * @see #ptrBasic(int, int)
	 */
	public final void basic(int bytes, int alignTo) {
		pos(bytes, alignTo);
	}

	/**
	 * <p>
	 * Allocates space in the plan for a pointer to an aggregate value and
	 * increments the indirection level.
	 * </p>
	 *
	 * <p>
	 * Each call to this method must be bracketed by a matching call to
	 * {@link #ptrEnd()}. However, the bracketing {@link #ptrEnd()} must not be
	 * called until storage for {@code pointedToSizeBytes} have been allocated
	 * by calling {@link #basic(int, int)} or {@link #ptrBasic(int, int)}.
	 * </p>
	 * 
	 * <p>
	 * Until the {@link #ptrEnd()} call, all storage allocated by this builder
	 * will be placed in the indirect storage referenced by the pointer created
	 * by this method.
	 * </p>
	 *
	 * <p>
	 * Note that the size of the pointer allocated by this method is, and its
	 * alignment, are both implicitly {@link PrimitiveSize#POINTER}.
	 * </p>
	 *
	 * @param pointedToSizeBytes
	 *            Size, in bytes, of the aggregate value that will be stored in
	 *            the allocated space
	 * @param alignTo
	 *            Required alignment, in bytes, of the aggregate value (
	 *            <em>eg</em> 1, 2, 4, 8)
	 * @see #ptrEnd()
	 * @see #ptrBasic(int, int)
	 * @see #containerBegin()
	 * @see #ptrVariableIndirect()
	 */
	public final void ptrBegin(int pointedToSizeBytes, int alignTo) {
		final int ptrPos = pos(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		final int pointedToPos = nextAligned(nextPtdToPos, alignTo);
		nextPtdToPos = pointedToPos + pointedToSizeBytes;
		nextPosStack.add(nextPos);
		nextPos = pointedToPos;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	/**
	 * <p>
	 * Ends space allocation for the aggregate pointed to by a pointer allocated
	 * in {@link #ptrBegin(int, int)} and decrements the indirection level.
	 * </p>
	 */
	public final void ptrEnd() {
		int N = nextPosStack.size();
		nextPos = nextPosStack.remove(N - 1).intValue();
	}

	/**
	 * <p>
	 * Allocates space for two values: a pointer; and the non-aggregate,
	 * non-pointer value the pointer points to. This method does not increment
	 * the indirection level.
	 * </p>
	 *
	 * <p>
	 * Note that the size of the pointer allocated by this method is, and its
	 * alignment, are both implicitly {@link PrimitiveSize#POINTER}.
	 * </p>
	 *
	 * @param pointedToSizeBytes
	 *            Size to allocate for the non-aggregate, non-pointer value
	 *            pointed to by the allocated pointer
	 * @param alignTo
	 *            Required alignment, in bytes, of the value that will be stored
	 *            at the location pointed to by the pointer (<em>eg</em> 1, 2,
	 *            4, 8)
	 * @see #basic(int, int)
	 * @see #ptrBegin(int, int)
	 * @see #ptrVariableIndirect()
	 */
	public final void ptrBasic(int pointedToSizeBytes, int alignTo) {
		// NOTE: Nobody uses this except BasicPointer, which is slated to be
		//       deleted in any event.
		final int ptrPos = pos(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		final int pointedToPos = nextAligned(nextPtdToPos, alignTo);
		minAlignIndirect = Math.max(minAlignIndirect, alignTo);
		nextPtdToPos = pointedToPos + pointedToSizeBytes;
		posList.add(pointedToPos);
		++skipper.nPos;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	/**
	 * <p>
	 * Allocates space for two values: a pointer, a variable-length value, such
	 * as a string, that the pointer pointers to.
	 * </p>
	 *
	 * <p>
	 * Note that the size of the pointer allocated by this method is, and its
	 * alignment, are both implicitly {@link PrimitiveSize#POINTER}.
	 * </p>
	 *
	 * @see #ptrVariableIndirectPseudoParam()
	 * @see #ptrBasic(int, int)
	 * @see #ptrBegin(int, int)
	 */
	public final void ptrVariableIndirect() {
		assert variableIndirectPos < variableIndirectCount;
		int ptrPos = pos(PrimitiveSize.POINTER, PrimitiveSize.POINTER);
		int pointedToPos = VI_MASK | variableIndirectPos++;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	/**
	 * <p>
	 * Allocates space for a "pseudo-parameter" that will hold a variable
	 * indirect return value. This method may be called only once for any given
	 * plan builder, and it must be the last storage allocation method called on
	 * the builder. 
	 * </p>
	 *
	 * <p>
	 * The variable indirect pseudo-parameter is a pointer to variable indirect
	 * storage that sits at the last position in the marshalled storage block.
	 * It is also the last variable indirect pointer allocated, so its variable
	 * indirect index is equal to the variable indirect count minus 1 (note,
	 * therefore, that the variable indirect count passed to the builder on
	 * construction must include 1 count for the vipp.
	 * </p>
	 *
	 * <p>
	 * Conceptually, the relevant data structures look like the following:
	 * <code>
	 * <dl>
	 *     <dt>data</dt>
	 *     <dd>
	 *     <div style="white-space:pre">&lt;direct storage&gt; &lt;align pad&gt; &lt;indirect storage&gt; &lt;align pad&gt; &lt;vipp pointer&gt;</div>
	 *     <br/><div style="white-space:pre;tab-size:75">&#9;^ size_total</div>
	 *     </dd>
	 *     <dt>posList</dt>
	 *     <dd>pos<sub>0</sub>, pos<sub>1</sub> ..., pos<sub>N-1</sub>, pos<sub>vipp</sub></dd>
	 *     <dt>ptrList</dt>
	 *     <dd>&lt;word(ptr<sub>0</sub>), byte(ptd_to<sub>0</sub>)&gt;, &lt;word(ptr<sub>1</sub>), byte(ptd_to<sub>1</sub>)&gt;, ..., &lt;word(ptr<sub>N-1</sub>), byte(ptd_to<sub>N-1</sub>)&gt;, &lt;word(vipp), size_total + vi_count - 1&gt;
	 * </dl>
	 * </code>
	 * </p>
	 * 
	 * @see #ptrVariableIndirect()
	 */
	public final void ptrVariableIndirectPseudoParam() {
		assert variableIndirectCount - 1 == variableIndirectPos;
		assert nextPosStack.isEmpty() && skipperStack.isEmpty();
		assert 0 == (nextPos & INDIRECT_MASK);
		nextPosStack.add(nextPos);
		nextPos = nextPtdToPos;
		ptrVariableIndirect();
		nextPtdToPos = nextPos;            // include the vipp in indirect block
		nextPos = nextPosStack.remove(0);  // needed to get correct direct size
	}

	/**
	 * <p>
	 * Increments the containment level and indicates the start of a
	 * container-type aggregate.
	 * </p>
	 *
	 * <p>
	 * This method should be called to initiate storage for a container-type
	 * aggregate (<em>ie</em> {@code struct}). Each call to this method must be
	 * bracketed by a matching call to {@link #containerEnd()} after all of the
	 * storage required by the container's elements has been allocated.
	 * </p>
	 *
	 * @param alignTo
	 *            Required alignment, in bytes, of the whole container (
	 *            <em>eg</em> 1, 2, 4, 8)
	 * @see #containerEnd()
	 * @see #arrayBegin()
	 * @see #ptrBegin(int, int)
	 */
	public final void containerBegin(int alignTo) {
		skipperStack.add(skipper);
		skipper = new ElementSkipper(0, 0);
		nextPos = nextAligned(alignTo);
		alignStack.add(alignTo);
	}

	/**
	 * <p>
	 * Decrements the containment level and indicates the end of a
	 * container-type aggregate.
	 * </p>
	 *
	 * <p>
	 * The {@link ElementSkipper} returned by this method can be used to skip
	 * over the container-type aggregate during marshalling when there is no
	 * value to marshall in. For example, in the following Suneido code:
	 * 
	 * <pre>    // Suppose A => struct { B b } and B => struct { int8 x ; int 8 y }
	 *    (<strong>dll void</strong> Library:f(A a))(Object())</pre>
	 * 
	 * the value to be marshalled as parameter <code>a</code> of structure type
	 * <code>A</code> simply does not contain a member <code>b</code>. Thus, the
	 * marshalling of <code>a</code>'s member <code>b</code> can be skipped by
	 * using the {@link ElementSkipper} returned by calling this method when
	 * ending storage allocation for <code>b</code>.
	 * </p>
	 *
	 * @return An element skipper for the container-type aggregate, as described
	 *         above
	 * @see #containerBegin()
	 */
	public final ElementSkipper containerEnd() {
		final ElementSkipper result = skipper;
		final int N = skipperStack.size();
		skipper = skipperStack.remove(N - 1);
		skipper.nPos += result.nPos;
		skipper.nPtr += result.nPtr;
		assert N == alignStack.size();
		final int nextAlignTo = alignStack.remove(N - 1);
		if (alignToWordBoundary && nextPosStack.isEmpty()
				&& skipperStack.isEmpty()) {
			// If we are now at the outermost containment level (i.e. we just
			// finished a parameter that is itself a container), make sure we
			// advance to the next word boundary.
			final int rem = nextPos % PrimitiveSize.WORD;
			if (0 != rem) {
				nextPos += PrimitiveSize.WORD - rem;
			}
		} else {
			nextPos = nextAligned(nextAlignTo);
		}
		// 
		return result;
	}

	/**
	 * <p>
	 * Increments the containment level and indicates the start of an array-type
	 * aggregate.
	 * </p>
	 *
	 * <p>
	 * This method should be called to initiate storage for an array-type
	 * aggregate (<em>ie</em> <code><strong>int32</strong>[6]</code>). Each call
	 * to this method must be bracketed by a matching call to
	 * {@link #arrayEnd()} after all of the storage required by the container's
	 * elements has been allocated.
	 * </p>
	 *
	 * @param alignTo
	 *            Required alignment, in bytes, of the the array elements (
	 *            <em>eg</em> 1, 2, 4, 8)
	 * @see #arrayEnd()
	 * @see #containerBegin()
	 * @see #ptrBegin(int, int)
	 */
	public final void arrayBegin(int alignTo) {
		containerBegin(alignTo);
	}

	/**
	 * <p>
	 * Decrements the containment level and indicates the end of an array-type
	 * aggregate.
	 * </p>
	 *
	 * <p>
	 * The {@link ElementSkipper} returned by this method can be used to skip
	 * over the array-type aggregate during marshalling when there is no
	 * value to marshall in. For example, in the following Suneido code:
	 * 
	 * <pre>    // Suppose C => struct { int8[100] d }
	 *    (<strong>dll void</strong> Library:g(C c))(Object())</pre>
	 * 
	 * the value to be marshalled as parameter <code>c</code> of structure type
	 * <code>C</code> simply does not contain a member <code>d</code>. Thus, the
	 * marshalling of <code>c</code>'s member <code>d</code> can be skipped by
	 * using the {@link ElementSkipper} returned by calling this method when
	 * ending storage allocation for <code>d</code>.
	 * </p>
	 *
	 * @return Element skipper for the array-type aggregate, as described above
	 * @see #arrayBegin()
	 */
	public final ElementSkipper arrayEnd() {
		return containerEnd();
	}

	public final MarshallPlan makeMarshallPlan() {
		// Preliminary assertions
		assert 0 == (nextPos & INDIRECT_MASK);
		assert nextPosStack.isEmpty();
		assert posList.size() == skipper.nPos;
		assert ptrList.size() == skipper.nPtr;
		assert skipperStack.isEmpty();
		assert alignStack.isEmpty();
		assert variableIndirectPos == variableIndirectCount;
		// While the builder is still in use to allocate storage in a plan (i.e.
		// before this method gets called), the ultimate direct and indirect
		// size of the marshall plan isn't known. Thus we have to flag items
		// with MASK_INDIRECT or MASK_VI to show they indicate indirect or
		// variable-indirect storage.
		//
		// But now that the storage allocation is done, we need to finalize the
		// position list and pointer list based on the actual final size of the
		// storage required for the plan.
		final int sizeDirect = nextAligned(nextPos, minAlignDirect);
		final int sizeIndirect = nextPtdToPos & (~INDIRECT_MASK);
		int sizeTotal = 0;
		int[] ptrArray = null;
		int[] posArray = null;
		if (0 == sizeIndirect && 0 == variableIndirectCount) {
			sizeTotal = nextAligned(sizeDirect, Long.BYTES);
			ptrArray = new int[0];
			posArray = Ints.toArray(posList);
		} else {
			final int indirectStart = nextAligned(sizeDirect,
					Math.max(PrimitiveSize.WORD, minAlignIndirect));
			sizeTotal = nextAligned(indirectStart + sizeIndirect, Long.BYTES);
			// Splice position lists together
			final int N = posList.size();
			posArray = new int[N];
			for (int k = 0; k < N; ++k) {
				final int pos = posList.get(k);
				if (INDIRECT_MASK == (pos & INDIRECT_MASK))
					posArray[k] = indirectStart + (pos & ~INDIRECT_MASK);
				else
					posArray[k] = pos;
			}
			// Fix up pointer list
			final int P = ptrList.size();
			ptrArray = new int[P];
			for (int k = 0; k < P;) {
				final int ptrByteOffset = ptrList.get(k);
				assert 0 == ptrByteOffset % PrimitiveSize.POINTER; // TODO: You can take this out when sure it works
				if (INDIRECT_MASK == (ptrByteOffset & INDIRECT_MASK)) {
					ptrArray[k] = (indirectStart + (ptrByteOffset & ~INDIRECT_MASK));
				} else {
					ptrArray[k] = ptrByteOffset;
				}
				++k;
				final int ptdToItem = ptrList.get(k);
				if (INDIRECT_MASK == (INDIRECT_MASK & ptdToItem)) {
					// Indirect storage byte offset
					ptrArray[k] = indirectStart + (ptdToItem & ~INDIRECT_MASK);
				} else {
					// Variable indirect index
					assert VI_MASK == (VI_MASK & ptdToItem);
					ptrArray[k] = sizeTotal + (ptdToItem & ~VI_MASK);
				}
				++k;
			}
		}
		// Create the marshall plan
		return makeMarshallPlan(sizeDirect, minAlignDirect, sizeIndirect,
				sizeTotal, variableIndirectCount, ptrArray, posArray);
	}

	//
	// INTERNALS
	//

	protected abstract MarshallPlan makeMarshallPlan(int sizeDirect,
			int alignDirect, int sizeIndirect, int sizeTotal,
			int variableIndirectCount, int[] ptrArray, int[] posArray);

	private static int nextAligned(final int nextRaw, final int alignTo) {
		assert 0 <= nextRaw && 0 < alignTo;
		final int rem = nextRaw % alignTo;
		return 0 == rem ? nextRaw : nextRaw + (alignTo - rem);
	}

	private int nextAligned(final int alignTo) {
		if (nextPosStack.isEmpty()) {
			minAlignDirect = Math.max(minAlignDirect, alignTo);
		} else {
			minAlignIndirect = Math.max(minAlignIndirect, alignTo);
		}
		return nextAligned(nextPos, alignTo);
	}

	private int pos(final int bytes, int alignTo) {
		if (alignToWordBoundary && nextPosStack.isEmpty()
				&& skipperStack.isEmpty()) {
			// If this is an argument to a function, align it to the next whole
			// word. This is correct on both x86 stdcall and x64.
			alignTo = PrimitiveSize.WORD;
		}
		final int pos = nextAligned(alignTo);
		posList.add(pos);
		nextPos = pos + bytes;
		++skipper.nPos;
		return pos;
	}
}
