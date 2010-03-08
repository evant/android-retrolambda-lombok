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

import java.math.BigInteger;

import lombok.Getter;

public class IntegralLiteral extends AbstractNode implements Literal, Expression, DescribedNode {
	private Long value;
	private String rawValue;
	private String errorReasonForValue = "Missing value";
	@Getter private boolean markedAsLong;
	@Getter private LiteralType literalType = LiteralType.DECIMAL;
	private int parens;
	
	@Override
	public IntegralLiteral setParens(int parens) {
		this.parens = parens;
		return this;
	}
	
	@Override
	public int getParens() {
		return this.parens;
	}
	
	@Override
	public int getIntendedParens() {
		return this.parens;
	}
	
	@Override
	public boolean needsParentheses() {
		return false;
	}
	
	@Override
	public boolean isStatementExpression() {
		return false;
	}
	
	@Override
	public String getDescription() {
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
	
	@Override
	public IntegralLiteral copy() {
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
	
	private static final BigInteger MAX_LONG = new BigInteger("FFFFFFFFFFFFFFFF", 0x10);
	private static final BigInteger MAX_INT = new BigInteger("FFFFFFFF", 0x10);
	
	public IntegralLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReasonForValue = "Missing value";
			this.markedAsLong = false;
		} else {
			this.rawValue = raw;
			this.errorReasonForValue = null;
			String v = raw.trim();
			if (v.startsWith("-")) {
				this.errorReasonForValue = "Integral literals can't start with -; wrap them in a UnaryExpression: " + v;
				this.value = null;
				return this;
			}
			this.markedAsLong = v.endsWith("L") || v.endsWith("l");
			v = markedAsLong ? raw.substring(0, raw.length()-1) : raw;
			LiteralType newLT;
			try {
				int radix;
				boolean noNegatives = false;
				int prefix;
				if (v.startsWith("0x")) {
					newLT = LiteralType.HEXADECIMAL;
					radix = 0x10;
					noNegatives = true;
					prefix = 2;
				} else if (v.startsWith("0") && v.length() > 1) {
					newLT = LiteralType.OCTAL;
					radix = 010;
					noNegatives = true;
					prefix = 1;
				} else {
					newLT = LiteralType.DECIMAL;
					radix = 10;
					prefix = 0;
				}
				
				if (this.markedAsLong && noNegatives) {
					BigInteger parsed = new BigInteger(v.substring(prefix), radix);
					if (parsed.compareTo(markedAsLong ? MAX_LONG : MAX_INT) > 0) {
						this.errorReasonForValue = (markedAsLong ? "Long" : "Int") + " Literal too large: " + v;
						this.value = null;
						return this;
					} else {
						this.value = parsed.longValue();
					}
				} else {
					this.value = Long.parseLong(v.substring(prefix), radix);
				}
				if (!markedAsLong && (this.value.longValue() != this.value.intValue())) {
					this.errorReasonForValue = "value too large to fit in 'int' type; add a suffix 'L' to fix this.";
				}
				this.literalType = newLT;
			} catch (NumberFormatException e) {
				this.value = null;
				this.errorReasonForValue = "Not a valid integral literal: " + v;
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
	
	@Override
	public String getRawValue() {
		return rawValue;
	}
	
	private void checkValueExists() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed integral literal(%s): %s", errorReasonForValue, rawValue));
	}
	
	@Override
	public void accept(AstVisitor visitor) {
		visitor.visitIntegralLiteral(this);
	}
}
