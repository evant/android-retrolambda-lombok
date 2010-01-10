package lombok.ast;

import java.util.List;

public class BooleanLiteral extends Expression implements Literal {
	private Boolean value;
	private String rawValue;
	private String errorReason = "Missing value";
	
	public BooleanLiteral setValue(boolean value) {
		this.value = value;
		this.rawValue = "" + value;
		this.errorReason = null;
		
		return this;
	}
	
	public BooleanLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReason = "Missing value";
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.errorReason = null;
			if (v.equals("true")) this.value = true;
			else if (v.equals("false")) this.value = false;
			else {
				this.value = null;
				this.errorReason = "Not a boolean value: " + v;
			}
		}
		
		return this;
	}
	
	public boolean getValue() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed boolean literal(%s): %s", errorReason, rawValue));
		return value;
	}
	
	public String getRawValue() {
		return rawValue;
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitBooleanLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
