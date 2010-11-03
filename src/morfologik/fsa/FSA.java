package morfologik.fsa;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;

/**
 * This is a top interface for walking Finite State Automata as described in Jan
 * Daciuk's <i>Incremental Construction of Finite-State Automata and
 * Transducers, and Their Use in the Natural Language Processing</i> (PhD
 * thesis, Technical University of Gdansk).
 */
public abstract class FSA implements Iterable<ByteBuffer> {
	/**
	 * Returns an iterator over all binary sequences starting from the initial
	 * FSA state and ending in final nodes. The returned iterator is a
	 * {@link ByteBuffer} that changes on each call to {@link Iterator#next()},
	 * so if the content should be preserved, it must be copied somewhere else.
	 * 
	 * <p>
	 * It is guaranteed that the returned byte buffer is backed by a byte array
	 * and that the content of the byte buffer starts at the array's index 0.
	 */
	public Iterator<ByteBuffer> iterator() {
		return getTraversalHelper().getAllSubsequences(getRootNode()).iterator();
	}

	/**
	 * Returns an object which can be used to walk the edges of this finite
	 * state automaton and match arbitrary sequences against its states.
	 */
	public FSATraversalHelper getTraversalHelper() {
		return new FSATraversalHelper(this);
	}

	/**
	 * Returns the identifier of the root node of this automaton. May return 0
	 * if the start node is also the end node.
	 */
	public abstract int getRootNode();

	/**
	 * Returns the identifier of the first arc leaving <code>node</code> or 0 if
	 * the node has no outgoing arcs.
	 */
	public abstract int getFirstArc(int node);

	/**
	 * Returns the identifier of an arc leaving <code>node</code> and labeled
	 * with <code>label</code>. An identifier equal to 0 means the node has no
	 * outgoing arc labeled <code>label</code>.
	 */
	public abstract int getArc(int node, byte label);

	/**
	 * Returns the identifier of the next arc after <code>arc</code> and leaving
	 * <code>node</code>. Zero is returned if no more arcs are available for the
	 * node.
	 */
	public abstract int getNextArc(int arc);

	/**
	 * Return the end node pointed to by a given <code>arc</code>. Terminal arcs
	 * (those that point to a terminal state) have no end node representation
	 * and throw a runtime exception.
	 */
	public abstract int getEndNode(int arc);

	/**
	 * Return the label associated with a given <code>arc</code>.
	 */
	public abstract byte getArcLabel(int arc);

	/**
	 * If the automaton was compiled with {@link FSAFlags#NUMBERS}, this method
	 * retrieves the node's number, which (for perfect hashing) equals the count
	 * of the set of right-language elements (final states) under <code>node</code>.
	 */
	public abstract int getNodeNumber(int node);

	/**
	 * Returns <code>true</code> if the destination node at the end of this
	 * <code>arc</code> corresponds to an input sequence created when building
	 * this automaton.
	 */
	public abstract boolean isArcFinal(int arc);

	/**
	 * Returns <code>true</code> if this <code>arc</code> does not have a
	 * terminating node (@link {@link #getEndNode(int)} will throw an
	 * exception). Implies {@link #isArcFinal(int)}.
	 */
	public abstract boolean isArcTerminal(int arc);
	
	/**
	 * Returns a set of flags for this FSA instance.
	 */
	public abstract Set<FSAFlags> getFlags();

	/**
	 * A factory for reading automata in any library-supported version.
	 */
	public static FSA getInstance(InputStream in) throws IOException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in, Math.max(FSAHeader.MAX_HEADER_LENGTH + 1, 1024));
		}

		in.mark(FSAHeader.MAX_HEADER_LENGTH);
		FSAHeader header = FSAHeader.read(in);
		in.reset();

		if (header.version == FSA5.VERSION)
			return new FSA5(in);

		if (header.version == CFSA.VERSION)
			return new CFSA(in);

	    throw new IOException("Unsupported automaton version: " + header.version);
    }
}