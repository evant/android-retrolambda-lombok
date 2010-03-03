package lombok.ast;

public interface RawListAccessor<T extends Node, P extends Node> extends Iterable<Node> {
	P up();
	Node owner();
	void clear();
	boolean isEmpty();
	int size();
	Node first();
	Node last();
	Node get(int idx);
	boolean contains(Node source);
	P migrateAllFrom(RawListAccessor<?, ?> otherList);
	P addToStart(Node... node);
	P addToEnd(Node... node);
	P addBefore(Node ref, Node... node);
	P addAfter(Node ref, Node... node);
	P replace(Node source, Node replacement);
	P remove(Node source);
	StrictListAccessor<T, P> asStrictAccessor();
}
