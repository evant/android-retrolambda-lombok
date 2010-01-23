package lombok.ast.grammar;

import java.util.Collections;
import java.util.List;

import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Assert;
import lombok.ast.Block;
import lombok.ast.Break;
import lombok.ast.Case;
import lombok.ast.Catch;
import lombok.ast.Continue;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyStatement;
import lombok.ast.ExpressionStatement;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.LabelledStatement;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Switch;
import lombok.ast.Synchronized;
import lombok.ast.Throw;
import lombok.ast.Try;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.While;

import org.parboiled.BaseActions;

public class StatementsActions extends BaseActions<Node> {
	public Node createBlock(List<Node> statements) {
		Block b = new Block();
		if (statements != null) for (Node s : statements) {
			if (s != null) b.contents().addToEndRaw(s);
		}
		
		return b;
	}
	
	public Node createEmptyStatement() {
		return new EmptyStatement();
	}
	
	public Node createLabelledStatement(List<Node> labelNames, Node statement) {
		Node current = statement;
		if (labelNames != null) for (Node n : labelNames) {
			if (n != null) current = new LabelledStatement().setRawLabel(n).setRawStatement(current);
		}
		return current;
	}
	
	public Node createIfStatement(Node condition, Node statement, Node elseStatement) {
		return new If().setRawCondition(condition).setRawStatement(statement).setRawElseStatement(elseStatement);
	}
	
	public Node createAssertStatement(Node assertion, Node message) {
		return new Assert().setRawAssertion(assertion).setRawMessage(message);
	}
	
	public Node createSwitchStatement(Node condition, Node body) {
		return new Switch().setRawCondition(condition).setRawBody(body);
	}
	
	public Node createCaseStatement(Node condition) {
		return new Case().setRawCondition(condition);
	}
	
	public Node createDefaultStatement() {
		return new Default();
	}
	
	public Node createWhileStatement(Node condition, Node statement) {
		return new While().setRawCondition(condition).setRawStatement(statement);
	}
	
	public Node createDoStatement(Node condition, Node statement) {
		return new DoWhile().setRawCondition(condition).setRawStatement(statement);
	}
	
	public Node createStatementExpressionList(Node head, List<Node> tail) {
		TemporaryNode.StatementExpressionList result = new TemporaryNode.StatementExpressionList();
		if (head != null) result.expressions.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.expressions.add(n);
		return result;
	}
	
	public Node createBasicFor(Node init, Node condition, Node update, Node statement) {
		For result = new For().setRawCondition(condition).setRawStatement(statement);
		List<Node> inits, updates;
		
		if (init instanceof TemporaryNode.StatementExpressionList) {
			inits = ((TemporaryNode.StatementExpressionList)init).expressions;
		} else {
			inits = Collections.singletonList(init);
		}
		
		if (update instanceof TemporaryNode.StatementExpressionList) {
			updates = ((TemporaryNode.StatementExpressionList)update).expressions;
		} else {
			updates = Collections.singletonList(update);
		}
		
		for (Node n : inits) if (n != null) result.inits().addToEndRaw(n);
		for (Node n : updates) if (n != null) result.updates().addToEndRaw(n);
		return result;
	}
	
	public Node createEnhancedFor(Node modifiers, Node type, Node varName, List<String> dims, Node iterable, Node statement) {
		VariableDefinition decl = new VariableDefinition().setRawTypeReference(type).variables().addToEndRaw(
				new VariableDefinitionEntry().setRawName(varName).setDimensions(dims == null ? 0 : dims.size()));
		if (modifiers != null) decl.setRawModifiers(modifiers);
		return new ForEach().setRawVariable(decl).setRawIterable(iterable).setRawStatement(statement);
	}
	
	public Node createBreak(Node label) {
		return new Break().setRawLabel(label);
	}
	
	public Node createContinue(Node label) {
		return new Continue().setRawLabel(label);
	}
	
	public Node createReturn(Node value) {
		return new Return().setRawValue(value);
	}
	
	public Node createThrow(Node throwable) {
		return new Throw().setRawThrowable(throwable);
	}
	
	public Node createSynchronizedStatement(Node lock, Node body) {
		return new Synchronized().setRawLock(lock).setRawBody(body);
	}
	
	public Node createCatch(Node modifiers, Node type, Node varName, Node body) {
		VariableDefinition decl = new VariableDefinition().setRawTypeReference(type).variables().addToEndRaw(
				new VariableDefinitionEntry().setRawName(varName));
		if (modifiers != null) decl.setRawModifiers(modifiers);
		return new Catch().setRawExceptionDeclaration(decl).setRawBody(body);
	}
	
	public Node createTryStatement(Node body, List<Node> catches, Node finallyBody) {
		Try result = new Try().setRawBody(body).setRawFinally(finallyBody);
		if (catches != null) for (Node c : catches) if (c != null) result.catches().addToEndRaw(c);
		return result;
	}
	
	public Node addLocalVariableModifiers(Node variableDefinition, Node modifiers) {
		if (modifiers != null && variableDefinition instanceof VariableDefinition) {
			((VariableDefinition)variableDefinition).setRawModifiers(modifiers);
		}
		
		return variableDefinition;
	}
	
	public Node createAlternateConstructorInvocation(Node typeArguments, Node arguments) {
		MethodInvocation args = (arguments instanceof MethodInvocation) ? (MethodInvocation)arguments : new MethodInvocation();
		return new AlternateConstructorInvocation().setRawConstructorTypeArguments(typeArguments).arguments().migrateAllFromRaw(args.arguments());
	}
	
	public Node createSuperConstructorInvocation(Node qualifier, Node typeArguments, Node arguments) {
		MethodInvocation args = (arguments instanceof MethodInvocation) ? (MethodInvocation)arguments : new MethodInvocation();
		return new SuperConstructorInvocation().setRawQualifier(qualifier).setRawConstructorTypeArguments(typeArguments).arguments().migrateAllFromRaw(args.arguments());
	}
	
	public Node createExpressionStatement(Node expression) {
		return new ExpressionStatement().setRawExpression(expression);
	}
	
	public Node createVariableDeclaration(Node definition) {
		return new VariableDeclaration().setRawDefinition(definition);
	}
}
