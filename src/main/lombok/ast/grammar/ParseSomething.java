package lombok.ast.grammar;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import lombok.ast.Node;

import org.parboiled.Parboiled;
import org.parboiled.common.Function;
import org.parboiled.common.StringUtils;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.parboiled.trees.Printability;

public class ParseSomething {
	public static void main(String[] args) throws IOException {
		String src = null;
		if (args.length > 0) {
			src = readFile(args);
		}
		
		JavaParser p = Parboiled.createParser(JavaParser.class);
		
		if (src == null) {
			Scanner s = new Scanner(System.in);
			while (true) {
				String line = s.nextLine();
				if (line.isEmpty()) return;
				parse(p, line);
			}
		} else {
			parse(p, src);
		}
	}
	
	private static void parse(JavaParser parser, String input) {
		ParsingResult<Node> result = parser.parse(parser.testRules(), input);
		System.out.println(ParseTreeUtils.printNodeTree(result, new Function<org.parboiled.Node<Node>, Printability>() {
			@Override public Printability apply(org.parboiled.Node<Node> from) {
				return from.getValue() != null ? Printability.PrintAndDescend : Printability.Descend;
//				if (from.getValue())
////				if (from.getLabel() != null) {
////					if (from.getLabel().matches("^(\".*\")|ws|wsChar$")) return Printability.Print;
////				}
////				
//				return Printability.PrintAndDescend;
			}
		}));
		if (result.hasErrors()) {
			System.out.println(StringUtils.join(result.parseErrors, "---\n"));
		}
	}
	
	private static String readFile(String[] args) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(args[0]);
		ByteArrayOutputStream fos = new ByteArrayOutputStream();
		byte[] b = new byte[65536];
		while (true) {
			int r = fis.read(b);
			if (r == -1) break;
			fos.write(b, 0, r);
		}
		fos.write('\n');
		return new String(fos.toByteArray(), "UTF-8");
	}
}
