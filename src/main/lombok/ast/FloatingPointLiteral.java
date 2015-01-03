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

public class FloatingPointLiteral extends AbstractNode.WithParens implements Literal, Expression, DescribedNode {
	private Double value;
	private String rawValue;
	private String errorReasonForValue = "Missing value";
	private boolean markedAsFloat;
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
		return errorReasonForValue;
	}
	
	public LiteralType astLiteralType() {
		return literalType;
	}
	
	public FloatingPointLiteral astLiteralType(LiteralType type) {
		if (type == null) throw new NullPointerException("type");
		if (type == LiteralType.OCTAL) throw new IllegalArgumentException("there's no such thing as an octal floating point literal");
		this.literalType = type;
		updateRawValue();
		
		return this;
	}
	
	public boolean astMarkedAsFloat() {
		return markedAsFloat;
	}
	
	public FloatingPointLiteral astMarkedAsFloat(boolean marked) {
		this.markedAsFloat = marked;
		updateRawValue();
		
		return this;
	}
	
	@Override
	public FloatingPointLiteral copy() {
		FloatingPointLiteral result = new FloatingPointLiteral();
		result.value = value;
		result.rawValue = result.rawValue;
		result.errorReasonForValue = result.errorReasonForValue;
		result.markedAsFloat = result.markedAsFloat;
		result.literalType = literalType;
		return result;
	}
	
	public FloatingPointLiteral astDoubleValue(double value) {
		checkSpecialValues(value);
		this.markedAsFloat = false;
		this.value = value;
		this.errorReasonForValue = null;
		updateRawValue();
		return this;
	}
	
	public FloatingPointLiteral astFloatValue(float value) {
		checkSpecialValues(value);
		this.markedAsFloat = true;
		this.errorReasonForValue = null;
		this.value = Double.valueOf(value);
		updateRawValue();
		return this;
	}
	
	private void checkSpecialValues(double value) throws AstException {
		if (Double.isNaN(value)) throw new AstException(this, "NaN cannot be expressed as a floating point literal");
		if (Double.isInfinite(value)) throw new AstException(this, "Infinity cannot be expressed as a floating point literal");
		if ((Double.doubleToRawLongBits(value) & 0x8000000000000000L) != 0) throw new AstException(this,
				"Floating Point literals cannot be negative; wrap a literal in a UnaryExpression to accomplish this");
	}
	
	private void updateRawValue() {
		if (errorReasonForValue != null) return;
		String suffix = markedAsFloat ? "F" : "";
		
		switch (literalType) {
		case DECIMAL:
			rawValue = value + suffix;
			break;
		case HEXADECIMAL:
			rawValue = Double.toHexString(value) + suffix;
			break;
		default:
			assert false: "literalType is null / octal";
		}
	}
	
	public FloatingPointLiteral rawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReasonForValue = "Missing value";
			this.markedAsFloat = false;
		} else {
			this.rawValue = raw;
			this.errorReasonForValue = null;
			String v = raw.trim();
			this.markedAsFloat = v.endsWith("F") || v.endsWith("f");
			v = (markedAsFloat || v.endsWith("D") || v.endsWith("d")) ? raw.substring(0, raw.length()-1) : raw;
			if (v.startsWith("-")) {
				this.errorReasonForValue = "Floating Point literals can't start with -; wrap them in a UnaryExpression: " + v;
				this.value = null;
				return this;
			}
			try {
				//We double-checked the code - Double.parseDouble will parse exactly everything that is legal according to the JLS!
				value = Double.parseDouble(v);
				literalType = (v.startsWith("0x") || v.startsWith("0X")) ? LiteralType.HEXADECIMAL : LiteralType.DECIMAL;
			} catch (NumberFormatException e) {
				this.value = null;
				this.errorReasonForValue = "Not a valid floating point literal: " + v;
			}
		}
		
		return this;
	}
	
	public double astDoubleValue() throws AstException {
		return value == null ? 0.0D : value.doubleValue();
	}
	
	public float astFloatValue() throws AstException {
		return value == null ? 0.0F : value.floatValue();
	}
	
	@Override
	public String rawValue() {
		return rawValue;
	}
	
	@Override
	public void accept(AstVisitor visitor) {
		if (!visitor.visitFloatingPointLiteral(this)) visitor.endVisit(this);
		visitor.afterVisitFloatingPointLiteral(this);
	}
}
