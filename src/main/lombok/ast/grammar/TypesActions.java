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
import lombok.ast.Type;
import lombok.ast.TypePart;
import lombok.ast.TypeVariable;
import lombok.ast.WildcardKind;

import org.parboiled.BaseActions;

public class TypesActions extends BaseActions<Node> {
	public Node createPrimitiveType(String text) {
		return new Type().parts().addToStartRaw(new TypePart().setRawIdentifier(new Identifier().setName(text)));
	}
	
	public Node createTypePart(Node identifier, Node typePart) {
		if (!(typePart instanceof TypePart)) return new TypePart().setRawIdentifier(identifier);
		//todo add screwed up typePart as dangling tail to returned node.
		
		return ((TypePart)typePart).setRawIdentifier(identifier);
	}
	
	public Node createWildcardedType(String extendsOrSuper, Node type) {
		WildcardKind wildcard = WildcardKind.UNBOUND;
		if (extendsOrSuper != null) extendsOrSuper = extendsOrSuper.trim();
		if ("extends".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.EXTENDS;
		if ("super".equalsIgnoreCase(extendsOrSuper)) wildcard = WildcardKind.SUPER;
		
		if (!(type instanceof Type)) return new Type().setWildcard(wildcard);
		//todo add screwed up typePart as dangling tail to returned node.
		
		return ((Type)type).setWildcard(wildcard);
	}
	
	public Node createUnboundedWildcardType() {
		return new Type().setWildcard(WildcardKind.UNBOUND);
	}
	
	public Node createTypeArguments(Node head, List<Node> tail) {
		TypePart tp = new TypePart();
		if (head != null) tp.generics().addToEndRaw(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) tp.generics().addToEndRaw(n);
		}
		
		return tp;
	}
	
	public Node createReferenceType(Node head, List<Node> tail) {
		Type t = new Type();
		if (head != null) t.parts().addToEndRaw(head);
		if (tail != null) for (Node n : tail) {
			if (n != null) t.parts().addToEndRaw(n);
		}
		
		return t;
	}
	
	public Node addArrayDimensionsToType(Node value, List<String> bracketPairs) {
		if (value instanceof Type) {
			int arrayDimensions = bracketPairs == null ? 0 : bracketPairs.size();
			return ((Type)value).setArrayDimensions(arrayDimensions);
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
