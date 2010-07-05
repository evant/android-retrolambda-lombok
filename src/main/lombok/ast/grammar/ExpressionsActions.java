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
import lombok.ast.VariableReference;

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
					.rawLeft(currentLeft)
					.rawRight(tail.get(i).getValue()).rawOperator(operators.get(i));
			source.registerStructure(currentLeft, operatorsNodes.get(i));
			positionSpan(currentLeft, head, tail.get(i));
		}
		
		return currentLeft;
	}
	
	public Node createAssignmentExpression(Node lhs, String operator, Node rhs) {
		if (rhs == null && operator == null) return lhs;
		return posify(new BinaryExpression().rawLeft(lhs).rawRight(rhs).rawOperator(operator));
	}
	
	public Node createInlineIfExpression(
			Node head, org.parboiled.Node<Node> operator1Node,
			org.parboiled.Node<Node> operator2Node, Node tail1, Node tail2) {
		
		if (tail1 == null || tail2 == null) return head;
		
		InlineIfExpression result = new InlineIfExpression()
				.rawCondition(head).rawIfTrue(tail1).rawIfFalse(tail2);
		source.registerStructure(result, operator1Node);
		source.registerStructure(result, operator2Node);
		return posify(result);
	}
	
	public Node createUnaryPrefixExpression(Node operand, org.parboiled.Node<Node> opNode, String symbol) {
		if (opNode == null) return operand;
		
		if (!opNode.getChildren().isEmpty() && "cast".equals(opNode.getChildren().get(0).getLabel())) {
			return posify(new Cast().rawOperand(operand).rawTypeReference(opNode.getValue()));
		} else {
			if (symbol != null) symbol = symbol.trim();
			if (!symbol.isEmpty()) {
				UnaryOperator op = UnaryOperator.fromSymbol(symbol, false);
				UnaryExpression expr = new UnaryExpression().rawOperand(operand);
				if (op != null) expr.astOperator(op);
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
				current = new Cast().rawOperand(current).rawTypeReference(operator.getValue());
			} else {
				String symbol = operatorTexts.get(i);
				if (symbol == null) continue;
				symbol = symbol.trim();
				if (symbol.isEmpty()) continue;
				
				UnaryOperator op = UnaryOperator.fromSymbol(symbol, false);
				UnaryExpression expr = new UnaryExpression().rawOperand(current);
				if (op != null) expr.astOperator(op);
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
			if (op.equals("++")) current = new UnaryExpression().rawOperand(current).astOperator(UnaryOperator.POSTFIX_INCREMENT);
			else if (op.equals("--")) current = new UnaryExpression().rawOperand(current).astOperator(UnaryOperator.POSTFIX_DECREMENT);
			org.parboiled.Node<Node> p = nodes.get(i);
			if (prev != null && !prev.getPosition().isUnplaced() && p != null) {
				current.setPosition(new Position(prev.getPosition().getStart(), p.getEndIndex()));
			}
		}
		return current;
	}
	
	public Node createInstanceOfExpression(Node operand, Node type) {
		if (type == null) return operand;
		return posify(new InstanceOf().rawObjectReference(operand).rawTypeReference(type));
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
		
		TypeReference typeReference = new TypeReference().astParts().addToEnd(
				classTypeArgs0.astIdentifier(createIdentifierIfNeeded(identifierNode, currentPos())));
		if (!classTypeArgsCorrect) {
			if (identifier != null && identifier.getValue() != null) {
				typeReference.setPosition(identifier.getValue().getPosition());
			}
		} else {
			positionSpan(typeReference, identifier, classTypeArgs);
		}
		
		ConstructorInvocation constructorInvocation = new ConstructorInvocation()
				.rawTypeReference(typeReference)
				.rawAnonymousClassBody(classBody);
		
		if (constructorTypeArgs instanceof TemporaryNode.TypeArguments) {
			for (Node arg : ((TemporaryNode.TypeArguments)constructorTypeArgs).arguments) {
				constructorInvocation.rawConstructorTypeArguments().addToEnd(arg);
			}
		}
		
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
				current = ((ConstructorInvocation)n).rawQualifier(current);
				positionSpan(current, qualifier, pNode);
			} else current.addDanglingNode(n);
		}
		
		return current;
	}
	
	public Node createMethodInvocationOperation(org.parboiled.Node<Node> dot, Node typeArguments, Node name, Node arguments) {
		MethodInvocation mi = new MethodInvocation().astName(createIdentifierIfNeeded(name, currentPos()));
		
		if (typeArguments instanceof TemporaryNode.TypeArguments) {
			for (Node arg : ((TemporaryNode.TypeArguments)typeArguments).arguments) {
				mi.rawMethodTypeArguments().addToEnd(arg);
			}
		} else mi.addDanglingNode(typeArguments);
		
		if (arguments instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)arguments).arguments) {
				mi.rawArguments().addToEnd(arg);
			}
		} else mi.addDanglingNode(arguments);
		
		source.registerStructure(mi, dot);
		
		return posify(mi);
	}
	
	public Node createSelectOperation(Node identifier) {
		return posify(new Select().astIdentifier(createIdentifierIfNeeded(identifier, currentPos())));
	}
	
	public Node createArrayAccessOperation(Node indexExpression) {
		return posify(new ArrayAccess().rawIndexExpression(indexExpression));
	}
	
	public Node createLevel1Expression(org.parboiled.Node<Node> operand, List<org.parboiled.Node<Node>> operations) {
		Node current = operand.getValue();
		if (operations == null) return current;
		
		for (org.parboiled.Node<Node> pNode : operations) {
			Node o = pNode.getValue();
			
			if (o instanceof ArrayAccess) {
				current = ((ArrayAccess)o).rawOperand(current);
			} else if (o instanceof MethodInvocation) {
				current = ((MethodInvocation)o).rawOperand(current);
			} else if (o instanceof Select) {
				current = ((Select)o).rawOperand(current);
			} else {
				current.addDanglingNode(o);
			}
			
			positionSpan(o, operand, pNode);
		}
		return current;
	}
	
	public Node createPrimary(Node identifier, Node methodArguments) {
		Identifier id = createIdentifierIfNeeded(identifier, currentPos());
		
		if (methodArguments instanceof TemporaryNode.MethodArguments) {
			MethodInvocation invoke = new MethodInvocation().astName(id);
			for (Node arg : ((TemporaryNode.MethodArguments)methodArguments).arguments) {
				invoke.rawArguments().addToEnd(arg);
			}
			return posify(invoke);
		} else {
			VariableReference ref = new VariableReference().astIdentifier(id);
			ref.addDanglingNode(methodArguments);
			return posify(ref);
		}
	}
	
	public Node createUnqualifiedConstructorInvocation(Node constructorTypeArgs, Node type, Node args, Node anonymousClassBody) {
		ConstructorInvocation result = new ConstructorInvocation()
				.rawTypeReference(type)
				.rawAnonymousClassBody(anonymousClassBody);
		
		if (constructorTypeArgs instanceof TemporaryNode.TypeArguments) {
			for (Node arg : ((TemporaryNode.TypeArguments)constructorTypeArgs).arguments) {
				result.rawConstructorTypeArguments().addToEnd(arg);
			}
		}
		
		if (args instanceof TemporaryNode.MethodArguments) {
			for (Node arg : ((TemporaryNode.MethodArguments)args).arguments) {
				result.rawArguments().addToEnd(arg);
			}
		} else result.addDanglingNode(args);
		
		return posify(result);
	}
	
	public Node createArrayInitializerExpression(Node head, List<Node> tail) {
		ArrayInitializer ai = new ArrayInitializer();
		if (head != null) ai.rawExpressions().addToEnd(head);
		if (tail != null) for (Node n : tail) if (n != null) ai.rawExpressions().addToEnd(n);
		return posify(ai);
	}
	
	public Node createDimension(Node dimExpr, org.parboiled.Node<Node> arrayOpen) {
		ArrayDimension d = new ArrayDimension().rawDimension(dimExpr);
		if (arrayOpen != null) d.setPosition(new Position(arrayOpen.getStartIndex(), currentPos()));
		return d;
	}
	
	public Node createArrayCreationExpression(Node type, List<Node> dimensions, Node initializer) {
		ArrayCreation ac = new ArrayCreation().rawComponentTypeReference(type).rawInitializer(initializer);
		if (dimensions != null) for (Node d : dimensions) {
			if (d != null) ac.rawDimensions().addToEnd(d);
		}
		
		return posify(ac);
	}
	
	public Node addParens(Node v) {
		if (v instanceof Expression) {
			((Expression)v).astParensPositions().add(new Position(startPos(), currentPos()));
		}
		return v;
	}
	
	public Node createThisOrSuperOrClass(org.parboiled.Node<Node> dot, String text, Node qualifier) {
		Node result;
		if ("super".equals(text)) result = new Super().rawQualifier(qualifier);
		else if ("class".equals(text)) result = new ClassLiteral().rawTypeReference(qualifier);
		else result = new This().rawQualifier(qualifier);
		if (dot != null) source.registerStructure(result, dot);
		return posify(result);
	}
	
	public boolean checkIfLevel1ExprIsValidForAssignment(Node node) {
		return node instanceof VariableReference || node instanceof Select || node instanceof ArrayAccess;
	}
	
	public boolean checkIfMethodOrConstructorInvocation(Node node) {
		return node instanceof MethodInvocation || node instanceof ConstructorInvocation;
	}
	
	public boolean typeIsAlsoLegalAsExpression(Node type) {
		if (!(type instanceof TypeReference)) return true;
		TypeReference tr = (TypeReference)type;
		if (tr.astArrayDimensions() > 0) return false;
		if (tr.isPrimitive() || tr.isVoid()) return false;
		for (Node part : tr.rawParts()) {
			if (part instanceof TypeReferencePart) {
				if (!((TypeReferencePart)part).rawTypeArguments().isEmpty()) return false;
			}
		}
		
		return true;
	}
}
