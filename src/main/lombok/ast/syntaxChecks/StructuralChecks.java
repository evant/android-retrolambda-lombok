package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Block;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.StaticInitializer;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.SyntaxProblem;
import lombok.ast.Throw;
import lombok.ast.TypeDeclaration;
import lombok.ast.TypeReference;
import lombok.ast.VariableDefinition;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StructuralChecks {
	private final List<SyntaxProblem> problems;
	
	public StructuralChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkAbstractMembersOnlyInAbstractTypes(MethodDeclaration md) {
		Node rawModifiers = md.getRawModifiers();
		if (!(rawModifiers instanceof Modifiers)) return;
		if (!((Modifiers)rawModifiers).isAbstract()) return;
		if (md.getParent() instanceof TypeDeclaration) {
			Node rawModifiersOfParent = ((TypeDeclaration)md.getParent()).getRawModifiers();
			if (!(rawModifiersOfParent instanceof Modifiers)) return;
			if (((Modifiers)rawModifiers).isAbstract()) return;
			problems.add(new SyntaxProblem(md, "Abstract methods are only allowed in interfaces and abstract classes"));
		}
	}
	
	public void initializersMustNotContainReturn(Return node) {
		Node parent = node;
		while (parent != null) {
			if (parent instanceof MethodDeclaration) return;
			if (parent instanceof StaticInitializer || parent instanceof InstanceInitializer) {
				problems.add(new SyntaxProblem(node, "Initializers cannot contain return statements."));
			}
			parent = parent.getParent();
		}
	}
	
	public void initializersMustNotContainThrowsDirectlyStatic(StaticInitializer node) {
		initializersMustNotContainThrowsDirectly(node.getRawBody());
	}
	
	public void initializersMustNotContainThrowsDirectlyInstance(InstanceInitializer node) {
		initializersMustNotContainThrowsDirectly(node.getRawBody());
	}
	
	private void initializersMustNotContainThrowsDirectly(Node rawBlock) {
		if (!(rawBlock instanceof Block)) return;
		for (Node s : ((Block)rawBlock).contents().getRawContents()) {
			if (s instanceof Throw) {
				problems.add(new SyntaxProblem(s, "Initializers must complete normally."));
			}
		}
	}
	
	public void returnValueVsVoidReturnType(Return node) {
		boolean shouldBeVoid = node.getRawValue() == null;
		Node parent = node;
		while (parent != null) {
			if (parent instanceof ConstructorDeclaration) {
				if (!shouldBeVoid) {
					problems.add(new SyntaxProblem(node, "Constructors cannot return a value."));
				}
				return;
			}
			if (parent instanceof MethodDeclaration) {
				Node rawTR = ((MethodDeclaration)parent).getRawReturnTypeReference();
				if (rawTR instanceof TypeReference) {
					if (((TypeReference)rawTR).isVoid()) {
						if (!shouldBeVoid) {
							problems.add(new SyntaxProblem(node, "void methods cannot return a value."));
						}
					} else {
						if (shouldBeVoid) {
							problems.add(new SyntaxProblem(node, "method should return a value."));
						}
					}
				}
				return;
			}
		}
	}
	
	public void constructorInvocationsOnlyInConstructorsSuper(SuperConstructorInvocation node) {
		constructorInvocationsOnlyInConstructors(node);
	}
	
	public void constructorInvocationsOnlyInConstructorsThis(AlternateConstructorInvocation node) {
		constructorInvocationsOnlyInConstructors(node);
	}
	
	private void constructorInvocationsOnlyInConstructors(Node node) {
		Node parent = node;
		while (parent != null) {
			if (parent instanceof ConstructorDeclaration) return;
			if (parent instanceof MethodDeclaration) {
				problems.add(new SyntaxProblem(node, "Calling another constructor directly is only allowed inside other constructors."));
				return;
			}
		}
	}
	
	public void superConstructorInvocationMustBeFirst(SuperConstructorInvocation node) {
		Node parent = node;
		while (parent != null) {
			if (parent instanceof ConstructorDeclaration) {
				Node rawBlock = ((ConstructorDeclaration)parent).getRawBody();
				if (rawBlock instanceof Block) {
					Node n = ((Block)rawBlock).contents().rawFirst();
					if (n != node) {
						problems.add(new SyntaxProblem(node,
								"Calling super must be the first statement in a constructor."));
					}
				}
				return;
			}
		}
	}
	
	public void varDefOfZero(VariableDefinition node) {
		if (node.variables().isEmpty()) {
			problems.add(new SyntaxProblem(node, "Empty variable declaration."));
		}
	}
	
	public void varargsOnlyLegalOnMethods(VariableDefinition node) {
		if (!node.isVarargs()) return;
		Node p = node.getParent();
		if (p == null) return;
		Node last = null;
		if (p instanceof ConstructorDeclaration) {
			last = ((ConstructorDeclaration)p).parameters().rawLast();
		} else if (p instanceof MethodDeclaration) {
			last = ((MethodDeclaration)p).parameters().rawLast();
		}
		
		if (last != node) problems.add(new SyntaxProblem(node, "VarArgs are only legal on the last parameter of a method or constructor declaration."));
	}
}
