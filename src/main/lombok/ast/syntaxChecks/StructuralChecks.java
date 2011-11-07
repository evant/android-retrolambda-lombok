/*
 * Copyright (C) 2010 The Project Lombok Authors.
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

import lombok.ast.AlternateConstructorInvocation;
import lombok.ast.Block;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.InstanceInitializer;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.Statement;
import lombok.ast.StaticInitializer;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Throw;
import lombok.ast.TypeDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StructuralChecks {
	public void checkAbstractMembersOnlyInAbstractTypes(MethodDeclaration md) {
		Modifiers modifiers = md.astModifiers();
		if (modifiers == null) return;
		if (!modifiers.isAbstract()) return;
		TypeDeclaration parent = md.upUpToTypeDeclaration();
		if (parent != null) {
			Modifiers modifiersOfParent = parent.astModifiers();
			if (modifiersOfParent != null && modifiersOfParent.isAbstract()) return;
			md.addMessage(error(MODIFIERS_ABSTRACT_NOT_ALLOWED, "Abstract methods are only allowed in interfaces and abstract classes"));
		}
	}
	
	public void initializersMustCompleteNormallyStatic(StaticInitializer node) {
		initializersMustCompleteNormally(node.rawBody());
	}
	
	public void initializersMustCompleteNormallyInstance(InstanceInitializer node) {
		initializersMustCompleteNormally(node.rawBody());
	}
	
	private void initializersMustCompleteNormally(Node rawBlock) {
		if (!(rawBlock instanceof Block)) return;
		for (Node s : ((Block)rawBlock).rawContents()) {
			if (s instanceof Throw) s.addMessage(error(INITIALIZERS_INITIALIZER_MUST_COMPLETE_NORMALLY, "Initializers may not contain throws statements."));
			if (s instanceof Return) s.addMessage(error(INITIALIZERS_INITIALIZER_MUST_COMPLETE_NORMALLY, "Initializers may not contain return statements."));
		}
	}
	
	public void superConstructorInvocationMustBeFirst(SuperConstructorInvocation node) {
		constructorInvocationMustBeFirst(node, "super");
	}
	
	public void alternateConstructorInvocationMustBeFirst(AlternateConstructorInvocation node) {
		constructorInvocationMustBeFirst(node, "this");
	}
	
	private void constructorInvocationMustBeFirst(Statement node, String desc) {
		if (node.getParent() == null) return;
		Block b = node.upToBlock();
		if (b == null || b.upToConstructorDeclaration() == null || b.astContents().first() != node) {
			node.addMessage(error(CONSTRUCTOR_INVOCATION_NOT_LEGAL_HERE, "Calling " + desc + " must be the first statement in a constructor."));
			return;
		}
	}
	
	public void varDefOfZero(VariableDefinition node) {
		if (node.astVariables().isEmpty()) {
			node.addMessage(error(VARIABLEDEFINITION_EMPTY, "Empty variable declaration."));
		}
	}
	
	public void varargsOnlyLegalOnMethods(VariableDefinition node) {
		if (!node.astVarargs()) return;
		if (node.getParent() == null) return;
		
		MethodDeclaration md = node.upIfParameterToMethodDeclaration();
		ConstructorDeclaration cd = node.upIfParameterToConstructorDeclaration();
		Node last;
		
		if (md != null) last = md.astParameters().last();
		else if (cd != null) last = cd.astParameters().last();
		else last = null;
		
		if (node != last) {
			node.addMessage(error(VARIABLEDEFINITION_VARARGS_NOT_LEGAL_HERE, "Varargs are only legal on the last parameter of a constructor or method."));
		}
	}
	
	public void varargsAndExtendedDimsDontMix(VariableDefinitionEntry node) {
		VariableDefinition vd = node.upToVariableDefinition();
		if (vd == null) return;
		if (node.astArrayDimensions() > 0 && vd.astVarargs()) {
			node.addMessage(error(VARIABLEDEFINITIONENTRY_EXTENDED_DIMENSIONS_NOT_LEGAL, "Extended dimensions are not legal on a varargs declaration."));
		}
	}
	
	public void checkMethodParamsAreSimple(MethodDeclaration md) {
		for (Node param : md.rawParameters()) {
			BasicChecks.checkVarDefIsSimple(param, param, "method parameters", "parameter");
		}
	}
	
	public void checkConstructorParamsAreSimple(ConstructorDeclaration cd) {
		for (Node param : cd.rawParameters()) {
			BasicChecks.checkVarDefIsSimple(param, param, "constructor parameters", "parameter");
		}
	}
}
