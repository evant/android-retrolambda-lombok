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
package lombok.ast.syntaxChecks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.SyntaxProblem;

/**
 * The base class of {@code SyntacticValidityVisitor}, which is generated. Don't use this class, use
 * the generated {@code SyntacticValidityVisitor}.
 */
public class SyntacticValidityVisitorBase extends ForwardingAstVisitor {
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
		boolean typeAssertionVowel = startsWithVowel(typeAssertionName);
		
		if (child == null) {
			if (mandatory) {
				return new SyntaxProblem(parent, String.format("Missing %s %s",
						name, typeAssertion.getSimpleName().toLowerCase()));
			}
		} else {
			if (!typeAssertion.isInstance(child)) {
				String actualName = child.getClass().getSimpleName();
				return new SyntaxProblem(parent, String.format(
						"%s isn't a%s %s but a%s %s",
						name,
						typeAssertionVowel ? "n" : "", typeAssertionName,
						startsWithVowel(actualName) ? "n" : "", actualName));
			}
		}
		
		return null;
	}
	
	private static boolean startsWithVowel(String typeAssertionName) {
		boolean typeAssertionVowel = typeAssertionName.isEmpty();
		if (!typeAssertionVowel) {
			char c = typeAssertionName.charAt(0);
			typeAssertionVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
		}
		return typeAssertionVowel;
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
