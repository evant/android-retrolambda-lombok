package lombok.ast.grammar;

import java.util.Collections;
import java.util.List;

import lombok.ast.Assert;
import lombok.ast.Block;
import lombok.ast.Break;
import lombok.ast.Case;
import lombok.ast.Continue;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyStatement;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.LabelledStatement;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.Switch;
import lombok.ast.Throw;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDeclarationEntry;
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
		TemporaryNodes.StatementExpressionList result = new TemporaryNodes.StatementExpressionList();
		if (head != null) result.expressions.add(head);
		if (tail != null) for (Node n : tail) if (n != null) result.expressions.add(n);
		return result;
	}
	
	public Node createBasicFor(Node init, Node condition, Node update, Node statement) {
		For result = new For().setRawCondition(condition).setRawStatement(statement);
		List<Node> inits, updates;
		
		if (init instanceof TemporaryNodes.StatementExpressionList) {
			inits = ((TemporaryNodes.StatementExpressionList)init).expressions;
		} else {
			inits = Collections.singletonList(init);
		}
		
		if (update instanceof TemporaryNodes.StatementExpressionList) {
			updates = ((TemporaryNodes.StatementExpressionList)update).expressions;
		} else {
			updates = Collections.singletonList(update);
		}
		
		for (Node n : inits) if (n != null) result.inits().addToEndRaw(n);
		for (Node n : updates) if (n != null) result.updates().addToEndRaw(n);
		return result;
	}
	
	public Node createEnhancedFor(List<Node> modifiers, Node type, Node varName, List<String> dims, Node iterable, Node statement) {
		//TODO integrate modifiers
		VariableDeclaration decl = new VariableDeclaration().setRawTypeReference(type).variables().addToEndRaw(
				new VariableDeclarationEntry().setRawName(varName).setDimensions(dims.size()));
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
}
