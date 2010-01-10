package lombok.ast;

import java.util.List;

public class CharLiteral extends Expression implements Literal {
	private Character value;
	private String rawValue;
	private String errorReason = "Missing value";
	
	static String toEscape(char c, boolean forCharLiteral, char next) {
		if (c == '\'') return forCharLiteral ? "\\'" : "'";
		if (c == '"') return forCharLiteral ? "\"" : "\\\"";
		if (c == '\b') return "\\b";
		if (c == '\t') return "\\t";
		if (c == '\n') return "\\n";
		if (c == '\f') return "\\f";
		if (c == '\r') return "\\r";
		if (c == '\\') return "\\\\";
		if (c < 0x20 || c == 127) {
			String octalEscape = Integer.toString(c, 010);
			boolean fill = (next >= '0' && next <= '7') && octalEscape.length() < 3;
			while (fill && octalEscape.length() < 3) octalEscape = "0" + octalEscape;
			return "\\" + octalEscape;
		}
		return "" + c;
	}
	
	static char fromEscape(char x) {
		if (x == 'b') return '\b';
		if (x == 't') return '\t';
		if (x == 'n') return '\n';
		if (x == 'f') return '\f';
		if (x == 'r') return '\r';
		if (x == '\'') return '\'';
		if (x == '"') return '"';
		if (x == '\\') return '\\';
		return 0;
	}
	
	public CharLiteral setValue(char value) {
		this.value = value;
		this.rawValue = "'" + toEscape(value, true, 'a');
		this.errorReason = null;
		
		return this;
	}
	
	public CharLiteral setRawValue(String raw) {
		setRawValue0(raw);
		System.out.println("RAWVALUE SET: " + this.value);
		return this;
	}
	
	private CharLiteral setRawValue0(String raw) {
		if (raw == null) {
			this.rawValue = null;
			this.value = null;
			this.errorReason = "Missing value";
		} else {
			this.rawValue = raw;
			String v = raw.trim();
			this.errorReason = null;
			this.value = null;
			
			if (!v.startsWith("'") || !v.endsWith("'")) {
				this.errorReason = "Character literals should be enclosed in single quotes: " + v;
				return this;
			}
			
			String content = v.substring(1, v.length()-1);
			if (content.length() == 0) {
				this.errorReason = "Empty character literal is not allowed";
				return this;
			}
			
			if (content.charAt(0) == '\\') {
				if (content.length() == 1) {
					this.errorReason = "Incomplete backslash escape: '\'";
					return this;
				}
				char x = content.charAt(1);
				char fromEscape = fromEscape(x);
				if (fromEscape != 0) {
					this.value = fromEscape;
					return this;
				}
				if (x >= '0' && x <= '7') {
					try {
						int possible = Integer.parseInt(content.substring(1), 010);
						if (possible <= 0377) {
							this.value = (char)possible;
							return this;
						}
					} catch (NumberFormatException e) {
						//fallthrough
					}
				}
				
				if (this.value != null && content.length() == 2) {
					return this;
				}
				
				this.value = null;
				this.errorReason = "Not a valid character literal: " + v;
				return this;
			}
			
			if (content.length() == 1) {
				char x = content.charAt(0);
				if (x == '\'' || x == '\n' || x == '\r') {
					this.errorReason = "Not a valid character literal: " + v;
				} else {
					this.value = x;
				}
				return this;
			}
			
			this.errorReason = "Not a valid character literal: " + v;
		}
		
		return this;
	}
	
	public char getValue() throws AstException {
		if (value == null) throw new AstException(this, String.format("misformed char literal(%s): %s", errorReason, rawValue));
		return value;
	}
	
	public String getRawValue() {
		return rawValue;
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitCharLiteral(this);
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReason != null) problems.add(new SyntaxProblem(this, errorReason));
	}
}
