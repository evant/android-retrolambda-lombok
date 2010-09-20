/*
 * Copyright © 2010 Reinier Zwitserloot, Roel Spilker and Robbert Jan Grootjans.
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Block;
import lombok.ast.CharLiteral;
import lombok.ast.Comment;
import lombok.ast.EmptyDeclaration;
import lombok.ast.For;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.StringLiteral;
import lombok.ast.TypeBody;
import lombok.ast.TypeReference;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.ecj.EcjTreeConverter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class EcjTreeConverterType1Test extends TreeBuilderRunner<Node> {
	public EcjTreeConverterType1Test() {
		super(false);
	}
	
	@Test
	public boolean testEcjTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	protected String convertToString(Source source, Node tree) {
		deleteEmptyDeclarations(tree);
		foldStringConcats(tree);
		splitVariableDefinitionEntries(tree);
		simplifyArrayDecls(tree);
		deleteComments(tree);
		StructureFormatter formatter = StructureFormatter.formatterWithoutPositions();
		formatter.skipProperty(CharLiteral.class, "value");
		formatter.skipProperty(StringLiteral.class, "value");
		tree.accept(new SourcePrinter(formatter));
		return formatter.finish();
	}
	
	private static void deleteComments(Node tree) {
		tree.accept(new ForwardingAstVisitor() {
			@Override public boolean visitComment(Comment node) {
				node.unparent();
				return false;
			}
		});
	}
	
	private void splitVariableDefinitionEntries(Node node) {
		node.accept(new ForwardingAstVisitor() {
			
			@Override public boolean visitVariableDefinition(VariableDefinition node) {
				
				if (node.astVariables().size() == 1) {
					return true;
				}
				Node parent = node.getParent();
				if (!(parent instanceof VariableDeclaration || parent instanceof For)) {
					return true;
				}
				
				if (parent instanceof VariableDeclaration) {
					splitVariableDeclaration((VariableDeclaration)parent);
				}
				
				if (parent instanceof For) {
					splitFor((For)parent, node);
				}
				return true;
			}
			
			private void splitVariableDeclaration(VariableDeclaration varDecl) {
				VariableDefinition varDef = varDecl.astDefinition();
				Node upFromDecl = varDecl.getParent();
				if (!(upFromDecl instanceof Block || upFromDecl instanceof TypeBody)) {
					return;
				}
				
				for (VariableDefinitionEntry varDefEntry : varDef.astVariables()) {
					if (upFromDecl instanceof Block) {
						VariableDeclaration splitDecl = new VariableDeclaration().astDefinition(splitAndUnparentVariableDeclaration(varDef, varDefEntry));
						((Block)upFromDecl).astContents().addBefore(varDecl, splitDecl);
					}
					else if (upFromDecl instanceof TypeBody) {
						VariableDeclaration splitDecl = new VariableDeclaration().astDefinition(splitAndUnparentVariableDeclaration(varDef, varDefEntry));
						((TypeBody)upFromDecl).astMembers().addBefore(varDecl, splitDecl);
					}
				}
				varDecl.unparent();
			}
			
			private void splitFor(For forStat, VariableDefinition varDef) {
				for (VariableDefinitionEntry varDefEntry : varDef.astVariables()) {
					VariableDefinition splitVarDef = splitAndUnparentVariableDeclaration(varDef, varDefEntry);
					
					/* 
					 * TODO: The way the converter adds multiple varDefs in a
					 * for is mimicked, though it does not seem to be a correct
					 * AST. Verify this and rewrite both the converter and
					 * the normalizer
					 */
					forStat.rawExpressionInits().addToEnd(splitVarDef);
				}
				forStat.astVariableDeclaration().unparent();
			}
			
			private VariableDefinition splitAndUnparentVariableDeclaration(VariableDefinition def, VariableDefinitionEntry varDefEntry) {
				varDefEntry.unparent();
				VariableDefinition copy = def.copy();
				copy.astVariables().clear();
				copy.astVariables().addToEnd(varDefEntry);
				return copy;
			}
		});
	}
	
	/*
	 * Dependent on splitVariableDefinitionEntries().
	 */
	private void simplifyArrayDecls(Node node) {
		node.accept(new ForwardingAstVisitor() {
			@Override public boolean visitVariableDefinition(VariableDefinition node) {
				VariableDefinitionEntry varDefEntry = node.astVariables().first();
				int arrayDimensions = varDefEntry.astArrayDimensions();
				if (arrayDimensions == 0) {
					return true;
				}
				varDefEntry.astArrayDimensions(0);
				TypeReference typeRef = node.astTypeReference();
				typeRef.astArrayDimensions(typeRef.astArrayDimensions() + arrayDimensions);
				return true;
			}
		});
	}
	
	private static void deleteEmptyDeclarations(Node node) {
		node.accept(new ForwardingAstVisitor() {
			@Override public boolean visitEmptyDeclaration(EmptyDeclaration node) {
				node.unparent();
				return true;
			}
		});
	}
	
	private static void foldStringConcats(Node tree) {
		tree.accept(new ForwardingAstVisitor() {
			@Override public boolean visitBinaryExpression(BinaryExpression node) {
				if (node.rawLeft() != null) node.rawLeft().accept(this);
				if (node.rawRight() != null) node.rawRight().accept(this);
				String left = null, right = null;
				if (node.astOperator() != BinaryOperator.PLUS || node.getParent() == null) return false;
				boolean leftIsChar = false;
				if (node.rawLeft() instanceof StringLiteral) left = ((StringLiteral) node.rawLeft()).astValue();
				if (node.rawLeft() instanceof CharLiteral) {
					left = "" + ((CharLiteral) node.rawLeft()).astValue();
					leftIsChar = true;
				}
				if (node.rawRight() instanceof StringLiteral) right = ((StringLiteral) node.rawRight()).astValue();
				if (!leftIsChar && node.rawRight() instanceof CharLiteral) right = "" + ((CharLiteral) node.rawRight()).astValue();
				if (left == null || right == null) return false;
				
				int start = node.rawLeft().getPosition().getStart();
				int end = node.rawRight().getPosition().getEnd();
				
				node.getParent().replaceChild(node, new StringLiteral().astValue(left + right).setPosition(new Position(start, end)));
				
				return false;
			}
		});
	}
	
	protected Node parseWithLombok(Source source) {
		List<Node> nodes = source.getNodes();
		assertEquals(1, nodes.size());
		
		return nodes.get(0);
	}
	
	protected Node parseWithTargetCompiler(Source source) {
		CompilerOptions compilerOptions = ecjCompilerOptions();
		Parser parser = new Parser(new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				compilerOptions,
				new DefaultProblemFactory()
			), compilerOptions.parseLiteralExpressionsAsConstants);
		parser.javadocParser.checkDocComment = true;
		CompilationUnit sourceUnit = new CompilationUnit(source.getRawInput().toCharArray(), source.getName(), "UTF-8");
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
		CompilationUnitDeclaration cud = parser.parse(sourceUnit, compilationResult);
		
		if (cud.hasErrors()) return null;
		
		EcjTreeConverter converter = new EcjTreeConverter();
		converter.visit(cud);
		return converter.get();
	}
}
