package lombok.ast.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.tools.SimpleJavaFileObject;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.ast.Node;
import lombok.ast.Version;
import lombok.ast.ecj.EcjTreeBuilder;
import lombok.ast.ecj.EcjTreeConverter;
import lombok.ast.ecj.EcjTreePrinter;
import lombok.ast.grammar.ParseProblem;
import lombok.ast.grammar.Source;
import lombok.ast.javac.JcTreeBuilder;
import lombok.ast.javac.JcTreeConverter;
import lombok.ast.javac.JcTreePrinter;
import lombok.ast.printer.HtmlFormatter;
import lombok.ast.printer.SourceFormatter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;
import lombok.ast.printer.TextFormatter;

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
import org.parboiled.google.collect.Lists;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import com.zwitserloot.cmdreader.CmdReader;
import com.zwitserloot.cmdreader.Description;
import com.zwitserloot.cmdreader.InvalidCommandLineException;
import com.zwitserloot.cmdreader.Mandatory;
import com.zwitserloot.cmdreader.Sequential;
import com.zwitserloot.cmdreader.Shorthand;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Main {
	private static class CmdArgs {
		@Shorthand("v")
		@Description("Print the name of each file as it is being converted.")
		private boolean verbose;
		
		@Description("Show version number and exit.")
		private boolean version;
		
		@Shorthand("h")
		@Description("Show this help text and exit.")
		private boolean help;
		
		@Shorthand("e")
		@Description("Sets the encoding of your source files. Defaults to the system default charset. Example: \"UTF-8\"")
		private String encoding;
		
		@Shorthand("p")
		@Description("Print converted code to standard output instead of saving it in target directory")
		private boolean print;
		
		@Shorthand("d")
		@Description("Directory to save converted files to")
		@Mandatory(onlyIfNot={"print", "help"})
		private String target;
		
		@Mandatory
		@Sequential
		@Description("Operations to apply to each source file. Comma-separated (no spaces). Valid options: ecj/javac/lombok first to decide how the file is parsed initially, " +
				"then any number of further ecj/javac/lombok keywords to convert ASTs, and finally text/source/html.")
		private String program;
		
		@Description("Files to convert. Provide either a file, or a directory. If you use a directory, all files in it (recursive) are converted")
		@Mandatory
		@Sequential
		private List<String> input = new ArrayList<String>();
	}
	
	public static void main(String[] rawArgs) throws Exception {
		CmdArgs args;
		CmdReader<CmdArgs> reader = CmdReader.of(CmdArgs.class);
		
		try {
			args = reader.make(rawArgs);
		} catch (InvalidCommandLineException e) {
			System.err.println(e.getMessage());
			System.err.println(reader.generateCommandLineHelp("java -jar lombok.ast.jar"));
			System.exit(1);
			return;
		}
		
		if (args.help) {
			System.out.println("lombok.ast java AST tool " + Version.getVersion());
			System.out.println(reader.generateCommandLineHelp("java -jar lombok.ast.jar"));
			System.exit(0);
			return;
		}
		
		if (args.version) {
			System.out.println(Version.getVersion());
			System.exit(0);
			return;
		}
		
		try {
			Charset charset = args.encoding == null ? Charset.defaultCharset() : Charset.forName(args.encoding);
			Main main = new Main(charset, args.verbose);
			main.compile(args.program);
			if (!args.print) {
				File targetDir = new File(args.target);
				if (!targetDir.exists()) targetDir.mkdirs();
				if (!targetDir.isDirectory()) {
					System.err.printf("%s is not a directory or cannot be created\n", targetDir.getCanonicalPath());
					System.exit(1);
					return;
				}
				main.setOutputDir(targetDir);
			}
			
			for (String input : args.input) {
				main.addToQueue(input);
			}
			
			main.go();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}
	}
	
	private void go() throws IOException {
		for (Plan p : files) {
			process(p.getFile(), outDir == null ? null : new File(outDir, p.getRelativeName()));
		}
		if (errors > 0) {
			System.err.printf("%d errors\n", errors);
		}
		System.exit(errors > 0 ? 2 : 0);
	}
	
	private void setOutputDir(File f) {
		this.outDir = f;
	}
	
	private void addToQueue(String item) throws IOException {
		addToQueue0(new File(item), "");
	}
	
	private void addToQueue0(File f, String pathSoFar) throws IOException {
		pathSoFar += (pathSoFar.isEmpty() ? "" : "/") + f.getName();
		if (f.isFile()) {
			if (f.getName().endsWith(".java")) {
				files.add(new Plan(f, pathSoFar));
			}
		} else if (f.isDirectory()) {
			for (File inner : f.listFiles()) {
				addToQueue0(inner, pathSoFar);
			}
		} else {
			throw new IllegalArgumentException("Unknown file: " + f.getCanonicalPath());
		}
	}
	
	@Data
	private static class Plan {
		final File file;
		final String relativeName;
	}
	
	private void process(File in, File out) throws IOException {
		if (verbose) {
			System.out.printf("Processing: %s to %s\n", in.getCanonicalPath(), out == null ? "sysout" : out.getCanonicalPath());
		}
		
		Source source = new Source(Files.toString(in, charset), in.getCanonicalPath());
		Object transfer = null;
		try {
			for (Operation<Object, Object> programElem : program) {
				transfer = programElem.process(source, transfer);
			}
			
			if (out == null) {
				System.out.println(transfer);
			} else {
				out.getParentFile().mkdirs();
				Files.write(transfer.toString(), out, charset);
			}
		} catch (ConversionProblem cp) {
			System.err.printf("Can't convert: %s due to %s\n", in.getCanonicalPath(), cp.getMessage());
			errors++;
		} catch (RuntimeException e) {
			System.err.printf("Error during convert: %s\n%s\n", in.getCanonicalPath(), printEx(e));
			errors++;
		}
	}
	
	private static String printEx(Throwable t) {
		val sb = new StringBuilder();
		sb.append(t.toString());
		sb.append("\n");
		Joiner.on("\n").appendTo(sb, t.getStackTrace());
		return sb.toString();
	}
	
	private void compile(String program) {
		this.program = compile0(program);
	}
	
	@SuppressWarnings("unchecked")
	private List<Operation<Object, Object>> compile0(String program) {
		String[] parts = program.split("\\s*,\\s*");
		List<Operation<Object, Object>> out = Lists.newArrayList();
		if (parts.length == 0) throw new IllegalArgumentException("No operations");
		Operation<?, ?> initialOp = CONVERSIONS.get("_," + parts[0]);
		if (initialOp == null) {
			List<String> initialOps = Lists.newArrayList();
			for (String key : CONVERSIONS.keySet()) {
				if (key.startsWith("_,")) initialOps.add(key.substring(2));
			}
			throw new IllegalArgumentException(String.format(
					"Illegal initial operation: %s\nLegal initial operations: %s",
					parts[0], Joiner.on(",").join(initialOps)));
		}
		
		out.add((Operation<Object, Object>) initialOp);
		for (int i = 0; i < parts.length - 1; i++) {
			String convKey = String.format("%s,%s", parts[i], parts[i + 1]);
			Operation<?, ?> convOp = CONVERSIONS.get(convKey);
			if (convOp == null) {
				List<String> convOps = Lists.newArrayList();
				for (String key : CONVERSIONS.keySet()) {
					if (key.startsWith(parts[i] + ",")) convOps.add(key.substring(parts[i].length() + 1));
				}
				throw new IllegalArgumentException(String.format(
						"Illegal conversion operation: %s\nLegal conversion operations from %s: %s",
						convKey, parts[i], Joiner.on(",").join(convOps)));
			}
			out.add((Operation<Object, Object>) convOp);
		}
		
		String lastPart = parts[parts.length - 1];
		if (!LEGAL_FINAL.contains(lastPart)) {
			throw new IllegalArgumentException(String.format(
					"Illegal final operation: %s\nLegal final operations: %s",
					lastPart, Joiner.on(",").join(LEGAL_FINAL)));
		}
		
		return out;
	}
	
	private final Charset charset;
	private List<Operation<Object, Object>> program;
	private final boolean verbose;
	private int errors;
	private File outDir = null;
	private final List<Plan> files = Lists.newArrayList();
	
	private interface Operation<A, B> {
		B process(Source source, A in) throws ConversionProblem;
	}
	
	private static class ConversionProblem extends Exception {
		ConversionProblem(String message) {
			super(message);
		}
	}
	
	protected CompilerOptions ecjCompilerOptions() {
		CompilerOptions options = new CompilerOptions();
		options.complianceLevel = ClassFileConstants.JDK1_6;
		options.sourceLevel = ClassFileConstants.JDK1_6;
		options.targetJDK = ClassFileConstants.JDK1_6;
		options.parseLiteralExpressionsAsConstants = true;
		return options;
	}
	
	private final Operation<Void, Node> parseWithLombok = new Operation<Void, Node>() {
		@Override public Node process(Source in, Void irrelevant) throws ConversionProblem {
			List<Node> nodes = in.getNodes();
			List<ParseProblem> problems = in.getProblems();
			if (problems.size() > 0) throw new ConversionProblem(String.format("Can't read file %s due to parse error: %s", in.getName(), problems.get(0)));
			if (nodes.size() == 1) return nodes.get(1);
			if (nodes.size() == 0) throw new ConversionProblem("No nodes parsed by lombok.ast");
			throw new ConversionProblem("More than 1 node parsed by lombok.ast");
		}
	};
	
	private final Operation<Void, ASTNode> parseWithEcj = new Operation<Void, ASTNode>() {
		@Override public ASTNode process(Source in, Void irrelevant) throws ConversionProblem {
			CompilerOptions compilerOptions = ecjCompilerOptions();
			Parser parser = new Parser(new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					compilerOptions,
					new DefaultProblemFactory()
				), compilerOptions.parseLiteralExpressionsAsConstants);
			parser.javadocParser.checkDocComment = true;
			CompilationUnit sourceUnit = new CompilationUnit(in.getRawInput().toCharArray(), in.getName(), charset.name());
			CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
			CompilationUnitDeclaration cud = parser.parse(sourceUnit, compilationResult);
			
			if (cud.hasErrors()) {
				throw new ConversionProblem(String.format("Can't read file %s due to parse error: %s", in.getName(), compilationResult.getErrors()[0]));
			}
			
			return cud;
		}
	};
	
	private final Operation<Void, JCCompilationUnit> parseWithJavac = new Operation<Void, JCCompilationUnit>() {
		@Override public JCCompilationUnit process(Source in, Void irrelevant) throws ConversionProblem {
			Context context = new Context();
			
			Options.instance(context).put(OptionName.ENCODING, charset.name());
			
			JavaCompiler compiler = new JavaCompiler(context);
			compiler.genEndPos = true;
			compiler.keepComments = true;
			
			JCCompilationUnit cu = compiler.parse(new ContentBasedJavaFileObject(in.getName(), in.getRawInput()));
			
			return cu;
		}
	};
	
	private final Operation<JCCompilationUnit, Node> javacToLombok = new Operation<JCCompilationUnit, Node>() {
		@Override public Node process(Source source, JCCompilationUnit in) throws ConversionProblem {
			JcTreeConverter converter = new JcTreeConverter();
			converter.visit(in);
			return converter.getResult();
		}
	};
	
	private final Operation<CompilationUnitDeclaration, Node> ecjToLombok = new Operation<CompilationUnitDeclaration, Node>() {
		@Override public Node process(Source source, CompilationUnitDeclaration in) throws ConversionProblem {
			EcjTreeConverter converter = new EcjTreeConverter();
			converter.visit(source.getRawInput(), in);
			return converter.get();
		}
	};
	
	private final Operation<Node, JCCompilationUnit> lombokToJavac = new Operation<Node, JCCompilationUnit>() {
		@Override public JCCompilationUnit process(Source source, Node in) throws ConversionProblem {
			JcTreeBuilder builder = new JcTreeBuilder();
			builder.visit(in);
			JCTree out = builder.get();
			if (out instanceof JCCompilationUnit) return (JCCompilationUnit) out;
			throw new ConversionProblem("result from lombokToJavac is not JCCompilationUnit");
		}
	};
	
	private final Operation<Node, CompilationUnitDeclaration> lombokToEcj = new Operation<Node, CompilationUnitDeclaration>() {
		@Override public CompilationUnitDeclaration process(Source source, Node in) throws ConversionProblem {
			EcjTreeBuilder builder = new EcjTreeBuilder(source.getRawInput(), source.getName(), ecjCompilerOptions());
			builder.visit(in);
			ASTNode out = builder.get();
			if (out instanceof CompilationUnitDeclaration) return (CompilationUnitDeclaration) out;
			throw new ConversionProblem("result from lombokToEcj is not CompilationUnitDeclaration");
		}
	};
	
	private final Operation<Node, String> lombokToHtml = new Operation<Node, String>() {
		@Override public String process(Source source, Node in) throws ConversionProblem {
			SourceFormatter formatter = new HtmlFormatter(source.getRawInput());
			in.accept(new SourcePrinter(formatter));
			
			for (ParseProblem x : source.getProblems()) {
				formatter.addError(x.getPosition().getStart(), x.getPosition().getEnd(), x.getMessage());
			}
			
			return formatter.finish();
		}
	};
	
	private final Operation<Node, String> lombokToSource = new Operation<Node, String>() {
		@Override public String process(Source source, Node in) throws ConversionProblem {
			SourceFormatter formatter = new TextFormatter();
			in.accept(new SourcePrinter(formatter));
			
			for (ParseProblem x : source.getProblems()) {
				formatter.addError(x.getPosition().getStart(), x.getPosition().getEnd(), x.getMessage());
			}
			
			return formatter.finish();
		}
	};
	
	private final Operation<Node, String> lombokToText = new Operation<Node, String>() {
		@Override public String process(Source source, Node in) throws ConversionProblem {
			SourceFormatter formatter = StructureFormatter.formatterWithPositions();
			in.accept(new SourcePrinter(formatter));
			
			for (ParseProblem x : source.getProblems()) {
				formatter.addError(x.getPosition().getStart(), x.getPosition().getEnd(), x.getMessage());
			}
			
			return formatter.finish();
		}
	};
	
	private final Operation<JCCompilationUnit, String> javacToText = new Operation<JCCompilationUnit, String>() {
		@Override public String process(Source source, JCCompilationUnit in) throws ConversionProblem {
			JcTreePrinter printer = new JcTreePrinter(true);
			printer.visit(in);
			return printer.toString();
		}
	};
	
	private final Operation<CompilationUnitDeclaration, String> ecjToText = new Operation<CompilationUnitDeclaration, String>() {
		@Override public String process(Source source, CompilationUnitDeclaration in) throws ConversionProblem {
			EcjTreePrinter printer = new EcjTreePrinter(true);
			printer.visit(in);
			return printer.toString();
		}
	};
	
	private final Map<String, Operation<?, ?>> CONVERSIONS = ImmutableMap.<String, Operation<?, ?>>builder()
			.put("_,ecj", parseWithEcj)
			.put("_,lombok", parseWithLombok)
			.put("_,javac", parseWithJavac)
			.put("javac,lombok", javacToLombok)
			.put("lombok,javac", lombokToJavac)
			.put("ecj,lombok", ecjToLombok)
			.put("lombok,ecj", lombokToEcj)
			.put("lombok,text", lombokToText)
			.put("lombok,source", lombokToSource)
			.put("lombok,html", lombokToHtml)
			.put("ecj,text", ecjToText)
			.put("javac,text", javacToText)
			.build();
	
	private final List<String> LEGAL_FINAL = ImmutableList.of("source", "html", "text");
	
	private static class ContentBasedJavaFileObject extends SimpleJavaFileObject {
		private final String content;
		
		public ContentBasedJavaFileObject(String name, String content) {
			super(new File(name).toURI(), Kind.SOURCE);
			this.content = content;
		}
		
		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return content;
		}
	}
}
