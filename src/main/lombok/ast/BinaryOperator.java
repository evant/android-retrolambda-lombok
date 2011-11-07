/*
 * Copyright (C) 2010 The Project Lombok Authors.
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
	PLUS_ASSIGN("+=", 14), MINUS_ASSIGN("-=", 14), MULTIPLY_ASSIGN("*=", 14), DIVIDE_ASSIGN("/=", 14), REMAINDER_ASSIGN("%=", 14), AND_ASSIGN("&=", 14), XOR_ASSIGN("^=", 14), OR_ASSIGN("|=", 14), SHIFT_LEFT_ASSIGN("<<=", 14), SHIFT_RIGHT_ASSIGN(">>=", 14), BITWISE_SHIFT_RIGHT_ASSIGN(">>>=", 14), ASSIGN("=", 14),
	LOGICAL_OR("||", 12),
	LOGICAL_AND("&&", 11),
	BITWISE_OR("|", 10),
	BITWISE_XOR("^", 9),
	BITWISE_AND("&", 8),
	EQUALS("==", 7), NOT_EQUALS("!=", 7),
	GREATER(">", 6), GREATER_OR_EQUAL(">=", 6), LESS("<", 6), LESS_OR_EQUAL("<=", 6),
	SHIFT_LEFT("<<", 5), SHIFT_RIGHT(">>", 5), BITWISE_SHIFT_RIGHT(">>>", 5),
	PLUS("+", 4), MINUS("-", 4),
	MULTIPLY("*", 3), DIVIDE("/", 3), REMAINDER("%", 3);
	
	private final String symbol;
	private final int pLevel;
	
	BinaryOperator(String symbol, int pLevel) {
		this.symbol = symbol;
		this.pLevel = pLevel;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public boolean isAssignment() {
		return pLevel == ASSIGN.pLevel;
	}
	
	public static BinaryOperator fromSymbol(String symbol) {
		for (BinaryOperator op : values()) {
			if (op.symbol.equals(symbol)) return op;
		}
		
		return null;
	}
	
	int pLevel() {
		return pLevel;
	}
}
