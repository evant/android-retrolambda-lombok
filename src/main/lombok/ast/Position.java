/*
 * Copyright (C) 2010 The Project Lombok Authors.
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

import lombok.Data;

/**
 * The position information which lets you find the raw characters that represent this node in a source file.
 * For generated nodes, {@code generatedBy} is set to non-{@code null} and the {@code start} and {@code end}
 * refer to the places where the node would have appeared if it wasn't generated.
 */
@Data
public final class Position {
	private final int start, end;
	private final Node generatedBy;
	
	public static Position UNPLACED = new Position(-1, -1);
	
	public Position(int start, int end) {
		this.start = start;
		this.end = end;
		this.generatedBy = null;
	}
	
	public Position(int start, int end, Node generatedBy) {
		this.start = start;
		this.end = end;
		this.generatedBy = generatedBy;
	}
	
	public int size() {
		return end - start;
	}
	
	public boolean isUnplaced() {
		return start == UNPLACED.start && end == UNPLACED.end;
	}
	
	public Position withGeneratedBy(Node generatedBy) {
		return new Position(start, end, generatedBy);
	}
	
	public Position withoutGeneratedBy() {
		return new Position(start, end);
	}
	
	public Position withEnd(int position) {
		return new Position(start, position, generatedBy);
	}
	
	public Position withStart(int position) {
		return new Position(position, end, generatedBy);
	}
}
