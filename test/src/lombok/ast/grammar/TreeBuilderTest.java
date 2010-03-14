package lombok.ast.grammar;

import static org.junit.Assert.*;

import java.io.File;

abstract class TreeBuilderTest<N> extends DirectoryRunner.SourceFileBasedTester {
	protected File getDirectory() {
		return new File("test/idempotency");
	}
	
	protected boolean testCompiler(Source source) throws Exception {
		N parsedWithTargetCompiler = parseWithTargetCompiler(source);
		if (parsedWithTargetCompiler == null) {
			// Skip test if target compiler can't compile it.
			// A separate test that checks if test samples compile
			// at all will do error reporting.
			return false;
		}
		String targetString = convertToString(parsedWithTargetCompiler);
		
		source.parseCompilationUnit();
		if (!source.getProblems().isEmpty()) {
			StringBuilder message = new StringBuilder();
			for (ParseProblem p : source.getProblems()) {
				message.append(p.toString());
				message.append("\n");
			}
			printDebugInformation(source, targetString, null);
			fail(message.toString());
		}
		
		String lombokString;
		try {
			lombokString = convertToString(parseWithLombok(source));
		} catch (Exception e) {
			printDebugInformation(source, targetString, null);
			throw e;
		} catch (Error e) {
			printDebugInformation(source, targetString, null);
			throw e;
		}
		
		try {
			assertEquals(targetString, lombokString);
		} catch (AssertionError e) {
			printDebugInformation(source, targetString, lombokString);
			throw e;
		}
		
		return true;
	}
	
	protected void printDebugInformation(Source source, String ecjString, String lombokString) {
		String name = source.getName();
		System.out.printf("==== Processing %s ====\n", name);
		System.out.println(source.getRawInput());
		System.out.println("=========== Expected ============");
		System.out.println(ecjString);
		if (lombokString != null) {
			System.out.println("============ Actual =============");
			System.out.println(lombokString);
		}
		System.out.printf("======= End of %s =======\n", name);
	}
	
	protected abstract String convertToString(N tree) throws Exception;
	
	protected abstract N parseWithLombok(Source source) throws Exception;
	
	protected abstract N parseWithTargetCompiler(Source source) throws Exception;
}
