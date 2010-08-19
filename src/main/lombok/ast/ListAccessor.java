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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class ListAccessor<T extends Node, P extends Node> {
	private List<AbstractNode> list;
	private final AbstractNode parent;
	private final Class<T> tClass;
	private final String listName;
	private final P returnAsParent;
	private boolean escaped;
	
	private ListAccessor(AbstractNode parent, Class<T> tClass, String listName, P returnAsParent) {
		this.list = new ArrayList<AbstractNode>();
		this.parent = parent;
		this.tClass = tClass;
		this.listName = listName;
		this.returnAsParent = returnAsParent;
	}
	
	private void fixEscaped() {
		if (escaped) {
			list = new ArrayList<AbstractNode>(list);
			escaped = false;
		}
	}
	
	public static <T extends Node, P extends Node> StrictListAccessor<T, P> emptyStrict(final String listName, final P returnAsParent) {
		return new StrictListAccessor<T, P>() {
			@Override public P addAfter(Node ref, T... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addBefore(Node ref, T... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addToEnd(T... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addToStart(T... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public RawListAccessor<T, P> asRawAccessor() {
				return emptyRaw(listName, returnAsParent);
			}
			
			@Override public void clear() {
			}
			
			@Override public boolean contains(Node source) {
				return false;
			}
			
			@Override public T first() {
				return null;
			}
			
			@Override public boolean isEmpty() {
				return true;
			}
			
			@Override public T last() {
				return null;
			}
			
			@Override public P migrateAllFrom(StrictListAccessor<? extends T, ?> otherList) {
				if (otherList != null && otherList.getClass() != getClass()) throw new UnsupportedOperationException();
				return returnAsParent;
			}
			
			@Override public Node owner() {
				return returnAsParent;
			}
			
			@Override public void remove(Node source) throws NoSuchElementException {
				throw new NoSuchElementException();
			}
			
			@Override public void replace(Node source, T replacement) throws NoSuchElementException {
				throw new NoSuchElementException(listName + " does not contain: " + source);
			}
			
			@Override public int size() {
				return 0;
			}
			
			@Override public P up() {
				return returnAsParent;
			}
			
			@Override public Iterator<T> iterator() {
				return Collections.<T>emptyList().iterator();
			}
		};
	}
	
	public static <T extends Node, P extends Node> RawListAccessor<T, P> emptyRaw(final String listName, final P returnAsParent) {
		return new RawListAccessor<T, P>() {
			@Override public P addAfter(Node ref, Node... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addBefore(Node ref, Node... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addToEnd(Node... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public P addToStart(Node... node) {
				throw new UnsupportedOperationException();
			}
			
			@Override public StrictListAccessor<T, P> asStrictAccessor() {
				return emptyStrict(listName, returnAsParent);
			}
			
			@Override public void clear() {
			}
			
			@Override public boolean contains(Node source) {
				return false;
			}
			
			@Override public Node first() {
				return null;
			}
			
			@Override public boolean isEmpty() {
				return true;
			}
			
			@Override public Node last() {
				return null;
			}
			
			@Override public P migrateAllFrom(RawListAccessor<?, ?> otherList) {
				if (otherList != null && otherList.getClass() != getClass()) throw new UnsupportedOperationException();
				return returnAsParent;
			}
			
			@Override public Node owner() {
				return returnAsParent;
			}
			
			@Override public boolean remove(Node source) {
				return false;
			}
			
			@Override public boolean replace(Node source, Node replacement) throws NoSuchElementException {
				return false;
			}
			
			@Override public int size() {
				return 0;
			}
			
			@Override public P up() {
				return returnAsParent;
			}
			
			@Override public Iterator<Node> iterator() {
				return Collections.<Node>emptyList().iterator();
			}
		};
	}
	
	private RawListAccessor<T, P> raw = new RawListAccessor<T, P>() {
		@Override
		public P up() {
			return returnAsParent;
		}
		
		@Override
		public Node owner() {
			return parent;
		}
		
		@Override
		public void clear() {
			if (escaped) {
				list = new ArrayList<AbstractNode>();
				escaped = false;
			} else {
				list.clear();
			}
		}
		
		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}
		
		@Override
		public int size() {
			return list.size();
		}
		
		@Override
		public Node first() {
			try {
				return list.get(0);
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		}
		
		@Override
		public Node last() {
			try {
				return list.get(list.size()-1);
			} catch (IndexOutOfBoundsException e) {
				return null;
			}
		}
		
		@Override
		public boolean contains(Node source) {
			if (source == null) return false;
			if (source.getParent() != parent) return false;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == source) return true;
			}
			return false;
		}
		
		@Override
		public P migrateAllFrom(RawListAccessor<?, ?> otherList) {
			while (!otherList.isEmpty()) {
				AbstractNode n = (AbstractNode) otherList.first();
				otherList.remove(n);
				addToEnd(n);
			}
			
			return returnAsParent;
		}
		
		@Override
		public P addToStart(Node... node) {
			for (Node n : node) {
				AbstractNode child = (AbstractNode)n;
				if (child != null) {
					parent.adopt(child);
					fixEscaped();
					list.add(0, child);
				}
			}
			return returnAsParent;
		}
		
		@Override
		public P addToEnd(Node... node) {
			for (Node n : node) {
				AbstractNode child = (AbstractNode)n;
				if (child != null) {
					parent.adopt(child);
					fixEscaped();
					list.add(child);
				}
			}
			return returnAsParent;
		}
		
		@Override
		public P addBefore(Node ref, Node... node) {
			if (ref == null) throw new NullPointerException("ref");
			parent.ensureParentage((AbstractNode)ref);
			
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == ref) {
					int j = 0;
					for (Node n : node) {
						AbstractNode child = (AbstractNode)n;
						if (child != null) {
							child.ensureParentless();
							parent.adopt(child);
							fixEscaped();
							list.add(i + j, child);
							j++;
						}
					}
					return returnAsParent;
				}
			}
			throw new IllegalStateException(listName + " does not contain: " + ref);
		}
		
		@Override
		public P addAfter(Node ref, Node... node) {
			if (ref == null) throw new NullPointerException("ref");
			parent.ensureParentage((AbstractNode)ref);
			
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == ref) {
					int j = 0;
					for (Node n : node) {
						AbstractNode child = (AbstractNode)n;
						if (child != null) {
							child.ensureParentless();
							parent.adopt(child);
							fixEscaped();
							list.add(i + j + 1, child);
							j++;
						}
					}
					return returnAsParent;
				}
			}
			
			throw new IllegalStateException(listName + " does not contain: " + ref);
		}
		
		@Override
		public boolean replace(Node source, Node replacement) throws NoSuchElementException {
			if (source == null) return false;
			if (source.getParent() != parent) return false;
			if (replacement != null) ((AbstractNode)replacement).ensureParentless();
			
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == source) {
					parent.disown((AbstractNode)source);
					try {
						if (replacement != null) parent.adopt((AbstractNode)replacement);
					} catch (IllegalStateException e) {
						parent.adopt((AbstractNode)source);
						throw e;
					}
					fixEscaped();
					if (replacement == null) list.remove(i);	//screws up for counter, but we return right after anyway, so it doesn't matter.
					else list.set(i, (AbstractNode)replacement);
					return true;
				}
			}
			
			return false;
		}
		
		@Override
		public boolean remove(Node source) {
			if (source == null) return false;
			if (source.getParent() != parent) return false;
			
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) == source) {
					parent.disown((AbstractNode)source);
					fixEscaped();
					list.remove(i);
					return true;
				}
			}
			
			return false;
		}
		
		@Override
		public StrictListAccessor<T, P> asStrictAccessor() {
			return strict;
		}
		
		@Override public Iterator<Node> iterator() {
			final Iterator<AbstractNode> it = list.iterator();
			escaped = true;
			return new Iterator<Node>() {
				@Override public boolean hasNext() {
					return it.hasNext();
				}
				
				@Override public Node next() {
					return it.next();
				}
				
				@Override public void remove() {
					throw new UnsupportedOperationException("Iterator is read only");
				}
			};
		}
	};
	
	private StrictListAccessor<T, P> strict = new StrictListAccessor<T, P>() {
		@Override public P up() {
			return returnAsParent;
		}
		
		@Override public Node owner() {
			return parent;
		}
		
		@Override public void clear() {
			if (escaped) {
				list = new ArrayList<AbstractNode>();
				escaped = false;
			} else {
				list.clear();
			}
		}
		
		@Override public boolean isEmpty() {
			return list.isEmpty();
		}
		
		@Override public int size() {
			return list.size();
		}
		
		@Override public T first() {
			Node r = raw.first();
			if (!tClass.isInstance(r)) return null;
			return tClass.cast(r);
		}
		
		@Override public T last() {
			Node r = raw.last();
			if (!tClass.isInstance(r)) return null;
			return tClass.cast(r);
		}
		
		@Override public boolean contains(Node source) {
			return raw.contains(source);
		}
		
		@Override public P migrateAllFrom(StrictListAccessor<? extends T, ?> otherList) {
			while (!otherList.isEmpty()) {
				AbstractNode n = (AbstractNode) otherList.first();
				otherList.remove(n);
				raw.addToEnd(n);
			}
			
			return returnAsParent;
		}
		
		@Override public P addToStart(T... node) {
			return raw.addToStart(node);
		}
		
		@Override public P addToEnd(T... node) {
			return raw.addToEnd(node);
		}
		
		@Override public P addBefore(Node ref, T... node) {
			return raw.addBefore(ref, node);
		}
		
		@Override public P addAfter(Node ref, T... node) {
			return raw.addAfter(ref, node);
		}
		
		@Override public void replace(Node source, T replacement) throws NoSuchElementException {
			if (source == null) throw new NullPointerException();
			if (source.getParent() != parent) throw new NoSuchElementException(listName + " is not the parent of: " + source);
			
			if (!raw.replace(source, replacement)) {
				throw new NoSuchElementException(listName + " does not contain: " + source);
			}
		}
		
		@Override public void remove(Node source) throws NoSuchElementException {
			if (source == null) throw new NullPointerException();
			if (source.getParent() != parent) throw new NoSuchElementException(listName + " is not the parent of: " + source);
			
			if (!raw.remove(source)) {
				throw new NoSuchElementException(listName + " does not contain: " + source);
			}
		}
		
		@Override public RawListAccessor<T, P> asRawAccessor() {
			return raw;
		}
		
		@Override public Iterator<T> iterator() {
			final Iterator<AbstractNode> it = list.iterator();
			escaped = true;
			
			return new Iterator<T>() {
				T next = null;
				
				{
					advance();
				}
				
				private void advance() {
					while (it.hasNext()) {
						Node potential = it.next();
						if (tClass.isInstance(potential)) {
							next = tClass.cast(potential);
							return;
						}
					}
					next = null;
				}
				
				@Override public boolean hasNext() {
					return next != null;
				}
				
				@Override public T next() {
					if (next == null) throw new NoSuchElementException("No more elements");
					T node = next;
					advance();
					return node;
				}
				
				@Override public void remove() {
					throw new UnsupportedOperationException("Iterator is read only");
				}
			};
		}
	};
	
	static <T extends Node, P extends AbstractNode> ListAccessor<T, P> of(P parent, Class<T> tClass, String listName) {
		return new ListAccessor<T, P>(parent, tClass, listName, parent);
	}
	
	<Q extends Node> ListAccessor<T, Q> wrap(final Q returnThisAsParent) {
		final ListAccessor<T, P> original = this;
		
		return new ListAccessor<T, Q>(parent, tClass, listName, returnThisAsParent) {
			final RawListAccessor<T, Q> raw = 				new RawListAccessor<T, Q>() {
				final RawListAccessor<T, P> orig = original.asRaw();
				
				@Override public Iterator<Node> iterator() {
					return orig.iterator();
				}
				
				@Override public Q up() {
					return returnThisAsParent;
				}
				
				@Override public Node owner() {
					return orig.owner();
				}
				
				@Override public void clear() {
					orig.clear();
				}
				
				@Override public boolean isEmpty() {
					return orig.isEmpty();
				}
				
				@Override public int size() {
					return orig.size();
				}
				
				@Override public Node first() {
					return orig.first();
				}
				
				@Override public Node last() {
					return orig.last();
				}
				
				@Override public boolean contains(Node source) {
					return orig.contains(source);
				}
				
				@Override public Q migrateAllFrom(RawListAccessor<?, ?> otherList) {
					orig.migrateAllFrom(otherList);
					return returnThisAsParent;
				}
				
				@Override public Q addToStart(Node... node) {
					orig.addToStart(node);
					return returnThisAsParent;
				}
				
				@Override public Q addToEnd(Node... node) {
					orig.addToEnd(node);
					return returnThisAsParent;
				}
				
				@Override public Q addBefore(Node ref, Node... node) {
					orig.addBefore(ref, node);
					return returnThisAsParent;
				}
				
				@Override public Q addAfter(Node ref, Node... node) {
					orig.addAfter(ref, node);
					return returnThisAsParent;
				}
				
				@Override public boolean replace(Node source, Node replacement) {
					return orig.replace(source, replacement);
				}
				
				@Override public boolean remove(Node source) {
					return orig.remove(source);
				}
				
				@Override public StrictListAccessor<T, Q> asStrictAccessor() {
					return asStrict();
				}
			};
			
			final StrictListAccessor<T, Q> strict = new StrictListAccessor<T, Q>() {
				final StrictListAccessor<T, P> orig = original.asStrict();
				
				@Override public Iterator<T> iterator() {
					return orig.iterator();
				}
				
				@Override public Q up() {
					return returnThisAsParent;
				}
				
				@Override public Node owner() {
					return orig.owner();
				}
				
				@Override public void clear() {
					orig.clear();
				}
				
				@Override public boolean isEmpty() {
					return orig.isEmpty();
				}
				
				@Override public int size() {
					return orig.size();
				}
				
				@Override public T first() {
					return orig.first();
				}
				
				@Override public T last() {
					return orig.last();
				}
				
				@Override public boolean contains(Node source) {
					return orig.contains(source);
				}
				
				@Override public Q migrateAllFrom(StrictListAccessor<? extends T, ?> otherList) {
					orig.migrateAllFrom(otherList);
					return returnThisAsParent;
				}
				
				@Override public Q addToStart(T... node) {
					orig.addToStart(node);
					return returnThisAsParent;
				}
				
				@Override public Q addToEnd(T... node) {
					orig.addToEnd(node);
					return returnThisAsParent;
				}
				
				@Override public Q addBefore(Node ref, T... node) {
					orig.addBefore(ref, node);
					return returnThisAsParent;
				}
				
				@Override public Q addAfter(Node ref, T... node) {
					orig.addAfter(ref, node);
					return returnThisAsParent;
				}
				
				@Override public void replace(Node source, T replacement) throws NoSuchElementException {
					orig.replace(source, replacement);
				}
				
				@Override public void remove(Node source) throws NoSuchElementException {
					orig.remove(source);
				}
				
				@Override public RawListAccessor<T, Q> asRawAccessor() {
					return asRaw();
				}
			};
			
			@Override <Q2 extends Node> ListAccessor<T, Q2> wrap(Q2 returnThisAsParent) {
				return original.wrap(returnThisAsParent);
			}
			
			@Override StrictListAccessor<T, Q> asStrict() {
				return strict;
			}
			
			@Override RawListAccessor<T, Q> asRaw() {
				return raw;
			}
			
			@Override Iterable<AbstractNode> asIterable() {
				return original.asIterable();
			}
		};
	}
	
	StrictListAccessor<T, P> asStrict() {
		return strict;
	}
	
	RawListAccessor<T, P> asRaw() {
		return raw;
	}
	
	Iterable<AbstractNode> asIterable() {
		escaped = true;
		return list;
	}
	
	List<AbstractNode> backingList() {
		return list;
	}
}
