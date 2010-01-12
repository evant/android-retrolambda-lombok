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
package lombok.ast.grammar;

import java.util.Collections;
import java.util.List;

import lombok.ast.BinaryExpression;
import lombok.ast.Cast;
import lombok.ast.IdentifierExpression;
import lombok.ast.IncrementExpression;
import lombok.ast.InlineIfExpression;
import lombok.ast.Node;
import lombok.ast.UnaryExpression;

import org.parboiled.BaseActions;

public class OperatorsActions extends BaseActions<Node> {
	public Node createLeftAssociativeBinaryOperation(Node head, List<String> operators, List<Node> tail) {
		Node currentLeft = head;
		
		for (int i = 0; i < operators.size(); i++) {
			currentLeft = new BinaryExpression().setRawLeft(currentLeft).setRawRight(tail.get(i)).setRawOperator(operators.get(i));
		}
		
		return currentLeft;
	}
	
	public Node createRightAssociativeBinaryOperation(Node head, List<String> operators, List<Node> tail) {
		if (tail.size() == 0) return head;
		
		Node currentRight = tail.remove(tail.size() -1);
		Collections.reverse(tail);
		Collections.reverse(operators);
		tail.add(head);
		
		for (int i = 0; i < operators.size(); i++) {
			currentRight = new BinaryExpression().setRawLeft(tail.get(i)).setRawRight(currentRight).setRawOperator(operators.get(i));
		}
		
		return currentRight;
	}
	
	public Node createInlineIfOperation(Node head, List<String> operators1, List<String> operators2, List<Node> tail1, List<Node> tail2) {
		if (tail1.size() == 0 || tail2.size() == 0) return head;
		
		Node currentNode = tail2.remove(tail2.size() -1);
		
		Collections.reverse(tail1);
		Collections.reverse(tail2);
		tail2.add(head);
		
		for (int i = 0; i < tail1.size(); i++) {
			currentNode = new InlineIfExpression().setRawCondition(tail2.get(i)).setRawIfTrue(tail1.get(i)).setRawIfFalse(currentNode);
		}
		
		return currentNode;
	}
	
	public Node createUnaryOperation(String operator, Node operand) {
		Node current = operand;
		
		if (operator != null && operator.trim().equals("++")) return new IncrementExpression().setRawOperand(current).setPrefix(true);
		if (operator != null && operator.trim().equals("--")) return new IncrementExpression().setRawOperand(current).setPrefix(true).setDecrement(true);
		
		return new UnaryExpression().setRawOperand(current).setRawOperator(operator);
	}
	
	public Node createPostfixOperation(Node value, List<String> texts) {
		if (texts == null) return value;
		
		Node current = value;
		for (String op : texts) {
			if (op == null) continue;
			op = op.trim();
			if (op.equals("++")) current = new IncrementExpression().setRawOperand(current);
			else if (op.equals("--")) current = new IncrementExpression().setRawOperand(current).setDecrement(true);
		}
		return current;
	}
	
	public Node createTypeCastExpression(Node type, Node operand) {
		return new Cast().setRawOperand(operand).setRawType(type);
	}
	
	public Node createIdentifierExpression(Node identifier) {
		return new IdentifierExpression().setRawIdentifier(identifier);
	}
}
