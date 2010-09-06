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

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.CharLiteral;
import lombok.ast.EmptyDeclaration;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.StringLiteral;
import lombok.ast.ecj.EcjTreeConverter;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;
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
public class EcjToLombokTest extends TreeBuilderRunner<Node> {
	public EcjToLombokTest() {
		super(false);
	}
	
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		return Arrays.asList (
		DirDescriptor.of(new File("test/resources/idempotency"), true).withInclusion(Pattern.compile("^.*(?:[a-d]\\d{3}_).*\\.java$", Pattern.CASE_INSENSITIVE)));
//		DirDescriptor.of(new File("test/resources/idempotency"), true).withInclusion(Pattern.compile("^.*(?:[D]002_).*\\.java$", Pattern.CASE_INSENSITIVE)));
//		return Arrays.asList(
//				DirDescriptor.of(new File("test/resources/idempotency"), true),
//				DirDescriptor.of(new File("test/resources/alias"), true),
//				DirDescriptor.of(new File("test/resources/special"), true));
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
		StructureFormatter formatter = StructureFormatter.formatterWithoutPositions();
		formatter.skipProperty(CharLiteral.class, "value");
		formatter.skipProperty(StringLiteral.class, "value");
		tree.accept(new SourcePrinter(formatter));
		return formatter.finish();
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
		
		return EcjTreeConverter.convert(cud);
	}
}
