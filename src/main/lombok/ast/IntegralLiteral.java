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

import java.math.BigInteger;

public class IntegralLiteral extends AbstractNode.WithParens implements Literal, Expression, DescribedNode {
	private static final String NEGATIVE_NUMBERS_NOT_POSSIBLE = "Negative integral literals don't exist; wrap in a UnaryExpression with operator MINUS";
	
	private Long value;
	private String rawValue;
	private String errorReasonForValue = "Missing value";
	private boolean markedAsLong;
	private LiteralType literalType = LiteralType.DECIMAL;
	
	@Override
	public boolean isStatementExpression() {
		return false;
	}
	
	@Override
	public String getDescription() {
		return value != null ? String.valueOf(value) : null;
	}
	
	public String getErrorReasonForValue() {
		if (errorReasonForValue != null) return errorReasonForValue;
		long v = value;
		if (markedAsLong) {
			if (literalType != LiteralType.DECIMAL) return null;
			if (v >= 0) return null;
			if (v == Long.MIN_VALUE) {
				return containedInUnaryMinus() ? null : "Long literal too large: " + rawValue;
			}
			return NEGATIVE_NUMBERS_NOT_POSSIBLE;
		} else {
			if ((v & 0xFFFFFFFF00000000L) != 0) return "Int literal too large: " + rawValue;
			if (literalType != LiteralType.DECIMAL) return null;
			if (v <= Integer.MAX_VALUE) return null;
			if (v == 1L + Integer.MAX_VALUE) {
				return containedInUnaryMinus() ? null : "Int literal too large: " + rawValue;
			}
			return NEGATIVE_NUMBERS_NOT_POSSIBLE;
		}
	}
	
	private boolean containedInUnaryMinus() {
		return getParens() == 0 && getParent() instanceof UnaryExpression &&
				((UnaryExpression)getParent()).astOperator() == UnaryOperator.UNARY_MINUS;
	}
	
	public static Expression ofInt(int value) {
		IntegralLiteral v = new IntegralLiteral();
		if (value < 0) {
			return new UnaryExpression().astOperator(UnaryOperator.UNARY_MINUS).astOperand(v.astIntValue(-value));
		}
		return v.astIntValue(value);
	}
	
	public static Expression ofLong(long value) {
		IntegralLiteral v = new IntegralLiteral();
		if (value < 0) {
			return new UnaryExpression().astOperator(UnaryOperator.UNARY_MINUS).astOperand(v.astLongValue(-value));
		}
		return v.astLongValue(value);
	}
	
	public LiteralType astLiteralType() {
		return literalType;
	}
	
	public IntegralLiteral astLiteralType(LiteralType type) {
		if (type == null) throw new NullPointerException("type");
		this.literalType = type;
		updateRawValue();
		
		return this;
	}
	
	public boolean astMarkedAsLong() {
		return markedAsLong;
	}
	
