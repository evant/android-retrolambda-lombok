/*
 * Copyright © 2011 Reinier Zwitserloot and Roel Spilker.
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
package lombok.ast.app;

import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.app.Main.ConversionProblem;
import lombok.ast.app.Main.Operation;
import lombok.ast.grammar.Source;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;

class EcjBugsNormalization {
	private EcjBugsNormalization() {}
	
	static final Operation<Node, Node> lombokToEcjBugsNormalizedLombok = new Operation<Node, Node>() {
		@Override public Node process(Source source, Node in) throws ConversionProblem {
			in.accept(new ForwardingAstVisitor() {
				public boolean visitMethodDeclaration(lombok.ast.MethodDeclaration node) {
					if (!node.astModifiers().astAnnotations().isEmpty()) node.astJavadoc(null);
					return true;
				}
				
				public boolean visitAnnotationMethodDeclaration(lombok.ast.AnnotationMethodDeclaration node) {
					if (!node.astModifiers().astAnnotations().isEmpty()) node.astJavadoc(null);
					return true;
				}
			});
			
			return in;
		}
	};
	
	static final Operation<CompilationUnitDeclaration, CompilationUnitDeclaration> ecjToEcjBugsNormalizedEcj = new Operation<CompilationUnitDeclaration, CompilationUnitDeclaration>() {
		@Override public CompilationUnitDeclaration process(Source source, CompilationUnitDeclaration in) throws ConversionProblem {
			in.traverse(new ASTVisitor() {
				public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
					if (methodDeclaration.annotations != null && methodDeclaration.annotations.length > 0) methodDeclaration.javadoc = null;
					return true;
				}
				
				public boolean visit(AnnotationMethodDeclaration methodDeclaration, ClassScope classScope) {
					if (methodDeclaration.annotations != null && methodDeclaration.annotations.length > 0) methodDeclaration.javadoc = null;
					return true;
				}
				
				public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
					if (typeDeclaration.annotations != null && typeDeclaration.annotations.length > 0) typeDeclaration.javadoc = null;
					return true;
				}
				
				public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
					if (typeDeclaration.annotations != null && typeDeclaration.annotations.length > 0) typeDeclaration.javadoc = null;
					return true;
				}
				
				public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
					if (typeDeclaration.annotations != null && typeDeclaration.annotations.length > 0) typeDeclaration.javadoc = null;
					return true;
				}
			}, (CompilationUnitScope) null);
			
			return in;
		}
	};
	

}
