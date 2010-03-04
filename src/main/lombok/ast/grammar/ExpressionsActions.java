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
import lombok.ast.ArrayCreation;
import lombok.ast.ArrayDimension;
import lombok.ast.ArrayInitializer;
import lombok.ast.BinaryExpression;
import lombok.ast.Cast;
import lombok.ast.ClassLiteral;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.Identifier;
import lombok.ast.InlineIfExpression;
import lombok.ast.InstanceOf;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.Select;
import lombok.ast.Super;
import lombok.ast.This;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;

public class ExpressionsActions extends SourceActions {
	public ExpressionsActions(Source source) {
		super(source);
	}
	
	public Node createLeftAssociativeBinaryExpression(
			org.parboiled.Node<Node> head,
			List<String> operators,
			List<org.parboiled.Node<Node>> tail) {
		
		Node currentLeft = head.getValue();
		
		for (int i = 0; i < operators.size(); i++) {
			currentLeft = new BinaryExpression()
					.setRawLeft(currentLeft)
					.setRawRight(tail.get(i).getValue()).setRawOperator(operators.get(i));
			positionSpan(currentLeft, head, tail.get(i));
		}
		
		return currentLeft;
	}
	
	public Node createAssignmentExpression(Node lhs, String operator, Node rhs) {
		return posify(new BinaryExpression().setRawLeft(lhs).setRawRight(rhs).setRawOperator(operator));
	}
	
	public Node createInlineIfExpression(
			org.parboiled.Node<Node> head,
			List<String> operators1, List<String> operators2,
			List<org.parboiled.Node<Node>> tail1, List<org.parboiled.Node<Node>> tail2) {
		
		if (tail1.size() == 0 || tail2.size() == 0) return head.getValue();
		
		org.parboiled.Node<Node> end = tail2.remove(tail2.size() -1);
		Node currentNode = end.getValue();
		
		Collections.reverse(tail1);
		Collections.reverse(tail2);
		tail2.add(head);
		
		for (int i = 0; i < tail1.size(); i++) {
			currentNode = new InlineIfExpression()
					.setRawCondition(tail2.get(i).getValue())
					.setRawIfTrue(tail1.get(i).getValue())
					.setRawIfFalse(currentNode);
			positionSpan(currentNode, tail2.get(i), end);
		}
		
		return currentNode;
	}
	
	public Node createUnaryPrefixExpression(
			org.parboiled.Node<Node> operand,
			List<org.parboiled.Node<Node>> operators,
			List<String> operatorTexts) {
		
		if (operators == null || operators.isEmpty()) return operand.getValue();
		
		Node current = operand.getValue();
		
		for (int i = operators.size()-1; i >= 0; i--) {
			org.parboiled.Node<Node> operator = operators.get(i);
			Node prev = current;
			if (operator == null) continue;
			if (!operator.getChildren().isEmpty() && "cast".equals(operator.getChildren().get(0).getLabel())) {
				current = new Cast().setRawOperand(current).setRawTypeReference(operator.getValue());
			} else {
				String symbol = operatorTexts.get(i);
				if (symbol == null) continue;
				symbol = symbol.trim();
				if (symbol.isEmpty()) continue;
				
				UnaryOperator op = UnaryOperator.fromSymbol(symbol, false);
				UnaryExpression expr = new UnaryExpression().setRawOperand(current);
				if (op != null) expr.setOperator(op);
				current = expr;
			}
			if (prev != null && !prev.getPosition().isUnplaced() && prev != current && current != null) {
				positionSpan(current, operator, operand);
			}
		}
		
		return current;
	}
	
	public Node createUnaryPostfixExpression(Node operand, List<org.parboiled.Node<Node>> nodes, List<String> operators) {
		if (operators == null) return operand;
		
		Node current = operand;
		for (int i = 0; i < operators.size(); i++) {
			String op = operators.get(i);
			if (op == null) continue;
			op = op.trim();
			Node prev = current;
			if (op.equals("++")) current = new UnaryExpression().setRawOperand(current).setOperator(UnaryOperator.POSTFIX_INCREMENT);
			else if (op.equals("--")) current = new UnaryExpression().setRawOperand(current).setOperator(UnaryOperator.POSTFIX_DECREMENT);
			org.parboiled.Node<Node> p = nodes.get(i);
			if (prev != null && !prev.getPosition().isUnplaced() && p != null) {
				current.setPosition(new Position(prev.getPosition().getStart(), source.mapPositionRtrim(p.getEndLocation().index)));
			}
		}
		return current;
	}
	
	public Node createInstanceOfExpression(Node operand, Node type) {
		if (type == null) return operand;
		return posify(new InstanceOf().setRawObjectReference(operand).setRawTypeReference(type));
	}
	
