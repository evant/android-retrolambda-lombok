package lombok.ast.grammar;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.ast.grammar.JcTreeBuilderTest.TestJavaFileObject;
import lombok.ast.grammar.RunForEachFileInDirRunner.DirDescriptor;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parboiled.Parboiled;
import org.parboiled.ReportingParseRunner;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;

@RunWith(RunForEachFileInDirRunner.class)
public class PerformanceTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	private static final int REPS = 50;
	private static final boolean VERBOSE = System.getProperty("lombok.ast.test.verbose") != null;
	private static final double MAX_FACTOR = 10;
	
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		List<DirDescriptor> descriptors = new ArrayList<DirDescriptor>();
		descriptors.add(DirDescriptor.of(new File("test/resources/idempotency"), true));
		descriptors.add(DirDescriptor.of(new File("test/resources/alias"), true));
		descriptors.add(DirDescriptor.of(new File("test/resources/special"), true));
		if (VERBOSE) {
			descriptors.add(DirDescriptor.of(new File("test/resources/performance"), true));
		}
		return descriptors;
	}
	
	@Test
	public void testPerformance(Source source) {
		parseWithJavac(source);
		long takenByJavac = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithJavac(source);
		}
		takenByJavac = System.currentTimeMillis() - takenByJavac;
		
		parseWithEcj(source);
		long takenByEcj = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithEcj(source);
		}
		takenByEcj = System.currentTimeMillis() - takenByEcj;
		
		source.parseCompilationUnit();
		long takenByLombok = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			source.clear();
			source.parseCompilationUnit();
		}
		takenByLombok = System.currentTimeMillis() - takenByLombok;
		
		parseWithParboiled(source);
		long takenByParboiled = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithParboiled(source);
		}
		takenByParboiled = System.currentTimeMillis() - takenByParboiled;
		
		String fn = source.getName();
		String fnPrefix, fnSuffix; {
			int sep = fn.lastIndexOf('/');
			if (sep == -1) {
				fnPrefix = "";
				fnSuffix = fn;
			} else {
				fnPrefix = fn.substring(0, sep);
				fnSuffix = fn.substring(sep + 1);
			}
			if (fnSuffix.endsWith(".java")) fnSuffix = fnSuffix.substring(0, fnSuffix.length() - ".java".length());
			if (fnPrefix.length() > 20) fnPrefix = "\u2026" + fnPrefix.substring(fnPrefix.length() - 19);
			if (fnSuffix.length() > 30) fnSuffix = fnSuffix.substring(0, 10) + "\u2026" + fnSuffix.substring(fnSuffix.length() - 19);
		}
		
		if (VERBOSE) {
			printHeader();
			System.out.printf("[%20s / %30s] l.ast: %5d [  1.00 : %6.02f] jc: %5d [%6.02f :   1.00] ecj: %5d [%6.02f : %6.02f] pb: %5d [%6.02f : %6.02f]\n",
					fnPrefix, fnSuffix,
					takenByLombok, (double)takenByLombok / takenByJavac,
					takenByJavac, (double)takenByLombok / takenByJavac,
					takenByEcj, (double)takenByLombok / takenByEcj, (double)takenByEcj / takenByJavac,
					takenByParboiled, (double)takenByLombok / takenByParboiled, (double)takenByParboiled / takenByJavac);
		}
		
		double factorVsJavac = (double)takenByLombok / takenByJavac;
		if (factorVsJavac > MAX_FACTOR) {
			fail(String.format("Performance is slower than javac by factor %d on %s", (int)factorVsJavac, source.getName()));
		}
	}
	
	private static boolean headerPrinted = false;
	
	private void printHeader() {
		if (headerPrinted) return;
		headerPrinted = true;
		System.out.printf("[%20s / %30s] Per entry: time in millis for %d reps [lombok takes X longer than ~ : ~ takes X longer than javac]\n",
				"path", "file", REPS);
	}
	
	private void parseWithParboiled(Source source) {
		if (VERBOSE) {
			ParboiledJavaGrammar parser = Parboiled.createParser(ParboiledJavaGrammar.class);
			ReportingParseRunner.run(parser.compilationUnit(), source.getRawInput());
		}
	}
	
	private void parseWithJavac(Source source) {
		Context context = new Context();
		JavaCompiler compiler = new JavaCompiler(context);
		compiler.genEndPos = true;
		compiler.parse(new TestJavaFileObject(source.getName(), source.getRawInput()));
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	private void parseWithEcj(Source source) {
		if (VERBOSE) {
			CompilerOptions compilerOptions = ecjCompilerOptions();
			Parser parser = new Parser(new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()
				), compilerOptions.parseLiteralExpressionsAsConstants);
			parser.javadocParser.checkDocComment = true;
			CompilationUnit sourceUnit = new CompilationUnit(source.getRawInput().toCharArray(), source.getName(), "UTF-8");
			CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
			parser.parse(sourceUnit, compilationResult);
		}
	}
}
