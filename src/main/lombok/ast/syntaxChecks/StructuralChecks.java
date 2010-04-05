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
import lombok.ast.VariableDefinitionEntry;
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
		for (Node s : ((Block)rawBlock).rawContents()) {
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
			
			parent = parent.getParent();
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
			
			parent = parent.getParent();
		}
	}
	
	public void superConstructorInvocationMustBeFirst(SuperConstructorInvocation node) {
		Node parent = node;
		while (parent != null) {
			if (parent instanceof ConstructorDeclaration) {
				Node rawBlock = ((ConstructorDeclaration)parent).getRawBody();
				if (rawBlock instanceof Block) {
					Node n = ((Block)rawBlock).rawContents().first();
					if (n != node) {
						problems.add(new SyntaxProblem(node,
								"Calling super must be the first statement in a constructor."));
					}
				}
				return;
			}
			
			parent = parent.getParent();
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
			last = ((ConstructorDeclaration)p).rawParameters().last();
		} else if (p instanceof MethodDeclaration) {
			last = ((MethodDeclaration)p).rawParameters().last();
		}
		
		if (last != node) problems.add(new SyntaxProblem(node, "VarArgs are only legal on the last parameter of a method or constructor declaration."));
	}
	
	public void varargsAndExtendedDimsDontMix(VariableDefinitionEntry node) {
		if (node.getArrayDimensions() > 0) {
			if (node.getParent() instanceof VariableDefinition) {
				if (((VariableDefinition)node.getParent()).isVarargs()) {
					problems.add(new SyntaxProblem(node, "Extended dimensions are not legal on a varargs declaration."));
				}
			}
		}
	}
	
	public void checkMethodParamsAreSimple(MethodDeclaration md) {
		for (Node param : md.rawParameters()) {
			BasicChecks.checkVarDefIsSimple(problems, param, param, "method parameters", "parameter");
		}
	}
}
