package lombok.ast;

import java.util.List;

import lombok.Getter;

public class Identifier extends Node {
	@Getter private String name;
	
	public Identifier setName(String name) {
		this.name = name;
		return this;
	}
	
	@Override public void checkSyntacticValidity(List<SyntaxProblem> problems) {
		if (name == null || !name.isEmpty()) problems.add(new SyntaxProblem(this, "nameless identifier"));
	}
	
	@Override public void accept(ASTVisitor visitor) {
		visitor.visitIdentifier(this);
	}
}
