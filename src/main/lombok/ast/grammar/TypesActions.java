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
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.WildcardKind;

public class TypesActions extends SourceActions {
	public TypesActions(Source source) {
		super(source);
	}
	
	public Node createPrimitiveType(String text) {
		Identifier identifier = posify(new Identifier().astValue(text));
		TypeReferencePart typeReferencePart = posify(new TypeReferencePart()
				.astIdentifier(identifier));
		return posify(new TypeReference().rawParts().addToStart(typeReferencePart));
	}
	
	public Node createTypeReferencePart(org.parboiled.Node<Node> identifier, Node typeArguments) {
		TypeReferencePart result = new TypeReferencePart().astIdentifier(createIdentifierIfNeeded(identifier.getValue(), currentPos()));
		
		if (typeArguments instanceof TemporaryNode.TypeArguments) {
			for (Node arg : ((TemporaryNode.TypeArguments)typeArguments).arguments) {
				result.rawTypeArguments().addToEnd(arg);
			}
		}
		
		posify(result); //We only care about the end position here.
		return result.setPosition(new Position(identifier.getStartLocation().getIndex(), result.getPosition().getEnd()));
	}
	
	public Node createWildcardedType(org.parboiled.Node<Node> qmark, org.parboiled.Node<Node> boundType, String extendsOrSuper, Node type) {
		if (extendsOrSuper != null) extendsOrSuper = extendsOrSuper.trim();
		WildcardKind wildcard = WildcardKind.UNBOUND;
		if ("extends".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.EXTENDS;
		if ("super".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.SUPER;
		
		TypeReference ref;
		
		if (!(type instanceof TypeReference)) {
			ref = new TypeReference();
			ref.addDanglingNode(type);
		} else {
			ref = (TypeReference)type;
		}
		
		ref.astWildcard(wildcard);
		source.registerStructure(ref, qmark);
		for (org.parboiled.Node<Node> childPNode : boundType.getChildren()) {
			if (childPNode != null) source.registerStructure(ref, childPNode);
		}
		return posify(ref);
	}
	
	public Node createUnboundedWildcardType(org.parboiled.Node<Node> qmark) {
		TypeReference ref = new TypeReference().astWildcard(WildcardKind.UNBOUND);
		source.registerStructure(ref, qmark);
		return posify(ref);
	}
	
	public Node createTypeArguments(Node head, List<Node> tail) {
		TemporaryNode.TypeArguments ta = new TemporaryNode.TypeArguments();
		if (head != null) ta.arguments.add(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) ta.arguments.add(n);
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
		
		TypeReference ref = new TypeReference().astArrayDimensions(arrayDims);
		if (value instanceof TypeReference) {
			TypeReference orig = (TypeReference)value;
			ref.astWildcard(orig.astWildcard());
			ref.rawParts().migrateAllFrom(orig.rawParts());
		}
		return posify(ref);
	}
	
	public Node createTypeVariable(Node name, Node head, List<Node> tail) {
		TypeVariable tv = new TypeVariable().astName(createIdentifierIfNeeded(name, currentPos()));
		
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
