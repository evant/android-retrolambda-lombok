package lombok.ast.grammar;

import java.util.Collections;
import java.util.List;

import lombok.ast.BinaryExpression;
import lombok.ast.InlineIfExpression;
import lombok.ast.Node;

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
}
