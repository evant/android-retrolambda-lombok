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
import lombok.ast.grammar.TemporaryNode.ExprChain;

public class ExpressionsActions extends SourceActions {
	public ExpressionsActions(Source source) {
		super(source);
	}
	
	public Node createLeftAssociativeBinaryExpression(
			org.parboiled.Node<Node> head,
			List<org.parboiled.Node<Node>> operatorsNodes, 
			List<String> operators,
			List<org.parboiled.Node<Node>> tail) {
		
		Node currentLeft = head.getValue();
		
		for (int i = 0; i < operators.size(); i++) {
			currentLeft = new BinaryExpression()
					.setRawLeft(currentLeft)
					.setRawRight(tail.get(i).getValue()).setRawOperator(operators.get(i));
			source.registerStructure(currentLeft, operatorsNodes.get(i));
			positionSpan(currentLeft, head, tail.get(i));
		}
		
		return currentLeft;
	}
	
	public Node createAssignmentExpression(Node lhs, String operator, Node rhs) {
		return posify(new BinaryExpression().setRawLeft(lhs).setRawRight(rhs).setRawOperator(operator));
	}
	
	public Node createInlineIfExpression(
			Node head, org.parboiled.Node<Node> operator1Node,
			org.parboiled.Node<Node> operator2Node, Node tail1, Node tail2) {
		
		if (tail1 == null || tail2 == null) return head;
		
		InlineIfExpression result = new InlineIfExpression()
				.setRawCondition(head).setRawIfTrue(tail1).setRawIfFalse(tail2);
		source.registerStructure(result, operator1Node);
		source.registerStructure(result, operator2Node);
		return posify(result);
	}
	
	public Node createUnaryPrefixExpression(Node operand, org.parboiled.Node<Node> opNode, String symbol) {
		if (opNode == null) return operand;
		
		if (!opNode.getChildren().isEmpty() && "cast".equals(opNode.getChildren().get(0).getLabel())) {
			return posify(new Cast().setRawOperand(operand).setRawTypeReference(opNode.getValue()));
		} else {
			if (symbol != null) symbol = symbol.trim();
			if (!symbol.isEmpty()) {
				UnaryOperator op = UnaryOperator.fromSymbol(symbol, false);
				UnaryExpression expr = new UnaryExpression().setRawOperand(operand);
				if (op != null) expr.setOperator(op);
				return posify(expr);
			}
		}
		
		return operand;
	}
	