	public Node createQualifiedConstructorInvocation(
			Node constructorTypeArgs,
			org.parboiled.Node<Node> identifier, org.parboiled.Node<Node> classTypeArgs,
			Node methodArguments, Node classBody) {
		
		TypeReferencePart classTypeArgs0;
		boolean classTypeArgsCorrect = false;
		Node identifierNode = identifier == null ? null : identifier.getValue();
		if (classTypeArgs != null && classTypeArgs.getValue() instanceof TypeReferencePart) {
			classTypeArgs0 = (TypeReferencePart)classTypeArgs.getValue();
			classTypeArgsCorrect = true;
		} else {
			classTypeArgs0 = new TypeReferencePart();
			if (identifierNode != null) {
				int pos = identifierNode.getPosition().getEnd();
				classTypeArgs0.setPosition(new Position(pos, pos));
			}
		}
		MethodInvocation methodArguments0 = (methodArguments instanceof MethodInvocation) ? (MethodInvocation)methodArguments : new MethodInvocation();
		
		TypeReference typeReference = new TypeReference().parts().addToEnd(
				classTypeArgs0.setRawIdentifier(identifierNode));
		if (!classTypeArgsCorrect) {
			if (identifier != null && identifier.getValue() != null) {
				typeReference.setPosition(identifier.getValue().getPosition());
			}
		} else {
			positionSpan(typeReference, identifier, classTypeArgs);
		}
		return posify(new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(typeReference)
				.rawArguments().migrateAllFrom(methodArguments0.rawArguments())
				.setRawAnonymousClassBody(classBody));
	}
	
	public Node createChainOfQualifiedConstructorInvocations(
			org.parboiled.Node<Node> qualifier,
			List<org.parboiled.Node<Node>> constructorInvocations) {
		Node current = qualifier.getValue();
		
		if (constructorInvocations == null) return current;
		
		for (org.parboiled.Node<Node> pNode : constructorInvocations) {
			Node n = pNode.getValue();
			if (n instanceof ConstructorInvocation) {
				current = ((ConstructorInvocation)n).setRawQualifier(current);
				positionSpan(current, qualifier, pNode);
			}
			//TODO else hang dangling node.
		}
		
		return current;
	}
	
	public Node createMethodInvocationOperation(Node typeArguments, Node name, Node arguments) {
		MethodInvocation mi = (arguments instanceof MethodInvocation) ? (MethodInvocation)arguments : new MethodInvocation();
		//TODO hang dangling node on mi if arguments is non null but also not an MI.
		return posify(mi.setRawName(name).setRawMethodTypeArguments(typeArguments));
	}
	
	public Node createSelectOperation(Node identifier) {
		return posify(new Select().setRawIdentifier(identifier));
	}
	
	public Node createArrayAccessOperation(Node indexExpression) {
		return posify(new ArrayAccess().setRawIndexExpression(indexExpression));
	}
	
	public Node createLevel1Expression(org.parboiled.Node<Node> operand, List<org.parboiled.Node<Node>> operations) {
		Node current = operand.getValue();
		if (operations == null) return current;
		
		for (org.parboiled.Node<Node> pNode : operations) {
			Node o = pNode.getValue();
			
			if (o instanceof ArrayAccess) {
				current = ((ArrayAccess)o).setRawOperand(current);
			} else if (o instanceof MethodInvocation) {
				current = ((MethodInvocation)o).setRawOperand(current);
			} else if (o instanceof Select) {
				current = ((Select)o).setRawOperand(current);
			}
			//TODO else hang dangling node.
			
			positionSpan(o, operand, pNode);
		}
		return current;
	}
	
	public Node createPrimary(Node identifier, Node methodArguments) {
		if (methodArguments instanceof MethodInvocation) return posify(((MethodInvocation)methodArguments).setRawName(identifier));
		//TODO if (methodArguments != null) add dangling node.
		
		return identifier;
	}
	
	public Node createUnqualifiedConstructorInvocation(Node constructorTypeArgs, Node type, Node args, Node anonymousClassBody) {
		MethodInvocation args0 = (args instanceof MethodInvocation) ? (MethodInvocation)args : new MethodInvocation();
		//TODO if (args != null) add dangling.
		
		return posify(new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(type)
				.rawArguments().migrateAllFrom(args0.rawArguments())
				.setRawAnonymousClassBody(anonymousClassBody));
	}
	
	public Node createArrayInitializerExpression(Node head, List<Node> tail) {
		ArrayInitializer ai = new ArrayInitializer();
		if (head != null) ai.rawExpressions().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) ai.rawExpressions().addToEnd(n);
		return posify(ai);
	}
	
	public Node createDimension(Node dimExpr, org.parboiled.Node<Node> arrayOpen) {
		ArrayDimension d = new ArrayDimension().setRawDimension(dimExpr);
		if (arrayOpen != null) d.setPosition(new Position(source.mapPosition(arrayOpen.getStartLocation().index), getCurrentLocationRtrim()));
		return d;
	}
	
	public Node createArrayCreationExpression(Node type, List<Node> dimensions, Node initializer) {
		ArrayCreation ac = new ArrayCreation().setRawComponentTypeReference(type).setRawInitializer(initializer);
		if (dimensions != null) for (Node d : dimensions) {
			if (d != null) ac.rawDimensions().addToEnd(d);
		}
		
		return posify(ac);
	}
	
	public Node addParens(Node v) {
		if (v instanceof Expression) {
			((Expression)v).setParens(Math.max(0, ((Expression)v).getParens()) + 1);
		}
		return v;
	}
	
	public Node createThisOrSuperOrClass(String text, Node qualifier) {
		if ("super".equals(text)) return posify(new Super().setRawQualifier(qualifier));
		if ("class".equals(text)) return posify(new ClassLiteral().setRawTypeReference(qualifier));
		return posify(new This().setRawQualifier(qualifier));
	}
	
	public boolean checkIfLevel1ExprIsValidForAssignment(Node node) {
		return node instanceof Identifier || node instanceof Select || node instanceof ArrayAccess;
	}
	
	public boolean checkIfMethodOrConstructorInvocation(Node node) {
		return node instanceof MethodInvocation || node instanceof ConstructorInvocation;
	}
}
