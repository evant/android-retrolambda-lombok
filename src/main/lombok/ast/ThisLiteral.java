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
