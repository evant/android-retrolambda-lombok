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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lombok.ast node objects built via conversion from other ast APIs can have associated position info which
 * is useful for conversion back to the original AST.
 */
public class ConversionPositionInfo {
	private static WeakHashMap<Node, Map<String, Position>> store = new WeakHashMap<Node, Map<String, Position>>();
	
	public static void setConversionPositionInfo(Node on, String key, Position position) {
		if (on instanceof AbstractNode) {
			((AbstractNode) on).addConversionPositionInfo(key, position);
		} else {
			synchronized (store) {
				Map<String, Position> map = store.get(on);
				if (map == null) {
					map = new HashMap<String, Position>();
					store.put(on, map);
				}
				map.put(key, position);
			}
		}
	}
	
	public static Position getConversionPositionInfo(Node on, String key) {
		if (on instanceof AbstractNode) {
			return ((AbstractNode) on).getConversionPositionInfo(key);
		} else {
			synchronized (store) {
				Map<String, Position> map = store.get(on);
				if (map == null) return null;
				return map.get(key);
			}
		}
	}
}
