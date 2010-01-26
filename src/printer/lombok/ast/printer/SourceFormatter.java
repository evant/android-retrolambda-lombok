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
package lombok.ast.printer;

import java.io.IOException;

import lombok.ast.Node;

import org.parboiled.support.InputLocation;

public interface SourceFormatter {
	
	public static final char FAIL = '‽';
	
	public abstract void reportAssertionFailureNext(Node node, String message, Throwable error);
	
	public abstract void fail(String fail);
	
	public abstract void keyword(String text);
	
	public abstract void operator(String text);
	
	public abstract void verticalSpace();
	
	public abstract void space();
	
	public abstract void append(String text);
	
	public abstract void buildInline(Node node);
	
	public abstract void closeInline();
	
	public abstract void startSuppressBlock();
	
	public abstract void endSuppressBlock();
	
	public abstract void buildBlock(Node node);
	
	public abstract void closeBlock();
	
	public abstract void addError(InputLocation errorStart, InputLocation errorEnd, String errorMessage);
	
	public abstract String finish() throws IOException;
	
	public abstract void setTimeTaken(long taken);
	
}
