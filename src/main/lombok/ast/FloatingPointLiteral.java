package lombok.ast;

import java.util.List;

import lombok.Getter;

public class FloatingPointLiteral extends Expression implements Literal {
	private Double value;
	private String rawValue;
	private String errorReason = "Missing value";
	@Getter private boolean markedAsFloat;
	
	public FloatingPointLiteral setDoubleValue(double value) {
		checkSpecialValues(value);
		this.markedAsFloat = false;
		this.value = value;
		this.errorReason = null;
		this.rawValue = "" + value;
		return this;
	}
	
	public FloatingPointLiteral setFloatValue(float value) {
		checkSpecialValues(value);
		this.markedAsFloat = true;
		this.errorReason = null;
		this.value = Double.valueOf(value);
		this.rawValue = "" + value + "F";
		return this;
	}
	
	private void checkSpecialValues(double value) throws AstException {
		if (Double.isNaN(value)) throw new AstException(this, "NaN cannot be expressed as a floating point literal");
		if (Double.isInfinite(value)) throw new AstException(this, "Infinity cannot be expressed as a floating point literal");
	}
	
	public FloatingPointLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReason = "Missing value";
			this.markedAsFloat = false;
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.markedAsFloat = v.endsWith("F") || v.endsWith("f");
			v = (markedAsFloat || v.endsWith("D") || v.endsWith("d")) ? raw.substring(0, raw.length()-1) : raw;
			try {
				//We double-checked the code - Double.parseDouble will parse exactly everything that is legal according to the JLS!
				value = Double.parseDouble(v);
			} catch (NumberFormatException e) {
				this.value = null;
				this.errorReason = "Not a valid integral literal: " + raw.trim();
			}
		}
		
		return this;
	}
	
	public double doubleValue() throws AstException {
		checkValueExists();
		return value;
	}
	
	public float floatValue() throws AstException {
		checkValueExists();
		return value.floatValue();
	}
	
	public String getRawValue() {
		return rawValue;
	}
	
	private void checkValueExists() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed floating point literal(%s): %s", errorReason, rawValue));
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitFloatingPointLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
