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

import lombok.Getter;

public class IntegralLiteral extends AbstractNode implements Literal, Expression, DescribedNode {
	private Long value;
	private String rawValue;
	private String errorReasonForValue = "Missing value";
	@Getter private boolean markedAsLong;
	@Getter private LiteralType literalType = LiteralType.DECIMAL;
	private int parens;
	
	public IntegralLiteral setParens(int parens) {
		this.parens = parens;
		return this;
	}
	
	public int getParens() {
		return this.parens;
	}
	
	public int getIntendedParens() {
		return this.parens;
	}
	
	public boolean needsParentheses() {
		return false;
	}
	
	@Override public String getDescription() {
		return value != null ? String.valueOf(value) : null;
	}
	
	public String getErrorReasonForValue() {
		return errorReasonForValue;
	}
	
	public IntegralLiteral setLiteralType(LiteralType type) {
		if (type == null) throw new NullPointerException("type");
		this.literalType = type;
		updateRawValue();
		
		return this;
	}
	
	@Override public IntegralLiteral copy() {
		IntegralLiteral result = new IntegralLiteral();
		result.value = value;
		result.rawValue = rawValue;
		result.errorReasonForValue = errorReasonForValue;
		result.markedAsLong = markedAsLong;
		result.literalType = literalType;
		return result;
	}
	
	public IntegralLiteral setIntValue(int value) {
		if (value < 0) throw new AstException(this, "Integral literals cannot be negative; wrap a literal in a UnaryExpression to accomplish this");
		this.value = Long.valueOf(value);
		this.rawValue = "" + value;
		this.errorReasonForValue = null;
		this.markedAsLong = false;
		updateRawValue();
		return this;
	}
	
	public IntegralLiteral setLongValue(long value) {
		if (value < 0) throw new AstException(this, "Integral literals cannot be negative; wrap a literal in a UnaryExpression to accomplish this");
		this.value = value;
		this.rawValue = "" + value + "L";
		this.errorReasonForValue = null;
		this.markedAsLong = true;
		updateRawValue();
		return this;
	}
	
	private void updateRawValue() {
		if (errorReasonForValue != null) return;
		String suffix = markedAsLong ? "L" : "";
		
		switch (literalType) {
		case DECIMAL:
			rawValue = value + suffix;
			break;
		case HEXADECIMAL:
			rawValue = "0x" + Long.toString(value, 0x10) + suffix;
			break;
		case OCTAL:
			rawValue = "0" + Long.toString(value, 010) + suffix;
			break;
		default:
			assert false: "literalType is null";
		}
	}
	
	public IntegralLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReasonForValue = "Missing value";
			this.markedAsLong = false;
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.markedAsLong = v.endsWith("L") || v.endsWith("l");
			v = markedAsLong ? raw.substring(0, raw.length()-1) : raw;
			LiteralType newLT;
			try {
				if (v.startsWith("0x")) {
					this.value = Long.parseLong(v.substring(2), 0x10);
					newLT = LiteralType.HEXADECIMAL;
				} else if (v.equals("0")) {
					this.value = 0L;
					newLT = LiteralType.DECIMAL;
				} else if (v.startsWith("0")) {
					this.value = Long.parseLong(v, 010);	//010 = octal 8.
					newLT = LiteralType.OCTAL;
				} else {
					this.value = Long.parseLong(v, 10);
					newLT = LiteralType.DECIMAL;
				}
				if (!markedAsLong && (this.value.longValue() != this.value.intValue())) {
					this.errorReasonForValue = "value too large to fit in 'int' type; add a suffix 'L' to fix this.";
				} else {
					this.errorReasonForValue = null;
				}
				this.literalType = newLT;
			} catch (NumberFormatException e) {
				this.value = null;
				this.errorReasonForValue = "Not a valid integral literal: " + raw.trim();
			}
		}
		
		return this;
	}
	
	public long longValue() throws AstException {
		checkValueExists();
		return value;
	}
	
	public int intValue() throws AstException {
		checkValueExists();
		if (value.longValue() != value.intValue()) throw new AstException(this, String.format("integral literal doesn't fit in 'int' type: %s", rawValue));
		return value.intValue();
	}
	
	@Override public String getRawValue() {
		return rawValue;
	}
	
	private void checkValueExists() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed integral literal(%s): %s", errorReasonForValue, rawValue));
	}
	
	@Override public void accept(AstVisitor visitor) {
		visitor.visitIntegralLiteral(this);
	}
}
