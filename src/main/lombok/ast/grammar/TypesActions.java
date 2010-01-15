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
import lombok.ast.TypeArguments;
import lombok.ast.TypeReference;
import lombok.ast.TypeReferencePart;
import lombok.ast.TypeVariable;
import lombok.ast.WildcardKind;

import org.parboiled.BaseActions;

public class TypesActions extends BaseActions<Node> {
	public Node createPrimitiveType(String text) {
		return new TypeReference().parts().addToStartRaw(new TypeReferencePart().setRawIdentifier(new Identifier().setName(text)));
	}
	
	public Node createTypeReferencePart(Node identifier, Node typeArguments) {
		return new TypeReferencePart().setRawIdentifier(identifier).setRawTypeArguments(typeArguments);
	}
	
	public Node createWildcardedType(String extendsOrSuper, Node type) {
		WildcardKind wildcard = WildcardKind.UNBOUND;
		if (extendsOrSuper != null) extendsOrSuper = extendsOrSuper.trim();
		if ("extends".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.EXTENDS;
		if ("super".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.SUPER;
		
		if (!(type instanceof TypeReference)) return new TypeReference().setWildcard(wildcard);
		//todo add screwed up typePart as dangling tail to returned node.
		
		return ((TypeReference)type).setWildcard(wildcard);
	}
	
	public Node createUnboundedWildcardType() {
		return new TypeReference().setWildcard(WildcardKind.UNBOUND);
	}
	
	public Node createTypeArguments(Node head, List<Node> tail) {
		TypeArguments ta = new TypeArguments();
		if (head != null) ta.generics().addToEndRaw(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) ta.generics().addToEndRaw(n);
		}
		
		return ta;
	}
	
	public Node createReferenceType(Node head, List<Node> tail) {
		TypeReference t = new TypeReference();
		if (head != null) t.parts().addToEndRaw(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) t.parts().addToEndRaw(n);
		}
		
		return t;
	}
	
	public Node addArrayDimensionsToType(Node value, List<String> bracketPairs) {
		if (value instanceof TypeReference) {
			int arrayDimensions = bracketPairs == null ? 0 : bracketPairs.size();
			return ((TypeReference)value).setArrayDimensions(arrayDimensions);
		}
		
		return value;
	}
	
	public Node createTypeVariable(Node name, Node head, List<Node> tail) {
		TypeVariable tv = new TypeVariable().setRawName(name);
		
		if (head != null) tv.extending().addToEndRaw(head);
		if (tail != null) for (Node t : tail) if (t != null) tv.extending().addToEndRaw(t);
		return tv;
	}
	
	public Node createTypeVariables(Node head, List<Node> tail) {
		OrphanedTypeVariables otv = new OrphanedTypeVariables();
		if (head != null) otv.variables.add(head);
		if (tail != null) for (Node t : tail) otv.variables.add(t);
		return otv;
	}
}
