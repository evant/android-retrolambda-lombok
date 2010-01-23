package lombok.ast.printer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import lombok.ast.Node;
import lombok.ast.grammar.ParserGroup;

import org.apache.commons.io.FileUtils;
import org.parboiled.support.ParseError;
import org.parboiled.support.ParsingResult;

public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Provide a java source file to parse.");
			System.exit(0);
		}
		
		String rawSource = FileUtils.readFileToString(new File(args[0]), "UTF-8");
		ParserGroup group = new ParserGroup();
		ParsingResult<Node> result = group.structures.parse(group.structures.compilationUnit(), rawSource + "\n");
		long now = System.currentTimeMillis();
		result = group.structures.parse(group.structures.compilationUnit(), rawSource + "\n");
		long taken = System.currentTimeMillis() - now;
		HtmlPrinter printer = new HtmlPrinter(rawSource + "\n");
		printer.setTimeTaken(taken);
		result.parseTreeRoot.getValue().accept(printer);
		for (ParseError x : result.parseErrors) {
			printer.addError(x.getErrorStart(), x.getErrorEnd(), x.getErrorMessage());
		}
		
		File outFile = new File(args[0] + ".html");
		FileUtils.writeStringToFile(outFile, printer.toHtml(), "UTF-8");
		Desktop.getDesktop().browse(outFile.toURI());
		System.exit(0);
	}
}
