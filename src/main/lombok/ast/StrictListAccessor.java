package lombok.ast;

import java.util.NoSuchElementException;

public interface StrictListAccessor<T extends Node, P extends Node> extends Iterable<T> {
	P up();
	Node owner();
	void clear();
	boolean isEmpty();
	int size();
	T first();
	T last();
	T get(int idx);
	boolean contains(Node source);
	P migrateAllFrom(StrictListAccessor<? extends T, ?> otherList);
	P addToStart(T... node);
	P addToEnd(T... node);
	P addBefore(Node ref, T... node);
	P addAfter(Node ref, T... node);
	P replace(Node source, T replacement);
	void remove(Node source) throws NoSuchElementException;
	RawListAccessor<T, P> asRawAccessor();
}