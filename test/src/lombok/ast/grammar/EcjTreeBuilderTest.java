package lombok.ast.grammar;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import lombok.ast.Node;
import lombok.ast.ecj.EcjTreePrinter;
import lombok.ast.ecj.EcjTreeBuilder;

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

@RunWith(DirectoryRunner.class)
public class EcjTreeBuilderTest extends TreeBuilderTest<ASTNode> {
	public static File getDirectory() {
		return new File("test/idempotency");
	}
	
	@Test
	public boolean testEcjCompiler(Source source) throws Exception {
		if (source.getName().compareToIgnoreCase("D") > 0) {
			return false;
		}
		
		return testCompiler(source);
	}
	
	private static CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		//TODO turn this off. String concats should continue to work.
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	protected String convertToString(ASTNode tree) {
		EcjTreePrinter printer = new EcjTreePrinter();
		printer.visitEcjNode(tree);
		String string = printer.toString();
		return string;
	}
	
	protected ASTNode parseWithLombok(Source source) {
		List<Node> nodes = source.getNodes();
		assertEquals(1, nodes.size());
		
		EcjTreeBuilder builder = new EcjTreeBuilder(source, ecjCompilerOptions());
		nodes.get(0).accept(builder);
		return builder.get();
	}
	
	protected ASTNode parseWithTargetCompiler(Source source) {
		CompilerOptions compilerOptions = ecjCompilerOptions();
		Parser parser = new Parser(new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				compilerOptions,
				new DefaultProblemFactory()
			), compilerOptions.parseLiteralExpressionsAsConstants);
		
		CompilationUnit sourceUnit = new CompilationUnit(source.getRawInput().toCharArray(), source.getName(), "UTF-8");
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
		CompilationUnitDeclaration cud = parser.parse(sourceUnit, compilationResult);
		
		if (cud.hasErrors()) return null;
		return cud;
	}
}
