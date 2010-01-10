package lombok.ast;

import java.util.List;

public class NullLiteral extends Expression implements Literal {
	private String rawValue;
	private String errorReason = "Missing value";
	
	public NullLiteral setAsValid() {
		this.rawValue = "null";
		this.errorReason = null;
		return this;
	}
	
	public NullLiteral setRawValue(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.errorReason = "Missing value";
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			if (!v.equals("null")) {
				this.errorReason = "Only 'null' is a valid null literal, not: " + v;
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
		visitor.visitNullLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