	public IntegralLiteral astMarkedAsLong(boolean marked) {
		this.markedAsLong = marked;
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
	
	
	public IntegralLiteral astIntValue(int value) {
		this.value = value & 0xFFFFFFFFL;	//Suppress sign extension.
		this.rawValue = "" + value;
		this.errorReasonForValue = null;
		this.markedAsLong = false;
		updateRawValue();
		return this;
	}
	
	public IntegralLiteral astLongValue(long value) {
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
		
		StringBuilder out;
		boolean nulls;
		int nibbleCounter;
		
		switch (literalType) {
		case DECIMAL:
			rawValue = (value < 0 ? String.valueOf(value).substring(1) : value) + suffix;
			break;
		case HEXADECIMAL:
			out = new StringBuilder(19);
			out.append("0x");
			nulls = true;
			nibbleCounter = markedAsLong ? 60 : 28;
			for (; nibbleCounter >= 0 ; nibbleCounter -= 4) {
				int nibble = (int)(value >>> nibbleCounter) & 0xF;
				if (nulls && nibble == 0 && nibbleCounter != 0) continue;
				nulls = false;
				out.append((char)(nibble < 10 ? '0' + nibble : 'a' - 10 + nibble));
			}
			out.append(suffix);
			this.rawValue = out.toString();
			break;
		case OCTAL:
			out = new StringBuilder(25);
			out.append("0");
			nulls = true;
			if (markedAsLong) {
				if ((value & 01000000000000000000000L) != 0) {
					out.append("1");
					nulls = false;
				}
				nibbleCounter = 60;
			} else {
				int halfNibble = ((int)(value >>> 30)) & 03;
				if (halfNibble != 0) {
					out.append((char)('0' + halfNibble));
					nulls = false;
				}
				nibbleCounter = 27;
			}
			for (; nibbleCounter >= 0 ; nibbleCounter -= 3) {
				int nibble = (int)(value >>> nibbleCounter) & 07;
				if (nulls && nibble == 0 && nibbleCounter != 0) continue;
				nulls = false;
				out.append((char)('0' + nibble));
			}
			out.append(suffix);
			this.rawValue = out.toString();
			break;
		default:
			assert false: "literalType is null";
		}
	}
	
	public IntegralLiteral rawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReasonForValue = "Missing value";
			this.markedAsLong = false;
			return this;
		}
		
		this.rawValue = raw;
		this.value = null;
		this.errorReasonForValue = null;
		this.markedAsLong = false;
		String v = raw.trim();
		
		if (v.startsWith("-")) {
			this.errorReasonForValue = NEGATIVE_NUMBERS_NOT_POSSIBLE;
			return this;
		}
		
		boolean markedAsLong = v.endsWith("L") || v.endsWith("l");
		v = markedAsLong ? raw.substring(0, raw.length()-1) : raw;
		LiteralType newLT;
		int radix;
		int prefix;
		if (v.startsWith("0x")) {
			newLT = LiteralType.HEXADECIMAL;
			radix = 0x10;
			prefix = 2;
		} else if (v.startsWith("0") && v.length() > 1) {
			newLT = LiteralType.OCTAL;
			radix = 010;
			prefix = 1;
		} else {
			newLT = LiteralType.DECIMAL;
			radix = 10;
			prefix = 0;
		}
		
		long v1 = 0;
		BigInteger v2 = null;
		
		try {
			v1 = Long.parseLong(v.substring(prefix), radix);
		} catch (NumberFormatException e) {
			try {
				v2 = new BigInteger(v.substring(prefix), radix);
			} catch (NumberFormatException e2) {
				this.value = null;
				this.errorReasonForValue = "Not a valid integral literal: " + v;
				return this;
			}
		}
		
		Object result = setRawValue0(markedAsLong, v1, v2);
		if (result instanceof Long) {
			this.markedAsLong = markedAsLong;
			this.literalType = newLT;
			this.value = (Long)result;
		} else {
			this.errorReasonForValue = ((String)result) + v;
		}
		
		return this;
	}
	
	private static final BigInteger MAX_UNSIGNED_LONG = new BigInteger("FFFFFFFFFFFFFFFF", 0x10);
	
	private static Object setRawValue0(boolean markedAsLong, long v1, BigInteger v2) {
		if (v2 == null) { //Parsed number fits in standard long.
			return v1;
		} else { //Number only fits as a BigDecimal
			//There aren't many legal options here; it would have to be an unsigned format (hex/oct), and a long.
			if (!markedAsLong) return "Int Literal above maximum value: ";
			
			if (v2.compareTo(MAX_UNSIGNED_LONG) <= 0) return v2.longValue();
			
			return "Long literal too large: ";
		}
	}
	
	public long astLongValue() throws AstException {
		return value == null ? 0L : value.longValue();
	}
	
	public int astIntValue() throws AstException {
		return value == null ? 0 : value.intValue();
	}
	
	@Override
	public String rawValue() {
		return rawValue;
	}
	
	@Override
	public void accept(AstVisitor visitor) {
		if (!visitor.visitIntegralLiteral(this)) visitor.endVisit(this);
		visitor.afterVisitIntegralLiteral(this);
	}
}
