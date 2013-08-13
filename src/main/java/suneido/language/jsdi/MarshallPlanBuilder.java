package suneido.language.jsdi;

import java.util.ArrayList;

import suneido.language.jsdi.type.PrimitiveSize;

import com.google.common.primitives.Ints;

/**
 * Class for constructing a {@link MarshallPlan} by traversing a type tree. 
 *
 * @author Victor Schappert
 * @since 20130724
 */
@DllInterface
public final class MarshallPlanBuilder {

	//
	// DATA
	//

	private final int                       sizeDirect;
	private final int                       sizeIndirect;
	private final int                       variableIndirectCount;
	private final ArrayList<Integer>        posList;
	private final ArrayList<Integer>        ptrList;
	private final ArrayList<Integer>        nextPosStack; // indirection
	private final ArrayList<ElementSkipper> skipperStack; // containers
	private       ElementSkipper            skipper;
	private       int                       nextPos;
	private       int                       endPos;
	private       int                       variableIndirectPos;

	//
	// CONSTRUCTORS
	//

	public MarshallPlanBuilder(int sizeDirect, int sizeIndirect, int variableIndirectCount) {
		this.sizeDirect = sizeDirect;
		this.sizeIndirect = sizeIndirect;
		this.variableIndirectCount = variableIndirectCount;
		this.posList = new ArrayList<Integer>((sizeDirect + sizeIndirect)
				/ PrimitiveSize.WORD + 10);
		this.ptrList = new ArrayList<Integer>();
		this.nextPosStack = new ArrayList<Integer>();
		this.skipperStack = new ArrayList<ElementSkipper>();
		skipper = new ElementSkipper(0, 0);
		this.nextPos = 0;
		this.endPos = sizeDirect;
		this.variableIndirectPos = 0;
	}

	//
	// MUTATORS
	//

	public int pos(int bytes) {
		final int pos = nextPos;
		assert 0 <= pos && pos < endPos && endPos <= sizeDirect + sizeIndirect;
		posList.add(pos);
		if (nextPosStack.isEmpty() && skipperStack.isEmpty()) {
			// If this is an argument to a function, add the whole word size of
			// the argument, since that's how stdcall works.
			nextPos += PrimitiveSize.sizeWholeWords(bytes);
		} else {
			nextPos += bytes;
		}
		++skipper.nPos;
		return pos;
	}

	public void ptrBegin(int pointedToSizeBytes) {
		int ptrPos = pos(PrimitiveSize.POINTER);
		int pointedToPos = endPos;
		endPos += pointedToSizeBytes;
		assert sizeDirect <= endPos && endPos <= sizeDirect + sizeIndirect;
		nextPosStack.add(nextPos);
		nextPos = pointedToPos;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	public void ptrEnd() {
		int N = nextPosStack.size();
		nextPos = nextPosStack.remove(N - 1).intValue();
	}

	public void ptrBasic(int pointedToSizeBytes) {
		int ptrPos = pos(PrimitiveSize.POINTER);
		int pointedToPos = endPos;
		endPos += pointedToSizeBytes;
		assert sizeDirect <= endPos && endPos <= sizeDirect + sizeIndirect;
		posList.add(pointedToPos);
		++skipper.nPos;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	public void variableIndirectPtr() {
		int ptrPos = pos(PrimitiveSize.POINTER);
		int pointedToPos = sizeDirect + sizeIndirect + variableIndirectPos++;
		assert sizeDirect + sizeIndirect <= pointedToPos
				&& pointedToPos < sizeDirect + sizeIndirect
						+ variableIndirectCount;
		ptrList.add(ptrPos);
		ptrList.add(pointedToPos);
		skipper.nPtr += 2;
	}

	public void variableIndirectPseudoArg() {
		assert nextPosStack.isEmpty() && skipperStack.isEmpty();
		assert sizeDirect == nextPos;
		assert sizeDirect + sizeIndirect - PrimitiveSize.POINTER == endPos;
		endPos = sizeDirect + sizeIndirect;
		nextPos = endPos - PrimitiveSize.POINTER;
		variableIndirectPtr();
		nextPos = sizeDirect; // keep assertions happy
	}

	public void containerBegin() {
		skipperStack.add(skipper);
		skipper = new ElementSkipper(0, 0);
	}

	// TODO: Note that this returned value is valid for any marshall plan
	//       that includes the container, not just the one under construction
	public ElementSkipper containerEnd() {
		final ElementSkipper result = skipper;
		int N = skipperStack.size();
		skipper = skipperStack.remove(N - 1);
		skipper.nPos += result.nPos;
		skipper.nPtr += result.nPtr;
		// If we are now at the outermost containment level (i.e. we just
		// finished a stdcall argument that is itself a container), make sure we
		// advance to the next word boundary.
		if (nextPosStack.isEmpty() && skipperStack.isEmpty()) {
			int rem = nextPos % PrimitiveSize.WORD;
			if (0 != rem) {
				nextPos += PrimitiveSize.WORD - rem;
			}
			assert nextPos <= sizeDirect;
		}
		return result;
	}

	public void arrayBegin() {
		containerBegin();
	}

	public ElementSkipper arrayEnd() {
		return containerEnd();
	}

	public MarshallPlan makeMarshallPlan() {
		assert nextPosStack.isEmpty();
		assert sizeDirect == nextPos
				&& sizeDirect + sizeIndirect == endPos;
		assert skipperStack.isEmpty();
		assert posList.size() == skipper.nPos;
		assert ptrList.size() == skipper.nPtr;
		return new MarshallPlan(
			sizeDirect,
			sizeIndirect,
			Ints.toArray(ptrList),
			Ints.toArray(posList),
			variableIndirectCount
		);
	}
}
