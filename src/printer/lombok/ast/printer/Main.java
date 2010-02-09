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
package lombok.ast.printer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import lombok.ast.Node;
import lombok.ast.grammar.ParseProblem;
import lombok.ast.grammar.Source;

import org.apache.commons.io.FileUtils;

public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Provide a java source file to parse.");
			System.exit(0);
		}
		
		String rawSource = FileUtils.readFileToString(new File(args[0]), "UTF-8");
		Source source = new Source(rawSource);
		source.parseCompilationUnit();
		long now = System.currentTimeMillis();
		source = new Source(rawSource);
		source.parseCompilationUnit();
		Node result = source.getNodes().get(0);
		long taken = System.currentTimeMillis() - now;
		SourceFormatter formatter = new HtmlFormatter(rawSource + "\n");
		formatter.setTimeTaken(taken);
		result.accept(new SourcePrinter(formatter));
		for (ParseProblem x : source.getProblems()) {
			formatter.addError(x.getPosition().getStart(), x.getPosition().getEnd(), x.getMessage());
		}
		
		File outFile = new File(args[0] + ".html");
		FileUtils.writeStringToFile(outFile, formatter.finish(), "UTF-8");
		Desktop.getDesktop().browse(outFile.toURI());
		System.exit(0);
	}
}
