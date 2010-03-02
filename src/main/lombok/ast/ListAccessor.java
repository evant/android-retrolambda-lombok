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
	private final List<AbstractNode> list;
	private final AbstractNode parent;
	private final Class<T> tClass;
	private final String listName;
	private final P returnAsParent;
	
	private ListAccessor(List<AbstractNode> list, AbstractNode parent, Class<T> tClass, String listName, P returnAsParent) {
		this.list = list;
		this.parent = parent;
		this.tClass = tClass;
		this.listName = listName;
		this.returnAsParent = returnAsParent;
	}
	
	static <T extends Node, P extends AbstractNode> ListAccessor<T, P> of(List<AbstractNode> list, P parent, Class<T> tClass, String listName) {
		return new ListAccessor<T, P>(list, parent, tClass, listName, parent);
	}
	
	<Q extends Node> ListAccessor<T, Q> wrap(Q returnThisAsParent) {
		return new ListAccessor<T, Q>(list, parent, tClass, listName, returnThisAsParent);
	}
	
	public P up() {
		return returnAsParent;
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
		Iterator<AbstractNode> it = otherList.list.iterator();
		while (it.hasNext()) {
			AbstractNode n = it.next();
			otherList.parent.disown(n);
			it.remove();
			this.addToEndRaw(n);
		}
		
		return returnAsParent;
	}
	
	public P addToStart(T... node) {
		return addToStartRaw(node);
	}
	
	public P addToStartRaw(Node... node) {
		for (Node n : node) {
			AbstractNode child = (AbstractNode)n;
			if (child != null) {
				parent.adopt(child);
				list.add(0, child);
			}
		}
		return returnAsParent;
	}
	
	public P addToEnd(T... node) {
		return addToEndRaw(node);
	}
	
	public P addToEndRaw(Node... node) {
		for (Node n : node) {
			AbstractNode child = (AbstractNode)n;
			if (node != null) {
				parent.adopt(child);
				list.add(child);
			}
		}
		return returnAsParent;
	}
	
	public P addBefore(Node ref, T... node) {
		if (node == null) throw new NullPointerException("node");
		return addBeforeRaw(ref, node);
	}
	
	public P addBeforeRaw(Node ref, Node... node) {
		if (ref == null) throw new NullPointerException("ref");
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				int j = 0;
				for (Node n : node) {
					AbstractNode child = (AbstractNode)n;
					if (child != null) {
						child.ensureParentless();
						parent.adopt(child);
						list.add(i + j, child);
						j++;
					}
				}
				return returnAsParent;
			}
		}
		throw new IllegalStateException(listName + " does not contain: " + ref);
	}
	
	public P addAfter(Node ref, T... node) {
		if (node == null) throw new NullPointerException("node");
		return addAfterRaw(ref, node);
	}
	
	public P addAfterRaw(Node ref, Node... node) {
		if (ref == null) throw new NullPointerException("ref");
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == ref) {
				int j = 0;
				for (Node n : node) {
					AbstractNode child = (AbstractNode)n;
					if (child != null) {
						child.ensureParentless();
						parent.adopt(child);
						list.add(i + j + 1, child);
						j++;
					}
				}
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
		if (replacement != null) ((AbstractNode)replacement).ensureParentless();
		parent.ensureParentage((AbstractNode)source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown((AbstractNode)source);
				try {
					if (replacement != null) parent.adopt((AbstractNode)replacement);
				} catch (IllegalStateException e) {
					parent.adopt((AbstractNode)source);
					throw e;
				}
				if (replacement == null) list.remove(i);	//screws up for counter, but we return right after anyway, so it doesn't matter.
				else list.set(i, (AbstractNode)replacement);
				return returnAsParent;
			}
		}
		
		throw new IllegalStateException(listName + " does not contain: " + source);
	}
	
	public P remove(Node source) throws NoSuchElementException {
		if (source == null) return returnAsParent;
		parent.ensureParentage((AbstractNode)source);
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) {
				parent.disown((AbstractNode)source);
				list.remove(i);
				return returnAsParent;
			}
		}
		
		throw new NoSuchElementException(listName + " does not contain: " + source);
	}
	
	public boolean contains(Node source) {
		if (source == null) return false;
		if (source.getParent() != parent) return false;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == source) return true;
		}
		return false;
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
