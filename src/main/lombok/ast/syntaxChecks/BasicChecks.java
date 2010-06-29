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

import static lombok.ast.Message.*;
import static lombok.ast.syntaxChecks.MessageKey.*;

import lombok.ast.Identifier;
import lombok.ast.Node;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class BasicChecks {
	public void checkNameOfIdentifier(Identifier identifier) {
		String n = identifier.astValue();
		if (n.length() == 0) {
			identifier.addMessage(error(IDENTIFIER_EMPTY, "Empty Identifier"));
			return;
		}
		
		if (!Character.isJavaIdentifierStart(n.charAt(0))) {
			identifier.addMessage(error(IDENTIFIER_INVALID, "Not a legal start character for a java identifier: " + n.charAt(0)));
			return;
		}
		
		for (int i = 1; i < n.length(); i++) {
			if (!Character.isJavaIdentifierPart(n.charAt(i))) {
				identifier.addMessage(error(IDENTIFIER_INVALID, "Not a legal character in a java identifier: " + n.charAt(i)));
				return;
			}
		}
	}
	
	static void checkVarDefIsSimple(Node node, Node rawVarDef, String descriptionOfOuter, String descriptionOfRelation) {
		if (!(rawVarDef instanceof VariableDefinition)) return;
		switch (((VariableDefinition)rawVarDef).rawVariables().size()) {
		case 0: return;
		case 1: break;
		default:
			rawVarDef.addMessage(error(VARIABLEDEFINITION_ONLY_ONE, String.format("%s can only declare one %s variable", descriptionOfOuter, descriptionOfRelation)));
		}
		
		for (VariableDefinitionEntry entry : ((VariableDefinition)rawVarDef).astVariables()) {
			if (entry.rawInitializer() != null) entry.addMessage(error(VARIABLEDEFINITIONENTRY_INITIALIZER_NOT_ALLOWED, String.format(
					"%s can only declare %s variables without an initializer", descriptionOfOuter, descriptionOfRelation)));
		}
	}
}
