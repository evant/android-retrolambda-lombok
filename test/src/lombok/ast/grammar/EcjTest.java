package lombok.ast.grammar;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.ast.ecj.ASTNodePrinter;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DirectoryRunner.class)
public class EcjTest {
	public static File getDirectory() {
		return new File("test/idempotency");
	}
	
	@Test
	public boolean testJavaCompiler(Source source) throws IOException {
		if (false && source.getName().compareToIgnoreCase("B") > 0) {
			return false;
		}
		
		Map options = new HashMap();
		options.put(CompilerOptions.OPTION_Source, "1.6");
		Parser parser = new Parser(new ProblemReporter(
				DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
				new CompilerOptions(options), 
				new DefaultProblemFactory()
			), true);
		
		CompilationUnit sourceUnit = new CompilationUnit(source.getRawInput().toCharArray(), source.getName(), "UTF-8");
		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
		CompilationUnitDeclaration cud = parser.parse(sourceUnit, compilationResult);

		if (cud.hasErrors()) {
			CategorizedProblem[] problems = compilationResult.getProblems();
			for (CategorizedProblem problem : problems) {
				System.out.println(problem.getMessage());
			}
		}
		assertFalse(cud.hasErrors());
		
		
		ASTNodePrinter visitor = new ASTNodePrinter();
		try {
			cud.traverse(visitor, (CompilationUnitScope)null);
		}
		catch (RuntimeException e) {
			printDebugInformation(source, null, null);
			throw e;
		}
		
//		printDebugInformation(source, visitor.toString(), null);
		assertFalse(visitor.toString().isEmpty());
		return true;
	}
	

	private void printDebugInformation(Source source, String javacString, String lombokString) {
		String name = source.getName();
		System.out.printf("==== Processing %s ====\n", name);
		System.out.println(source.getRawInput());
		if (javacString != null) {
			System.out.println("=========== Expected ============");
			System.out.println(javacString);
		}
		if (lombokString != null) {
			System.out.println("============ Actual =============");
			System.out.println(lombokString);
		}
		System.out.printf("======= End of %s =======\n", name);
	}	
}
