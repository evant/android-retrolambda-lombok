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

import lombok.ast.ArrayAccess;
import lombok.ast.BinaryExpression;
import lombok.ast.Cast;
import lombok.ast.ConstructorInvocation;
import lombok.ast.IdentifierExpression;
import lombok.ast.IncrementExpression;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceOf;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.UnaryExpression;

import org.parboiled.BaseActions;

public class ExpressionsActions extends BaseActions<Node> {
	public Node createLeftAssociativeBinaryExpression(Node head, List<String> operators, List<Node> tail) {
		Node currentLeft = head;
		
		for (int i = 0; i < operators.size(); i++) {
			currentLeft = new BinaryExpression().setRawLeft(currentLeft).setRawRight(tail.get(i)).setRawOperator(operators.get(i));
		}
		
		return currentLeft;
	}
	
	public Node createRightAssociativeBinaryExpression(Node head, List<String> operators, List<Node> tail) {
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
	
	public Node createInlineIfExpression(Node head, List<String> operators1, List<String> operators2, List<Node> tail1, List<Node> tail2) {
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
	
	public Node createUnaryPrefixExpression(Node operand, List<org.parboiled.Node<Node>> operators, List<String> operatorTexts) {
		if (operators == null || operators.isEmpty()) return operand;
		
		Node current = operand;
		
		for (int i = operators.size()-1; i >= 0; i--) {
			org.parboiled.Node<Node> operator = operators.get(i);
			if (operator == null) continue;
			if ("cast".equals(operator.getLabel())) {
				current = new Cast().setRawOperand(current).setRawType(operator.getValue());
			} else {
				String symbol = operatorTexts.get(i);
				if (symbol == null) continue;
				symbol = symbol.trim();
				if (symbol.isEmpty()) continue;
				
				if (symbol.equals("--")) {
					current = new IncrementExpression().setRawOperand(current).setPrefix(true);
					continue;
				}
				if (symbol.equals("++")) {
					current = new IncrementExpression().setRawOperand(current).setPrefix(true).setDecrement(true);
					continue;
				}
				
				current = new UnaryExpression().setRawOperand(current).setRawOperator(symbol);
			}
		}
		
		return current;
	}
	
	public Node createUnaryPostfixExpression(Node operand, List<String> operators) {
		if (operators == null) return operand;
		
		Node current = operand;
		for (String op : operators) {
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
	
	public Node createInstanceOfExpression(Node operand, Node type) {
		return new InstanceOf().setRawObjectReference(operand).setRawType(type);
	}
	
	public Node createQualifiedConstructorInvocation(Node constructorTypeArgs, Node identifier, Node classTypeArgs, Node methodArguments, Node classBody) {
		TypeReferencePart classTypeArgs0 = (classTypeArgs instanceof TypeReferencePart) ? (TypeReferencePart)classTypeArgs : new TypeReferencePart();
		MethodInvocation methodArguments0 = (methodArguments instanceof MethodInvocation) ? (MethodInvocation)methodArguments : new MethodInvocation();
		
		return new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(new TypeReference().parts().addToEnd(classTypeArgs0.setRawIdentifier(identifier)))
				.arguments().migrateAllFromRaw(methodArguments0.arguments())
				.setRawAnonymousClassBody(classBody);
	}
	
	public Node createChainOfQualifiedConstructorInvocations(Node qualifier, List<Node> constructorInvocations) {
		Node current = qualifier;
		
		if (constructorInvocations == null) return current;
		
		for (Node n : constructorInvocations) {
			if (n instanceof ConstructorInvocation)
				current = ((ConstructorInvocation)n).setRawQualifier(current);
		}
		
		return current;
	}
	
	public Node createMethodInvocationOperation(Node typeArguments, Node name, Node arguments) {
		MethodInvocation mi = (arguments instanceof MethodInvocation) ? (MethodInvocation)arguments : new MethodInvocation();
		//TODO hang dangling node on mi if arguments is non null but also not an MI.
		return mi.setRawName(name).setRawMethodTypeArguments(typeArguments);
	}
	
	public Node createSelectOperation(Node identifier) {
		return new Select().setRawIdentifier(identifier);
	}
	
	public Node createArrayAccessOperation(Node indexExpression) {
		return new ArrayAccess().setRawIndexExpression(indexExpression);
	}
	
	public Node createLevel1Expression(Node operand, List<Node> operations) {
		Node current = operand;
		if (operations == null) return current;
		
		for (Node o : operations) {
			if (o instanceof ArrayAccess) {
				current = ((ArrayAccess)o).setRawOperand(current);
			} else if (o instanceof MethodInvocation) {
				current = ((MethodInvocation)o).setRawOperand(current);
			} else if (o instanceof Select) {
				current = ((Select)o).setRawOperand(operand);
			}
		}
		return current;
	}
	
	public Node createPrimary(Node identifier, Node methodArguments) {
		if (methodArguments instanceof MethodInvocation) return ((MethodInvocation)methodArguments).setRawName(identifier);
		//TODO if (methodArguments != null) add dangling node.
		
		if (methodArguments != null) System.err.println("WAHUH: " + methodArguments);
		if (methodArguments == null ) System.err.println("WAHUH2");
		
		return new IdentifierExpression().setRawIdentifier(identifier);
	}
	
	public Node createUnqualifiedConstructorInvocation(Node constructorTypeArgs, Node type, Node args, Node anonymousClassBody) {
		MethodInvocation args0 = (args instanceof MethodInvocation) ? (MethodInvocation)args : new MethodInvocation();
		//TODO if (args != null) add dangling.
		
		return new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(type)
				.arguments().migrateAllFromRaw(args0.arguments())
				.setRawAnonymousClassBody(anonymousClassBody);
	}
}
