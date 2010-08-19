package lombok.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.ast.grammar.Source;

public class Template<T extends Node> {
	private static <N extends Node> N process(Source s, String name, Class<N> type) {
		if (!s.getProblems().isEmpty()) {
			throw new AstException(null, "Can't parse snippet: " + s.getProblems().get(0).getMessage());
		}
		if (s.getNodes().isEmpty()) return null;
		if (s.getNodes().size() > 1) throw new AstException(null, "Can't parse snippet: more than one " + name + " in snippet");
		Node n = s.getNodes().get(0);
		if (type.isInstance(n)) return type.cast(n);
		throw new AstException(null, "Can't parse snippet: Not a " + name);
	}
	
	/**
	 * Parses one construct that is legal as a type member, and returns it.
	 * 
	 * Legal type members: <ul>
	 * <li>Constructor Declaration</li>
	 * <li>Method Declaration</li>
	 * <li>Static or Instance Initializer</li>
	 * <li>Any type declaration</li>
	 * <li>The empty declaration (lone semi-colon)</li>
	 * <li>A variable declaration</li>
	 * </ul>
	 * 
	 * Note that neither annotation method declarations nor enum constants will be parsed properly by this method.
	 */
	public static TypeMember parseMember(String source) throws AstException {
		Source s = new Source(source, "memberSnippet");
		s.parseMember();
		return process(s, "type member", TypeMember.class);
	}
	
	public static MethodDeclaration parseMethod(String source) throws AstException {
		Source s = new Source(source, "methodSnippet");
		s.parseMember();
		return process(s, "method", MethodDeclaration.class);
	}
	
	public static VariableDeclaration parseField(String source) throws AstException {
		Source s = new Source(source, "fieldSnippet");
		s.parseMember();
		return process(s, "field", VariableDeclaration.class);
	}
	
	public static Statement parseStatement(String source) throws AstException {
		Source s = new Source(source, "statementSnippet");
		s.parseStatement();
		return process(s, "statement", Statement.class);
	}
	
	public static Expression parseExpression(String source) throws AstException {
		Source s = new Source(source, "expressionSnippet");
		s.parseExpression();
		return process(s, "expression", Expression.class);
	}
	
	/**
	 * NB: Do not simply pass the result of {@code parseX} to this method; parsing is extremely slow. Instead, parse a template once,
	 * and then pass this one result every time. The template will never modify the original.
	 */
	@SuppressWarnings("unchecked")
	public static <N extends Node> Template<N> of(N source) throws AstException {
		return new Template<N>((N) source.copy());
	}
	
	private final T node;
	private int location;
	private Node responsible;
	private List<ReplacementOrder> replacements = new ArrayList<ReplacementOrder>();
	private int replacementsPointer = 0;
	
	private static class ReplacementOrder {
		String identifierToReplace, statementToReplace, expressionToReplace;
		List<? extends Node> replacement;
		Position position;
		
		static ReplacementOrder forIdentifier(String identifier, String newValue, Position position) {
			ReplacementOrder order = new ReplacementOrder();
			order.identifierToReplace = identifier;
			order.replacement = Collections.singletonList(newValue == null ? null : Identifier.of(newValue));
			order.position = position;
			return order;
		}
		
		static ReplacementOrder forStatement(String label, List<Statement> replacements, Position position) {
			ReplacementOrder order = new ReplacementOrder();
			order.statementToReplace = label;
			order.replacement = replacements == null ? Collections.<Node>emptyList() : replacements;
			order.position = position;
			return order;
		}
		
		static ReplacementOrder forExpression(String identifier, Expression replacement, Position position) {
			ReplacementOrder order = new ReplacementOrder();
			order.expressionToReplace = identifier;
			order.replacement = Collections.singletonList(replacement);
			order.position = position;
			return order;
		}
	}
	
