/*
 * Copyright (C) 2010-2011 The Project Lombok Authors.
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
import java.util.List;
import java.util.WeakHashMap;

/**
 * Lombok.ast node objects can have associated 'dangling nodes'. These are nodes that are parsed within or around that node which aren't supposed to be there, such as
 * statements in between 2 method declarations.
 */
public class DanglingNodes {
	private static WeakHashMap<Node, List<Node>> store = new WeakHashMap<Node, List<Node>>();
	
	public static void addDanglingNode(Node on, Node danglingNode) {
		if (on instanceof AbstractNode) {
			((AbstractNode) on).addDanglingNode(danglingNode);
		} else {
			synchronized (store) {
				List<Node> list = store.get(on);
				if (list == null) {
					list = new ArrayList<Node>();
					store.put(on, list);
				}
				list.add(danglingNode);
			}
		}
	}
	
	public static List<Node> getDanglingNodes(Node on) {
		if (on instanceof AbstractNode) {
			return ((AbstractNode) on).getDanglingNodes();
		} else {
			synchronized (store) {
				List<Node> list = store.get(on);
				if (list == null) return Collections.emptyList();
				return Collections.unmodifiableList(list);
			}
		}
	}
	
	public static void removeDanglingNode(Node on, Node danglingNode) {
		if (on instanceof AbstractNode) {
			((AbstractNode) on).removeDanglingNode(danglingNode);
		} else {
			List<Node> list = store.get(on);
			if (list != null) list.remove(danglingNode);
		}
	}
}
