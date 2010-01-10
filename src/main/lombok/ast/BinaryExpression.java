/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
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

import java.util.List;

public class BinaryExpression extends Expression {
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
	
	private Node left;
	private Node right;
	private String operatorRaw;
	private BinaryOperator operator;
	private String errorReasonForOperator = "missing operator";
	
	public Expression getLeft() {
		assertChildType(left, "left", true, Expression.class);
		return (Expression) left;
	}
	
	public Node getRawLeft() {
		return left;
	}
	
	public BinaryExpression setLeft(Expression left) {
		if (left == null) throw new NullPointerException("left is mandatory");
		this.left = left;
		return this;
	}
	
	public BinaryExpression setRawLeft(Node left) {
		this.left = left;
		return this;
	}
	
	public Expression getRight() {
		assertChildType(right, "right", true, Expression.class);
		return (Expression) right;
	}
	
	public lombok.ast.Node getRawRight() {
		return right;
	}
	
	public BinaryExpression setRight(Expression right) {
		if (right == null) throw new NullPointerException("right is mandatory");
		this.right = right;
		return this;
	}
	
	public BinaryExpression setRawRight(lombok.ast.Node right) {
		this.right = right;
		return this;
	}
	
	public BinaryExpression setOperator(BinaryOperator operator) {
		if (operator == null) throw new NullPointerException("operator is mandatory");
		this.errorReasonForOperator = null;
		this.operator = operator;
		this.operatorRaw = operator.getSymbol();
		
		return this;
	}
	
	public BinaryOperator getOperator() {
		if (operator == null) throw new AstException(this, errorReasonForOperator);
		return operator;
	}
	
	public BinaryExpression setRawOperator(String operator) {
		this.errorReasonForOperator = null;
		this.operator = null;
		this.operatorRaw = operator;
		
		if (operator == null) {
			this.errorReasonForOperator = "missing operator";
			return this;
		}
		
		this.operator = BinaryOperator.fromSymbol(operator.trim());
		if (this.operator != null) return this;
		
		this.errorReasonForOperator = "unknown binary operator: " + operator.trim();
		return this;
	}
	
	public String getRawOperator() {
		return operatorRaw;
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (errorReasonForOperator != null) problems.add(new SyntaxProblem(this, errorReasonForOperator));
		checkChildValidity(problems, this.left, "left", true, Expression.class);
		checkChildValidity(problems, this.right, "right", true, Expression.class);
	}
	
	@Override public void accept(ASTVisitor visitor) {
		if (visitor.visitBinaryExpression(this)) return;
		if (this.left != null) this.left.accept(visitor);
		if (this.right != null) this.right.accept(visitor);
	}
}
