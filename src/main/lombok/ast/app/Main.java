package lombok.ast.app;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import com.google.common.base.Charsets;
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

import lombok.ast.Node;
import lombok.ast.Version;
import lombok.ast.ecj.EcjTreeBuilder;
import lombok.ast.ecj.EcjTreeConverter;
import lombok.ast.ecj.EcjTreePrinter;
import lombok.ast.grammar.ContentBasedJavaFileObject;
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
	}
	
	private final String encoding;
	
	private Main(String encoding) {
		this.encoding = encoding;
	}
	
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
			CompilationUnit sourceUnit = new CompilationUnit(in.getRawInput().toCharArray(), in.getName(), encoding);
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
			
			Options.instance(context).put(OptionName.ENCODING, encoding);
			
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
}
