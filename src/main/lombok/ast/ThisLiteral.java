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

import java.util.List;

public class ThisLiteral extends Expression implements Literal {
	private String rawValue;
	private String errorReason = "Missing value";
	
	public ThisLiteral setAsValid() {
		this.rawValue = "this";
		this.errorReason = null;
		return this;
	}
	
	public ThisLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.errorReason = "Missing value";
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			if (!v.equals("this")) {
				this.errorReason = "Only 'this' is a valid this literal, not: " + v;
			} else {
				this.errorReason = null;
			}
		}
		
		return this;
	}
	
	public String getRawValue() {
		return rawValue;
	}
	
	public boolean isValid() {
		return errorReason == null;
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitThisLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
