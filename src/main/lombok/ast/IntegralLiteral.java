package lombok.ast;

import java.util.List;

import lombok.Getter;

public class IntegralLiteral extends Expression implements Literal {
	private Long value;
	private String rawValue;
	private String errorReason = "Missing value";
	@Getter private boolean markedAsLong;
	
	public IntegralLiteral setIntValue(int value) {
		this.value = Long.valueOf(value);
		this.rawValue = "" + value;
		this.errorReason = null;
		this.markedAsLong = false;
		return this;
	}
	
	public IntegralLiteral setLongValue(long value) {
		this.value = value;
		this.rawValue = "" + value + "L";
		this.errorReason = null;
		this.markedAsLong = true;
		return this;
	}
	
	public IntegralLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReason = "Missing value";
			this.markedAsLong = false;
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.markedAsLong = v.endsWith("L") || v.endsWith("l");
			v = markedAsLong ? raw.substring(0, raw.length()-1) : raw;
			try {
				if (v.startsWith("0x")) {
					this.value = Long.parseLong(v.substring(2), 0x10);
				} else if (v.startsWith("0")) {
					this.value = Long.parseLong(v, 010);	//010 = octal 8.
				} else {
					this.value = Long.parseLong(v, 10);
				}
				if (!markedAsLong && (this.value.longValue() != this.value.intValue())) {
					this.errorReason = "value too large to fit in 'int' type; add a suffix 'L' to fix this.";
				} else {
					this.errorReason = null;
				}
			} catch (NumberFormatException e) {
				this.value = null;
				this.errorReason = "Not a valid integral literal: " + raw.trim();
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
	
	public String getRawValue() {
		return rawValue;
	}
	
	private void checkValueExists() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed integral literal(%s): %s", errorReason, rawValue));
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitIntegralLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
