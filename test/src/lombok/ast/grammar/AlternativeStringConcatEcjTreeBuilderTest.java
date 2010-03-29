package lombok.ast.grammar;

import static java.util.Collections.singleton;

import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;

import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RunForEachFileInDirRunner.class)
public class AlternativeStringConcatEcjTreeBuilderTest extends EcjTreeBuilderTest {
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		return singleton(DirDescriptor.of(new File("test/resources/idempotency"), true).withInclusion(Pattern.compile("^.*StringConcatExpressions.java$", Pattern.CASE_INSENSITIVE)));
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
