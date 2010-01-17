package lombok.ast.grammar;

import java.util.List;

import lombok.ast.Assert;
import lombok.ast.Block;
import lombok.ast.Case;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.EmptyStatement;
import lombok.ast.If;
import lombok.ast.LabelledStatement;
import lombok.ast.Node;
import lombok.ast.Switch;
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
}
