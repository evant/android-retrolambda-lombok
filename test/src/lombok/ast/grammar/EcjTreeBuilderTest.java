package lombok.ast.grammar;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ast.Node;
import lombok.ast.ecj.EcjTreePrinter;
import lombok.ast.ecj.EcjTreeBuilder;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
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
		if (source.getName().compareToIgnoreCase("B") > 0) {
			return false;
		}
		
		return testCompiler(source);
	}
	
	@SuppressWarnings("unchecked")
	private static Map ecjCompilerOptions() {
		Map options = new HashMap();
		options.put(CompilerOptions.OPTION_Source, "1.6");
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
		
		EcjTreeBuilder builder = new EcjTreeBuilder();
		nodes.get(0).accept(builder);
		return builder.get();
	}
	
	protected ASTNode parseWithTargetCompiler(Source source) {
		@SuppressWarnings("unchecked")
		Map options = ecjCompilerOptions();
		
		Parser parser = new Parser(new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
				new CompilerOptions(options), 
				new DefaultProblemFactory()
			), true);
		
		CompilationUnit sourceUnit = new CompilationUnit(source.getRawInput().toCharArray(), source.getName(), "UTF-8");
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
		CompilationUnitDeclaration cud = parser.parse(sourceUnit, compilationResult);
		
		if (cud.hasErrors()) return null;
		return cud;
	}
}
