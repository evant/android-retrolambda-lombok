package lombok.ast;

public enum UnaryOperator {
	BINARY_NOT("~"), LOGICAL_NOT("!"), UNARY_PLUS("+"), UNARY_MINUS("-");
	
	private final String symbol;
	
	UnaryOperator(String symbol) {
		this.symbol = symbol;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public static UnaryOperator fromSymbol(String symbol) {
		for (UnaryOperator op : values()) {
			if (op.symbol.equals(symbol)) return op;
		}
		
		return null;
	}
}