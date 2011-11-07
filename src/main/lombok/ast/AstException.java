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

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public class AstException extends RuntimeException {
	private final Node problemNode;
	private final String message;
	
	public AstException(Node problemNode, String message) {
		super(message);
		this.message = message;
		this.problemNode = problemNode;
	}
	
	@Override public String toString() {
		if (problemNode == null && getMessage() == null) return "AstException (unknown cause)";
		if (problemNode == null) return "AstException: " + getMessage();
		String nodeDescription = problemNode == null ? "(null)" : (problemNode.getClass().getName() + "(toString failed)");
		try {
			nodeDescription = problemNode.toString();
		} catch (Throwable ignore) {
			//throwing exceptions in toString() is bad.
		}
		if (getMessage() == null) return "AstException at " + nodeDescription;
		return String.format("AstException: %s (at %s)", getMessage(), nodeDescription);
	}
}
