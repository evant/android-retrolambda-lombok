package lombok.ast;

public enum BinaryOperator {
	AND_ASSIGN("&="), XOR_ASSIGN("^="), OR_ASSIGN("|="), SHIFT_LEFT_ASSIGN("<<="), SHIFT_RIGHT_ASSIGN(">>="), BITWISE_SHIFT_RIGHT_ASSIGN(">>>="), ASSIGN("="),
	LOGICAL_OR("||"),
	LOGICAL_AND("&&"),
	BITWISE_OR("|"),
	BITWISE_XOR("^"),
	BITWISE_AND("&"),
	EQUALS("=="), NOT_EQUALS("!="),
	INSTANCEOF("instanceof"), GREATER(">"), GREATER_OR_EQUAL(">="), LESS("<"), LESS_OR_EQUAL("<="),
	SHIFT_LEFT("<<"), SHIFT_RIGHT(">>"), BITWISE_SHIFT_RIGHT(">>>"),
	PLUS("+"), MINUS("-"),
	MULTIPLY("*"), DIVIDE("/"), REMAINDER("%");
	
	private final String symbol;
	
	BinaryOperator(String symbol) {
		this.symbol = symbol;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public static BinaryOperator fromSymbol(String symbol) {
		for (BinaryOperator op : values()) {
			if (op.symbol.equals(symbol)) return op;
		}
		
		return null;
	}
}