	public Node createUnaryPrefixExpressions(
			org.parboiled.Node<Node> operand,
			List<org.parboiled.Node<Node>> operators,
			List<String> operatorTexts) {
		
		if (operators == null || operators.isEmpty()) return operand.getValue();
		
		Node current = operand.getValue();
		for (int i = operators.size() - 1; i >= 0; i--) {
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
				current.setPosition(new Position(prev.getPosition().getStart(), p.getEndLocation().index));
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
		
		TypeReference typeReference = new TypeReference().parts().addToEnd(
				classTypeArgs0.setRawIdentifier(identifierNode));
		if (!classTypeArgsCorrect) {
			if (identifier != null && identifier.getValue() != null) {
				typeReference.setPosition(identifier.getValue().getPosition());
			}
		} else {
			positionSpan(typeReference, identifier, classTypeArgs);
		}
		
		ConstructorInvocation constructorInvocation = new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(typeReference)
				.setRawAnonymousClassBody(classBody);
		
		if (methodArguments instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)methodArguments).arguments) {
				constructorInvocation.rawArguments().addToEnd(arg);
			}
		}
		
		return posify(constructorInvocation);
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
	
	public Node createMethodInvocationOperation(org.parboiled.Node<Node> dot, Node typeArguments, Node name, Node arguments) {
		MethodInvocation mi = new MethodInvocation().setRawName(name).setRawMethodTypeArguments(typeArguments);
		if (arguments instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)arguments).arguments) {
				mi.rawArguments().addToEnd(arg);
			}
		}
		//TODO hang dangling node on mi if arguments is non null but also not an MI.
		
		source.registerStructure(mi, dot);
		
		return posify(mi);
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
		if (methodArguments instanceof TemporaryNode.MethodArguments) {
			MethodInvocation invoke = new MethodInvocation().setRawName(identifier);
			for (Node arg : ((TemporaryNode.MethodArguments)methodArguments).arguments) {
				invoke.rawArguments().addToEnd(arg);
			}
			return posify(invoke);
		}
		//TODO if (methodArguments != null) add dangling node.
		
		return identifier;
	}
	
	public Node createUnqualifiedConstructorInvocation(Node constructorTypeArgs, Node type, Node args, Node anonymousClassBody) {
		ConstructorInvocation result = new ConstructorInvocation()
				.setRawConstructorTypeArguments(constructorTypeArgs)
				.setRawTypeReference(type)
				.setRawAnonymousClassBody(anonymousClassBody);
		if (args instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)args).arguments) {
				result.rawArguments().addToEnd(arg);
			}
		} else {
			//TODO if (args != null) add dangling.
		}
		return posify(result);
	}
	
	public Node createArrayInitializerExpression(Node head, List<Node> tail) {
		ArrayInitializer ai = new ArrayInitializer();
		if (head != null) ai.rawExpressions().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) ai.rawExpressions().addToEnd(n);
		return posify(ai);
	}
	
	public Node createDimension(Node dimExpr, org.parboiled.Node<Node> arrayOpen) {
		ArrayDimension d = new ArrayDimension().setRawDimension(dimExpr);
		if (arrayOpen != null) d.setPosition(new Position(arrayOpen.getStartLocation().index, currentPos()));
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
			((Expression)v).getParensPositions().add(new Position(startPos(), currentPos()));
		}
		return v;
	}
	
	public Node createThisOrSuperOrClass(org.parboiled.Node<Node> dot, String text, Node qualifier) {
		Node result;
		if ("super".equals(text)) result = new Super().setRawQualifier(qualifier);
		else if ("class".equals(text)) result = new ClassLiteral().setRawTypeReference(qualifier);
		else result = new This().setRawQualifier(qualifier);
		if (dot != null) source.registerStructure(result, dot);
		return posify(result);
	}
	
	public boolean checkIfLevel1ExprIsValidForAssignment(Node node) {
		return node instanceof Identifier || node instanceof Select || node instanceof ArrayAccess;
	}
	
	public boolean checkIfMethodOrConstructorInvocation(Node node) {
		return node instanceof MethodInvocation || node instanceof ConstructorInvocation;
	}
	
	public Node padBinaryExpressionChain(org.parboiled.Node<Node> tail, String opText, org.parboiled.Node<Node> opNode, org.parboiled.Node<Node> head) {
		if (tail == null) return head == null ? null : head.getValue();
		
		ExprChain chain = null;
		if (tail.getValue() instanceof ExprChain) {
			chain = (ExprChain)tail.getValue();
		} else {
			chain = new ExprChain();
			chain.tail = tail;
		}
		
		if (opNode != null || opText != null || head != null) {
			chain.opNodes.add(opNode);
			chain.opTexts.add(opText);
			chain.heads.add(head);
		}
		return chain;
	}
	
	public Node convertBinaryExpressionChain(Node node) {
		if (!(node instanceof ExprChain)) return node;
		
		ExprChain chain = (ExprChain) node;
		org.parboiled.Node<Node> head = null;
		
		if (chain.heads.isEmpty()) {
			head = chain.tail;
		} else {
			head = chain.heads.remove(chain.heads.size()-1);
			Collections.reverse(chain.heads);
			Collections.reverse(chain.opNodes);
			Collections.reverse(chain.opTexts);
			chain.heads.add(chain.tail);
		}
		
		Node currentLeft = head.getValue();
		
		for (int i = 0; i < chain.heads.size(); i++) {
			currentLeft = new BinaryExpression().setRawLeft(currentLeft).setRawRight(chain.heads.get(i).getValue()).setRawOperator(chain.opTexts.get(i));
			source.registerStructure(currentLeft, chain.opNodes.get(i));
			positionSpan(currentLeft, head, chain.heads.get(i));
		}
		return currentLeft;
	}
}
