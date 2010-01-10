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
package lombok.ast;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class Type extends Node {
	public static enum Wildcard {
		NONE, EXTENDS, SUPER, UNBOUND;
	}
	
	@Getter private String typeName;
	@Getter private Wildcard wildcard = Wildcard.NONE;
	private List<Type> generics = new ArrayList<Type>();
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (wildcard != Wildcard.UNBOUND && typeName == null) problems.add(new SyntaxProblem(this, "missing type identifier"));
	}
	
	@Override public void accept(ASTVisitor visitor) {
		if (visitor.visitType(this)) return;
		for (Type t : generics) t.accept(visitor);
	}
	
	public boolean hasGenerics() {
		return !generics.isEmpty();
	}
	
	public Iterable<Type> getGenerics() {
		return new ArrayList<Type>(generics);
	}
	
	public Type setUnboundWildcard() {
		this.typeName = null;
		this.wildcard = Wildcard.UNBOUND;
		for (Type t : generics) disown(t);
		generics.clear();
		return this;
	}
	
	public Type setSuperWildcard() {
		this.wildcard = Wildcard.SUPER;
		return this;
	}
	
	public Type setExtendsWildcard() {
		this.wildcard = Wildcard.EXTENDS;
		return this;
	}
	
	public Type setNoWildcard() {
		this.wildcard = Wildcard.NONE;
		return this;
	}
	
	public Type setTypeName(String name) {
		this.typeName = name;
		return this;
	}
	
	public Type addGenerics(Type parameter) {
		adopt(parameter);
		generics.add(parameter);
		return this;
	}
	
	public Type removeGenerics(Type parameter) {
		ensureParentage(parameter);
		for (int i = 0; i < generics.size(); i++) {
			if (generics.get(i) == parameter) {
				disown(parameter);
				generics.remove(i);
				return this;
			}
		}
		
		throw new IllegalStateException("Type does not contain: " + parameter);
	}
}
