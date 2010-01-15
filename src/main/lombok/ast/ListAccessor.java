/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ListAccessor<T extends Node, P extends Node> {
	private final List<Node> list;
	private final Node parent;
	private final Class<T> tClass;
	private final String listName;
	private final P returnAsParent;
	
	private ListAccessor(List<Node> list, Node parent, Class<T> tClass, String listName, P returnAsParent) {
		this.list = list;
		this.parent = parent;
		this.tClass = tClass;
		this.listName = listName;
		this.returnAsParent = returnAsParent;
	}
	
	static <T extends Node, P extends Node> ListAccessor<T, P> of(List<Node> list, P parent, Class<T> tClass, String listName) {
		return new ListAccessor<T, P>(list, parent, tClass, listName, parent);
	}
	
	<Q extends Node> ListAccessor<T, Q> wrap(Q returnThisAsParent) {
		return new ListAccessor<T, Q>(list, parent, tClass, listName, returnThisAsParent);
	}
	
	public void clear() {
		list.clear();
	}
	
	public P migrateAllFrom(ListAccessor<? extends T, ?> otherList) {
		for (Node n : otherList.list) {
			if (!tClass.isInstance(n)) throw new AstException(otherList.parent, String.format("List %s contains node that aren't of type %s", otherList.listName, tClass));
		}
		
		return migrateAllFromRaw(otherList);
	}
	
	public P migrateAllFromRaw(ListAccessor<? extends Node, ?> otherList) {
		Iterator<Node> it = otherList.list.iterator();
		while (it.hasNext()) {
			Node n = it.next();
			otherList.parent.disown(n);
			it.remove();
			this.addToEndRaw(n);
		}
		
		return returnAsParent;
	}
	
	public P addToStart(T node) {
		if (node == null) throw new NullPointerException("node");
		return addToStartRaw(node);
	}
	
	public P addToStartRaw(Node node) {
		if (node == null) throw new NullPointerException("node");
		parent.adopt(node);
		list.add(0, node);
		return returnAsParent;
	}
	
	public P addToEnd(T node) {
		if (node == null) throw new NullPointerException("node");
		return addToEndRaw(node);
	}
	
	public P addToEndRaw(Node node) {
		if (node != null) {
			parent.adopt(node);
			list.add(node);
		}
		return returnAsParent;
	}
	
	public P addBefore(T node, Node ref) {
		if (node == null) throw new NullPointerException("node");
		return addBeforeRaw(node, ref);
	}
	
	public P addBeforeRaw(Node node, Node ref) {
		if (node == null) return returnAsParent;
		if (ref == null) throw new NullPointerException("ref");
		node.ensureParentless();
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				parent.adopt(node);
				list.add(i, node);
				return returnAsParent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + ref);
	}
	
	public P addAfter(T node, Node ref) {
		if (node == null) throw new NullPointerException("node");
		return addAfterRaw(node, ref);
	}
	
	public P addAfterRaw(Node node, Node ref) {
		if (node == null) return returnAsParent;
		if (ref == null) throw new NullPointerException("ref");
		node.ensureParentless();
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				parent.adopt(node);
				list.add(i+1, node);
				return returnAsParent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + ref);
	}
	
	public P replace(Node source, T replacement) {
		if (replacement == null) throw new NullPointerException("replacement");
		return replaceRaw(source, replacement);
	}
	
	public P replaceRaw(Node source, Node replacement) {
		if (replacement != null) replacement.ensureParentless();
		parent.ensureParentage(source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown(source);
				try {
					if (replacement != null) parent.adopt(replacement);
				} catch (IllegalStateException e) {
					parent.adopt(source);
					throw e;
				}
				if (replacement == null) list.remove(i);	//screws up for counter, but we return right after anyway, so it doesn't matter.
				else list.set(i, replacement);
				return returnAsParent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + source);
	}
	
	public P remove(Node source) throws NoSuchElementException {
		if (source == null) return returnAsParent;
		parent.ensureParentage(source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown(source);
				list.remove(i);
				return returnAsParent;
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
	
	public Node rawFirst() {
		try {
			return list.get(0);
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	
	public T first() {
		Node r = rawFirst();
		if (!tClass.isInstance(r)) throw new AstException(parent, String.format(
				"first element of %w isn't of the appropriate type(%s): %s",
				listName, tClass.getSimpleName(), r.getClass().getSimpleName()));
		return tClass.cast(r);
	}
	
	public Node rawLast() {
		try {
			return list.get(list.size()-1);
		} catch (IndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		}
	}
	
	public T last() {
		Node r = rawLast();
		if (!tClass.isInstance(r)) throw new AstException(parent, String.format(
				"last element of %w isn't of the appropriate type(%s): %s",
				listName, tClass.getSimpleName(), r.getClass().getSimpleName()));
		return tClass.cast(r);
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public Iterable<Node> getRawContents() {
		return new ArrayList<Node>(list);
	}
	
	public int size() {
		return list.size();
	}
}