	private final AstVisitor visitor = new ForwardingAstVisitor() {
		@Override public void endVisit(Node node) {
			node.setPosition(new Position(node.getPosition().getStart(), location, responsible));
		}
		
		private ReplacementOrder currentOrder() {
			return (replacementsPointer < replacements.size()) ? replacements.get(replacementsPointer) : null;
		}
		
		@Override public boolean visitIdentifier(Identifier node) {
			ReplacementOrder order = currentOrder();
			if (order != null && order.identifierToReplace != null) {
				if (order.identifierToReplace.equals(node.astValue())) {
					Node replacement = order.replacement.get(0);
					int startLoc = order.position == null ? location : order.position.getStart();
					int endLoc = order.position == null ? location : order.position.getEnd();
					replacement.setPosition(new Position(startLoc, endLoc, responsible));
					location = endLoc;
					node.replace(replacement);
					replacementsPointer++;
					return true;
				}
			}
			
			return visitNode(node);
		}
		
		@Override public boolean visitLabelledStatement(LabelledStatement node) {
			ReplacementOrder order = currentOrder();
			if (order != null && order.statementToReplace.equals(node.astLabel().astValue())) {
				if (!(node.rawStatement() instanceof EmptyStatement)) {
					throw new IllegalStateException("Placeholder statements in templates should be of the form: \"labelName: ;\" - i.e. a labelled empty statement");
				}
				
				int startLoc, endLoc;
				if (order.position == null) {
					if (order.replacement.isEmpty() || order.replacement.get(0).getPosition().getStart() < 0) startLoc = location;
					else startLoc = order.replacement.get(0).getPosition().getStart();
					if (order.replacement.isEmpty() || order.replacement.get(order.replacement.size() - 1).getPosition().getEnd() < 0) endLoc = location;
					else endLoc = order.replacement.get(order.replacement.size() - 1).getPosition().getEnd();
				} else {
					startLoc = order.position.getStart();
					endLoc = order.position.getEnd();
				}
				switch (order.replacement.size()) {
				case 0:
					node.unparent();
					break;
				case 1:
					Node replacement = order.replacement.get(0);
					if (replacement.getPosition().isUnplaced()) Ast.setAllPositions(replacement, new Position(startLoc, endLoc, responsible));
					node.replace(replacement);
					break;
				default:
					// Replacing multiple statements only works in a Block.
					Block b = node.upToBlock();
					if (b == null) throw new IllegalStateException("Replacing one placeholder statement with multiple statements is legal only if the placeholder is in a block");
					b.rawContents().addAfter(node, order.replacement.toArray(new Node[0]));
					node.unparent();
					for (Node n : order.replacement) {
						if (n.getPosition().isUnplaced()) Ast.setAllPositions(n, new Position(startLoc, endLoc, responsible));
					}
				}
				replacementsPointer++;
				location = endLoc;
				return true;
			}
			
			return visitNode(node);
		}
		
		@Override public boolean visitVariableReference(VariableReference node) {
			ReplacementOrder order = currentOrder();
			if (order != null && order.expressionToReplace.equals(node.astIdentifier().astValue())) {
				Node replacement = order.replacement.get(0);
				int startLoc, endLoc;
				if (order.position == null) {
					if (order.replacement.isEmpty() || replacement.getPosition().getStart() < 0) startLoc = location;
					else startLoc = replacement.getPosition().getStart();
					if (order.replacement.isEmpty() || replacement.getPosition().getEnd() < 0) endLoc = location;
					else endLoc = replacement.getPosition().getEnd();
				} else {
					startLoc = order.position.getStart();
					endLoc = order.position.getEnd();
				}
				if (replacement.getPosition().isUnplaced()) Ast.setAllPositions(replacement, new Position(startLoc, endLoc, responsible));
				location = endLoc;
				node.replace(replacement);
				replacementsPointer++;
				return true;
			}
			
			return visitNode(node);
		}
		
		@Override public boolean visitNode(Node node) {
			node.setPosition(new Position(location, location, responsible));
			return false;
		}
	};
	
	private Template(T node) {
		this.node = node;
	}
	
	public Template<T> setStartPosition(int location) {
		this.location = location;
		return this;
	}
	
	public Template<T> setResponsibleNode(Node responsible) {
		this.responsible = responsible;
		return this;
	}
	
	public Template<T> replaceIdentifier(String placeholder, String replacement, Position p) {
		this.replacements.add(ReplacementOrder.forIdentifier(placeholder, replacement, p));
		return this;
	}
	
	public Template<T> replaceIdentifier(String placeholder, String replacement) {
		return replaceIdentifier(placeholder, replacement, null);
	}
	
	public Template<T> replaceStatement(String placeholder, Statement replacement, Position p) {
		this.replacements.add(ReplacementOrder.forStatement(placeholder, Collections.singletonList(replacement), p));
		return this;
	}
	
	public Template<T> replaceStatement(String placeholder, Statement replacement) {
		return replaceStatement(placeholder, replacement, null);
	}
	
	public Template<T> replaceStatement(String placeholder, List<Statement> replacement, Position p) {
		this.replacements.add(ReplacementOrder.forStatement(placeholder, replacement, p));
		return this;
	}
	
	public Template<T> replaceStatement(String placeholder, List<Statement> replacement) {
		return replaceStatement(placeholder, replacement, null);
	}
	
	public Template<T> replaceExpression(String placeholder, Expression replacement, Position p) {
		this.replacements.add(ReplacementOrder.forExpression(placeholder, replacement, p));
		return this;
	}
	
	public Template<T> replaceExpression(String placeholder, Expression replacement) {
		return replaceExpression(placeholder, replacement, null);
	}
	
	public T finish() {
		node.accept(visitor);
		return node;
	}
}
