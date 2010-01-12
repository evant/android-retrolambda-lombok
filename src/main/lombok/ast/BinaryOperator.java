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