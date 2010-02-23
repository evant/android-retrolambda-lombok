package lombok.ast.syntaxChecks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ast.ForwardingASTVisitor;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;

public class SyntacticValidityVisitorBase extends ForwardingASTVisitor {
	final Map<Class<?>, Object> checkerObjectStore = new HashMap<Class<?>, Object>();
	final boolean recursing;
	final List<SyntaxProblem> problems;
	
	SyntacticValidityVisitorBase(List<SyntaxProblem> problems, boolean recursing) {
		this.problems = problems;
		this.recursing = recursing;
	}
	
	@SuppressWarnings("unchecked")
	<T> T getCheckerObject(Class<T> clazz) {
		Object o = checkerObjectStore.get(clazz);
		if (o != null) return (T)o;
		try {
			o = clazz.getConstructor(List.class).newInstance(problems);
		} catch (Exception e) {
			throw new IllegalStateException("Class " + clazz.getName() + " could not be constructed. Does it have a public constructor that takes a list?", e);
		}
		checkerObjectStore.put(clazz, o);
		return (T)o;
	}
	
	/**
	 * Checks if a given child is syntactically valid; you specify exactly what is required for this to hold in the parameters.
	 * This method will recursively call {@link #checkSyntacticValidity(List)} on the child.
	 * 
	 * @param node The node responsible for the check.
	 * @param child The actual child node object that will be checked.
	 * @param name The name of this node. For example, in a binary operation, {@code "left side"}.
	 * @param mandatory If this node not being there (being {@code null}) is a problem or acceptable.
	 * @param typeAssertion If the node exists, it must be an instance of this type.
	 */
	void checkChildValidity(Node node, Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		SyntaxProblem p = verifyNodeRelation(node, child, name, mandatory, typeAssertion);
		if (p != null) problems.add(p);
	}
	
	public static SyntaxProblem verifyNodeRelation(Node parent, Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		String typeAssertionName = typeAssertion.getSimpleName().toLowerCase();
		boolean typeAssertionVowel = typeAssertionName.isEmpty();
		if (!typeAssertionVowel) {
			char c = typeAssertionName.charAt(0);
			typeAssertionVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
		}
		
		if (child == null) {
			if (mandatory) {
				return new SyntaxProblem(parent, String.format("Missing %s %s",
						name, typeAssertion.getSimpleName().toLowerCase()));
			}
		} else {
			if (!typeAssertion.isInstance(child)) {
				return new SyntaxProblem(parent, String.format(
						"%s isn't a%s %s",
						name, typeAssertionVowel ? "n" : "", typeAssertionName));
			}
		}
		
		return null;
	}
	
	@Override public boolean visitParseArtefact(Node node) {
		StringBuilder errorName = new StringBuilder();
		boolean first = true;
		for (char c : node.getClass().getSimpleName().toCharArray()) {
			if (first) {
				errorName.append(c);
				first = false;
				continue;
			}
			
			if (Character.isUpperCase(c)) errorName.append(" ").append(Character.toLowerCase(c));
			else errorName.append(c);
		}
		problems.add(new SyntaxProblem(node, errorName.toString()));
		
		return !recursing;
	}
}
