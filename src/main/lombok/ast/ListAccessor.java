package lombok.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ListAccessor<T extends Node, P extends Node> {
	private final List<Node> list;
	private final P parent;
	private final Class<T> tClass;
	private final String listName;
	
	private ListAccessor(List<Node> list, P parent, Class<T> tClass, String listName) {
		this.list = list;
		this.parent = parent;
		this.tClass = tClass;
		this.listName = listName;
	}
	
	static <T extends Node, P extends Node> ListAccessor<T, P> of(List<Node> list, P parent, Class<T> tClass, String listName) {
		return new ListAccessor<T, P>(list, parent, tClass, listName);
	}
	
	public void clear() {
		list.clear();
	}
	
	public P addToStart(T node) {
		return addToStartRaw(node);
	}
	
	public P addToStartRaw(Node node) {
		if (node == null) throw new NullPointerException("node");
		parent.adopt(node);
		list.add(0, node);
		return parent;
	}
	
	public P addToEnd(T node) {
		return addToEndRaw(node);
	}
	
	public P addToEndRaw(Node node) {
		if (node == null) throw new NullPointerException("node");
		parent.adopt(node);
		list.add(node);
		return parent;
	}
	
	public P addBefore(T node, Node ref) {
		return addBeforeRaw(node, ref);
	}
	
	public P addBeforeRaw(Node node, Node ref) {
		node.ensureParentless();
		if (ref == null) throw new NullPointerException("ref");
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				parent.adopt(node);
				list.add(i, node);
				return parent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + ref);
	}
	
	public P addAfter(T node, Node ref) {
		return addAfterRaw(node, ref);
	}
	
	public P addAfterRaw(Node node, Node ref) {
		node.ensureParentless();
		if (ref == null) throw new NullPointerException("ref");
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				parent.adopt(node);
				list.add(i+1, node);
				return parent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + ref);
	}
	
	public P replace(Node source, T replacement) {
		return replaceRaw(source, replacement);
	}
	
	public P replaceRaw(Node source, Node replacement) {
		replacement.ensureParentless();
		parent.ensureParentage(source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown(source);
				try {
					parent.adopt(replacement);
				} catch (IllegalStateException e) {
					parent.adopt(source);
					throw e;
				}
				list.set(i, replacement);
				return parent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + source);
	}
	
	public P remove(Node source) throws NoSuchElementException {
		parent.ensureParentage(source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown(source);
				list.remove(i);
				return parent;
			}
		}
		
		throw new NoSuchElementException(listName + " does not contain: " + source);
	}
	
	@SuppressWarnings("unchecked")
	public Iterable<T> getContents() {
		List<T> out = new ArrayList<T>((List<T>)list);
		
		for (Object o : out) {
			if (!tClass.isInstance(o)) throw new AstException(parent, String.format(
					"%s contains an element that isn't of the appropriate type(%s): %s",
					listName, tClass.getSimpleName(), o.getClass().getSimpleName()));
		}
		
		return out;
	}
	
	public Iterable<Node> getRawContents() {
		return new ArrayList<Node>(list);
	}
	
	public int size() {
		return list.size();
	}
}
