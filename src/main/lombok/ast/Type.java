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
