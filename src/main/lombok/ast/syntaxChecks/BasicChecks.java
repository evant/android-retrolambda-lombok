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
package lombok.ast.syntaxChecks;

import java.util.List;

import lombok.ast.Identifier;
import lombok.ast.SyntaxProblem;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class BasicChecks {
	private final List<SyntaxProblem> problems;
	
	public BasicChecks(List<SyntaxProblem> problems) {
		this.problems = problems;
	}
	
	public void checkNameOfIdentifier(Identifier identifier) {
		String n = identifier.getName();
		if (n == null || n.length() == 0) {
			problems.add(new SyntaxProblem(identifier, "Empty identifier"));
			return;
		}
		
		if (!Character.isJavaIdentifierStart(n.charAt(0))) {
			problems.add(new SyntaxProblem(identifier,
					"Not a legal start character for a java identifier: " + n.charAt(0)));
			return;
		}
		
		for (int i = 1; i < n.length(); i++) {
			if (!Character.isJavaIdentifierPart(n.charAt(i))) {
				problems.add(new SyntaxProblem(identifier,
						"Not a legal character in a java identifier: " + n.charAt(i)));
				return;
			}
		}
	}
}
