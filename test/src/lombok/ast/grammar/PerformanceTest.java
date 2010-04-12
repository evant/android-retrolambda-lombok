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

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import lombok.Cleanup;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parboiled.Parboiled;
import org.parboiled.ReportingParseRunner;

import com.google.common.collect.Lists;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;

@RunWith(RunForEachFileInDirRunner.class)
public class PerformanceTest extends RunForEachFileInDirRunner.SourceFileBasedTester {
	private static final int REPS = 50;
	private static final boolean VERBOSE = System.getProperty("lombok.ast.test.verbose") != null;
	private static final boolean EXTENDED = System.getProperty("lombok.ast.test.extended") != null;
	private static final double MAX_FACTOR = 15;
	private static long javacTotal, lombokTotal, ecjTotal, parboiledTotal;
	
	@BeforeClass
	public void init() {
		if (VERBOSE) {
			System.out.printf("[%20s / %30s] Per entry: time in millis for %d reps [lombok takes X longer than ~ : ~ takes X longer than javac]\n",
					"path", "file", REPS);
		}
	}
	
	@AfterClass
	public void summary() {
		if (VERBOSE) {
			System.out.printf("[%20s / %30s] l.ast: %5d [  1.00 : %6.02f] jc: %5d [%6.02f :   1.00] ecj: %5d [%6.02f : %6.02f] pb: %5d [%6.02f : %6.02f]\n",
					"", "*** TOTALS ***",
					lombokTotal, (double)lombokTotal / javacTotal,
					javacTotal, (double)lombokTotal / javacTotal,
					ecjTotal, (double)lombokTotal / ecjTotal, (double)ecjTotal / javacTotal,
					parboiledTotal, (double)lombokTotal / parboiledTotal, (double)parboiledTotal / javacTotal);
		}
	}
	
	@Override protected Collection<DirDescriptor> getDirDescriptors() {
		List<DirDescriptor> descriptors = Lists.newArrayList();
		descriptors.add(DirDescriptor.of(new File("test/resources/idempotency"), true));
		descriptors.add(DirDescriptor.of(new File("test/resources/alias"), true));
		descriptors.add(DirDescriptor.of(new File("test/resources/special"), true));
		if (VERBOSE) {
			descriptors.add(DirDescriptor.of(new File("test/resources/performance"), true));
		}
		return descriptors;
	}
	
	@Test
	public boolean testPerformance(Source source) {
		if (!EXTENDED) return false;
		parseWithJavac(source);
		long takenByJavac = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithJavac(source);
		}
		takenByJavac = System.currentTimeMillis() - takenByJavac;
		javacTotal += takenByJavac;
		
		parseWithEcj(source);
		long takenByEcj = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithEcj(source);
		}
		takenByEcj = System.currentTimeMillis() - takenByEcj;
		ecjTotal += takenByEcj;
		
		source.parseCompilationUnit();
		long takenByLombok = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			source.clear();
			source.parseCompilationUnit();
		}
		takenByLombok = System.currentTimeMillis() - takenByLombok;
		lombokTotal += takenByLombok;
		
		parseWithParboiled(source);
		long takenByParboiled = System.currentTimeMillis();
		for (int i = 0; i < REPS; i++) {
			parseWithParboiled(source);
		}
		takenByParboiled = System.currentTimeMillis() - takenByParboiled;
		parboiledTotal += takenByParboiled;
		
		String fn = source.getName();
		String fnPrefix, fnSuffix, fileName; {
			int sep = fn.lastIndexOf('/');
			if (sep == -1) {
				fnPrefix = "";
				fileName = fnSuffix = fn;
			} else {
				fnPrefix = fn.substring(0, sep);
				fileName = fnSuffix = fn.substring(sep + 1);
			}
			if (fnSuffix.endsWith(".java")) fnSuffix = fnSuffix.substring(0, fnSuffix.length() - ".java".length());
			if (fnPrefix.length() > 20) fnPrefix = "\u2026" + fnPrefix.substring(fnPrefix.length() - 19);
			if (fnSuffix.length() > 30) fnSuffix = fnSuffix.substring(0, 10) + "\u2026" + fnSuffix.substring(fnSuffix.length() - 19);
		}
		
		if (VERBOSE) {
			System.out.printf("[%20s / %30s] l.ast: %5d [  1.00 : %6.02f] jc: %5d [%6.02f :   1.00] ecj: %5d [%6.02f : %6.02f] pb: %5d [%6.02f : %6.02f]\n",
					fnPrefix, fnSuffix,
					takenByLombok, (double)takenByLombok / takenByJavac,
					takenByJavac, (double)takenByLombok / takenByJavac,
					takenByEcj, (double)takenByLombok / takenByEcj, (double)takenByEcj / takenByJavac,
					takenByParboiled, (double)takenByLombok / takenByParboiled, (double)takenByParboiled / takenByJavac);
		}
		
		double factorVsJavac = (double)takenByLombok / takenByJavac;
		if (factorVsJavac > MAX_FACTOR) {
			if (VERBOSE) {
				try {
					File reportFile = new File("test/reports/" + fileName + ".report");
					reportFile.getParentFile().mkdirs();
					@Cleanup FileOutputStream rawOut = new FileOutputStream(reportFile);
					Writer out = new BufferedWriter(new OutputStreamWriter(rawOut, "UTF-8"));
					out.write(String.format("Parse Profile for: %s which is slower than javac by a factor of %.02f\n", source.getName(), factorVsJavac));
					for (String report : source.getDetailedProfileInformation(25)) {
						out.write(report);
						out.write("===================================");
						out.write("\n");
					}
					out.close();
					rawOut.close();
					System.out.println("Profile report written to: " + reportFile.getCanonicalPath());
				} catch (IOException e) {
					System.err.println("I/O error writing profile report on " + source.getName() + "; Possibly ./test/reports is not writable?");
					e.printStackTrace();
				}
			}
			fail(String.format("Performance is slower than javac by factor %d on %s", (int)factorVsJavac, source.getName()));
		}
		
		return true;
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
