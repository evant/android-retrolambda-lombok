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

import static lombok.ast.syntaxChecks.MessageKey.*;
import static lombok.ast.Message.*;

import java.util.Map;

import com.google.common.collect.Maps;

import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;

/**
 * The base class of {@code SyntacticValidityVisitor}, which is generated. Don't use this class, use
 * the generated {@code SyntacticValidityVisitor}.
 */
public class SyntacticValidityVisitorBase extends ForwardingAstVisitor {
	final Map<Class<?>, Object> checkerObjectStore = Maps.newHashMap();
	final boolean recursing;
	
	SyntacticValidityVisitorBase(boolean recursing) {
		this.recursing = recursing;
	}
	
	@SuppressWarnings("unchecked")
	<T> T getCheckerObject(Class<T> clazz) {
		Object o = checkerObjectStore.get(clazz);
		if (o != null) return (T)o;
		try {
			o = clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Class " + clazz.getName() + " could not be constructed. Does it have a public no-args constructort?", e);
		}
		checkerObjectStore.put(clazz, o);
		return (T)o;
	}
	
	/**
	 * Checks if a given child is syntactically valid; you specify exactly what is required for this to hold in the parameters.
	 * This method will recursively call {@link #checkSyntacticValidity()} on the child.
	 * 
	 * @param node The node responsible for the check.
	 * @param child The actual child node object that will be checked.
	 * @param name The name of this node. For example, in a binary operation, {@code "left side"}.
	 * @param mandatory If this node not being there (being {@code null}) is a problem or acceptable.
	 * @param typeAssertion If the node exists, it must be an instance of this type.
	 */
	void checkChildValidity(Node node, Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		verifyNodeRelation(node, child, name, mandatory, typeAssertion);
	}
	
	public static boolean verifyNodeRelation(Node parent, Node child, String name, boolean mandatory, Class<?> typeAssertion) {
		String typeAssertionName = typeAssertion.getSimpleName().toLowerCase();
		boolean typeAssertionVowel = startsWithVowel(typeAssertionName);
		
		if (child == null) {
			if (mandatory) {
				parent.addMessage(error(NODE_MISSING_MANDATORY_CHILD, String.format("Missing %s %s",
						name, typeAssertion.getSimpleName().toLowerCase())));
				return false;
			}
		} else {
			if (!typeAssertion.isInstance(child)) {
				String actualName = child.getClass().getSimpleName();
				child.addMessage(error(NODE_CHILD_TYPE_INCORRECT, String.format(
						"%s isn't a%s %s but a%s %s",
						name,
						typeAssertionVowel ? "n" : "", typeAssertionName,
						startsWithVowel(actualName) ? "n" : "", actualName)));
				return false;
			}
		}
		
		return true;
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
		node.addMessage(error(PARSEARTEFACT, "parse artefact remained in node tree"));
		
		return !recursing;
	}
}
