package lombok.ast;

import java.util.List;

public class UnaryExpression extends Expression {
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
	
	private Node operand;
	private String operatorRaw;
	private UnaryOperator operator;
	private String errorReasonForOperator = "missing operator";
	
	public Expression getOperand() {
		assertChildType(operand, "operand", true, Expression.class);
		return (Expression) operand;
	}
	
	public Node getRawOperand() {
		return operand;
	}
	
	public UnaryExpression setOperand(Expression operand) {
		if (operand == null) throw new NullPointerException("operand is mandatory");
		this.operand = operand;
		return this;
	}
	
	public UnaryExpression setRawOperand(Node operand) {
		this.operand = operand;
		return this;
	}
	
	public UnaryExpression setOperator(UnaryOperator operator) {
		if (operator == null) throw new NullPointerException("operator is mandatory");
		this.errorReasonForOperator = null;
		this.operator = operator;
		this.operatorRaw = operator.getSymbol();
		
		return this;
	}
	
	public UnaryOperator getOperator() {
		if (operator == null) throw new AstException(this, errorReasonForOperator);
		return operator;
	}
	
	public UnaryExpression setRawOperator(String operator) {
		this.errorReasonForOperator = null;
		this.operator = null;
		this.operatorRaw = operator;
		
		if (operator == null) {
			this.errorReasonForOperator = "missing operator";
			return this;
		}
		
		this.operator = UnaryOperator.fromSymbol(operator.trim());
		if (this.operator != null) return this;
		
		this.errorReasonForOperator = "unknown unary operator: " + operator.trim();
		return this;
	}
	
	public String getRawOperator() {
		return operatorRaw;
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReasonForOperator != null) problems.add(new SyntaxProblem(this, errorReasonForOperator));
		checkChildValidity(problems, this.operand, "operand", true, Expression.class);
	}
	
	@Override public void accept(ASTVisitor visitor) {
		if (visitor.visitUnaryExpression(this)) return;
		if (this.operand != null) this.operand.accept(visitor);
	}
}
