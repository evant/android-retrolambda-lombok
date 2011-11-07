/*
 * Copyright (C) 2011 The Project Lombok Authors.
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
package lombok.ast.ecj;


import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public class EcjTreeOperations {
	private EcjTreeOperations() {
		// static utility class
	}
	
	private static String convertToString0(ASTNode tree, EcjTreePrinter printer) {
		dodgePostFixArraysInVarDeclarations(printer);
		dodgeCombinedBinaryExpressions(printer);
		dodgeExtendedStringLiterals(printer);
		printer.visit(tree);
		String string = printer.getContent();
		return string;
	}
	
	public static String convertToString(ASTNode tree) {
		return convertToString0(tree, EcjTreePrinter.printerWithPositions());
	}
	
	public static String convertToStringNoPositions(ASTNode tree) {
		return convertToString0(tree, EcjTreePrinter.printerWithoutPositions());
	}
	
	/*
	 * boolean[] s, t; results in 1 TypeReference, pointed at by both LocalDeclarations.
	 * boolean s[], t[]; results in 2 identical TypeReference instances, but we can't figure that out and get it wrong.
	 * It doesn't actually matter though, and it's virtually impossible to get right (would involve having to reparse source).
	 */
	private static void dodgePostFixArraysInVarDeclarations(EcjTreePrinter printer) {
		printer.skipReferenceTracking(LocalDeclaration.class, TypeReference.class);
		printer.skipReferenceTracking(FieldDeclaration.class, TypeReference.class);
	}
	
	private static void dodgeExtendedStringLiterals(EcjTreePrinter printer) {
		printer.skipProperty(StringLiteral.class, "lineNumber");
		printer.skipPropertyIfHasValue(ExtendedStringLiteral.class, "lineNumber", -1);
		printer.skipPropertyIfHasValue(ExtendedStringLiteral.class, "lineNumber", -2);
		printer.skipPropertyIfHasValue(StringLiteral.class, "lineNumber", -1);
		printer.skipPropertyIfHasValue(StringLiteral.class, "lineNumber", -2);
		printer.stringReplace("ExtendedStringLiteral", "StringLiteral");
	}
	
	private static void dodgeCombinedBinaryExpressions(EcjTreePrinter printer) {
		printer.skipProperty(CombinedBinaryExpression.class, "arity");
		printer.skipProperty(CombinedBinaryExpression.class, "arityMax");
		printer.skipProperty(CombinedBinaryExpression.class, "referencesTable");
		printer.stringReplace("CombinedBinaryExpression", "BinaryExpression");
	}
}
