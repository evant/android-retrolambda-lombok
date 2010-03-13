package lombok.ast.grammar;

import java.io.File;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DirectoryRunner.class)
public class AlternativeStringConcatEcjTreeBuilderTest extends EcjTreeBuilderTest {
	protected boolean shouldProcess(File file) {
		return file.getName().endsWith("StringConcatExpressions.java");
	}
	
	@Test
	public boolean testAlternativeStringConcatEcjCompiler(Source source) throws Exception {
		return testCompiler(source);
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = super.ecjCompilerOptions();
		options.parseLiteralExpressionsAsConstants = false;
		return options;
	}
}
