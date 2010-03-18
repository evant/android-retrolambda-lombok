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

import lombok.ast.Identifier;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.TypeArguments;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.WildcardKind;

public class TypesActions extends SourceActions {
	public TypesActions(Source source) {
		super(source);
	}
	
	public Node createPrimitiveType(String text) {
		Identifier identifier = posify(new Identifier().setName(text));
		int endPos = identifier.getPosition().getEnd();
		TypeArguments emptyTypeArguments = new TypeArguments();
		emptyTypeArguments.setPosition(new Position(endPos, endPos));
		TypeReferencePart typeReferencePart = posify(new TypeReferencePart()
				.setRawTypeArguments(emptyTypeArguments)
				.setRawIdentifier(identifier));
		return posify(new TypeReference().rawParts().addToStart(typeReferencePart));
	}
	
	public Node createTypeReferencePart(Node identifier, Node typeArguments) {
		Node emptyArgs = null;
		if (typeArguments == null) emptyArgs = new TypeArguments().setPosition(new Position(currentPos(), currentPos()));
		return posify(new TypeReferencePart().setRawIdentifier(identifier).setRawTypeArguments(typeArguments == null ? emptyArgs : typeArguments));
	}
	
	public Node createWildcardedType(org.parboiled.Node<Node> qmark, org.parboiled.Node<Node> boundType, String extendsOrSuper, Node type) {
		if (extendsOrSuper != null) extendsOrSuper = extendsOrSuper.trim();
		WildcardKind wildcard = WildcardKind.UNBOUND;
		if ("extends".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.EXTENDS;
		if ("super".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.SUPER;
		
		TypeReference ref;
		
		if (!(type instanceof TypeReference)) {
			ref = new TypeReference();
			if (type != null) {
				//TODO add screwed up typePart as dangling tail to returned node.
			}
		} else {
			ref = (TypeReference)type;
		}
		
		ref.setWildcard(wildcard);
		source.registerStructure(ref, qmark);
		for (org.parboiled.Node<Node> childPNode : boundType.getChildren()) {
			if (childPNode != null) source.registerStructure(ref, childPNode);
		}
		return posify(ref);
	}
	
	public Node createUnboundedWildcardType(org.parboiled.Node<Node> qmark) {
		TypeReference ref = new TypeReference().setWildcard(WildcardKind.UNBOUND);
		source.registerStructure(ref, qmark);
		return posify(ref);
	}
	
	public Node createTypeArguments(Node head, List<Node> tail) {
		TypeArguments ta = new TypeArguments();
		if (head != null) ta.rawGenerics().addToEnd(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) ta.rawGenerics().addToEnd(n);
		}
		
		return posify(ta);
	}
	
	public Node createReferenceType(Node head, List<Node> tail) {
		TypeReference t = new TypeReference();
		if (head != null) t.rawParts().addToEnd(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) t.rawParts().addToEnd(n);
		}
		
		return posify(t);
	}
	
	public Node setArrayDimensionsOfType(Node value, List<String> bracketPairs) {
		//TODO test a public int foo() [] {} method declaration.
		int arrayDims = bracketPairs == null ? 0 : bracketPairs.size();
		if (arrayDims == 0) return value;
		
		TypeReference ref = new TypeReference().setArrayDimensions(arrayDims);
		if (value instanceof TypeReference) {
			TypeReference orig = (TypeReference)value;
			ref.setWildcard(orig.getWildcard());
			ref.rawParts().migrateAllFrom(orig.rawParts());
		}
		return posify(ref);
	}
	
	public Node createTypeVariable(Node name, Node head, List<Node> tail) {
		TypeVariable tv = new TypeVariable().setRawName(name);
		
		if (head != null) tv.rawExtending().addToEnd(head);
		if (tail != null) for (Node t : tail) if (t != null) tv.rawExtending().addToEnd(t);
		return posify(tv);
	}
	
	public Node createTypeVariables(Node head, List<Node> tail) {
		TemporaryNode.OrphanedTypeVariables otv = new TemporaryNode.OrphanedTypeVariables();
		if (head != null) otv.variables.add(head);
		if (tail != null) for (Node t : tail) otv.variables.add(t);
		return posify(otv);
	}
}
