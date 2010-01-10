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

public class StringLiteral extends Expression implements Literal {
	private String value;
	private String rawValue;
	private String errorReason = "Missing value";
	
	public StringLiteral setValue(String value) {
		if (value == null) throw new AstException(this, "string value is mandatory");
		this.value = value;
		StringBuilder raw = new StringBuilder().append('"');
		char[] cs = value.toCharArray();
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];
			char next = (i < cs.length-1) ? cs[i+1] : 'a';
			raw.append(CharLiteral.toEscape(c, false, next));
		}
		this.rawValue = raw.append('"').toString();
		this.errorReason = null;
		
		return this;
	}
	
	public StringLiteral setRawValue(String raw) {
		setRawValue0(raw);
		System.out.println("RAWVALUE SET: " + this.value + "(" + this.errorReason + ")");
		return this;
	}
	
	
	public StringLiteral setRawValue0(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReason = "Missing value";
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.errorReason = null;
			this.value = null;
			
			if (!v.startsWith("\"") || !v.endsWith("\"")) {
				this.errorReason = "String literals should be enclosed in double quotes: " + v;
				return this;
			}
			
			String content = v.substring(1, v.length()-1);
			char[] cs = content.toCharArray();
			StringBuilder value = new StringBuilder();
			
			for (int i = 0; i < cs.length; i++) {
				if (cs[i] == '\n' || cs[i] == '\r') {
					this.errorReason = "newlines not allowed in string literal: " + v;
					return this;
				}
				
				if (cs[i] == '"') {
					this.errorReason = "unescaped double quotes not allowed in string literal: " +v;
					return this;
				}
				
				if (cs[i] == '\\') {
					if (i == v.length() -1) {
						this.errorReason = "Incomplete backslash escape: " + v;
						return this;
					}
					char x = cs[++i];
					char fromEscape = CharLiteral.fromEscape(x);
					if (fromEscape != 0) {
						value.append(fromEscape);
						continue;
					}
					
					if (x >= '0' && x <= '7') {
						char first = x;
						char second = (i < cs.length -1) ? cs[i+1] : 'a';
						char third = (i < cs.length -2) ? cs[i+2] : 'a';
						
						boolean secondFits = second >= '0' && second <= '7';
						boolean thirdFits = second >= '0' && second <= '7';
						
						if (first > '3') {
							if (secondFits) {
								i++;
								value.append((first - '0') * 010 + (second - '0'));
								continue;
							}
							value.append(first - '0');
							continue;
						}
						
						if (secondFits && thirdFits) {
							i += 2;
							value.append((first - '0') * 0100 + (second - '0') * 010 + (third - '0'));
							continue;
						}
						
						if (secondFits) {
							i++;
							value.append((first - '0') * 010 + (second - '0'));
							continue;
						}
						
						value.append(first - '0');
						continue;
					}
					
					this.errorReason = "Invalid string literal (invalid backslash escape): " + v;
					return this;
				}
				
				value.append(cs[i]);
			}
			this.value = value.toString();
		}
		
		return this;
	}
	
	public String getValue() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed string literal(%s): %s", errorReason, rawValue));
		return value;
	}
	
	public String getRawValue() {
		return rawValue;
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitStringLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
