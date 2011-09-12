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

import lombok.ast.Node;
import lombok.ast.ecj.EcjTreeBuilder;
import lombok.ast.ecj.EcjTreeConverter;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Compares an ASTNode parsed by ecj with a ASTNode parsed by ecj, converted to lombok.ast, and converted back.
 */
@RunWith(RunForEachFileInDirRunner.class)
public class EcjTreeConverterType2Test extends TreeBuilderRunner<ASTNode> {
	public EcjTreeConverterType2Test() {
		super(true);
	}
	
	@Test
	public boolean testEcjTreeConverter(Source source) throws Exception {
		return testCompiler(source);
	}
	
	@Override
	protected String convertToString(ASTNode tree) {
		return EcjTreeOperations.convertToString(tree);
	}
	
	@Override
	protected boolean checkForLombokAstParseFailure() {
		return false;
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	@Override
	protected ASTNode parseWithLombok(Source source) {
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
		converter.visit(source.getRawInput(), cud);
		Node lombokized = converter.get();
		
		EcjTreeBuilder builder = new EcjTreeBuilder(source.getRawInput(), source.getName(), ecjCompilerOptions());
		builder.visit(lombokized);
		return builder.get();
	}
	
	@Override
	protected ASTNode parseWithTargetCompiler(Source source) {
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
		
		return cud;
	}
}
