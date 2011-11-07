/*
 * Copyright (C) 2010 The Project Lombok Authors.
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

import static lombok.ast.syntaxChecks.MessageKey.*;
import static lombok.ast.Message.*;

import lombok.ast.Block;
import lombok.ast.Case;
import lombok.ast.Catch;
import lombok.ast.Default;
import lombok.ast.DoWhile;
import lombok.ast.For;
import lombok.ast.ForEach;
import lombok.ast.If;
import lombok.ast.Node;
import lombok.ast.Statement;
import lombok.ast.Switch;
import lombok.ast.Try;
import lombok.ast.TypeDeclaration;
import lombok.ast.VariableDeclaration;
import lombok.ast.While;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class StatementChecks {
	public void checkNotLoneTry(Try node) {
		if (node.rawCatches().size() == 0 && node.rawFinally() == null) {
			node.addMessage(error(TRY_LONE_TRY, "try statement with no catches and no finally"));
		}
	}
	
	public void checkDeclarationsAsDirectChildWhile(While node) {
		checkDeclarationsAsDirectChild(node, node.rawStatement());
	}
	
	public void checkDeclarationsAsDirectChildDo(DoWhile node) {
		checkDeclarationsAsDirectChild(node, node.rawStatement());
	}
	
	public void checkDeclarationsAsDirectChildForEach(ForEach node) {
		checkDeclarationsAsDirectChild(node, node.rawStatement());
	}
	
	public void checkDeclarationsAsDirectChildIf(If node) {
		checkDeclarationsAsDirectChild(node, node.rawStatement());
		checkDeclarationsAsDirectChild(node, node.rawElseStatement());
	}
	
	public void checkDeclarationsAsDirectChildFor(For node) {
		checkDeclarationsAsDirectChild(node, node.rawStatement());
	}
	
	private void checkDeclarationsAsDirectChild(Node n, Node c) {
		if (c instanceof VariableDeclaration) {
			c.addMessage(error(DECLARATION_NOT_ALLOWED, "Variable declarations only make sense in the context of a block."));
		}
		
		if (c instanceof TypeDeclaration) {
			c.addMessage(error(DECLARATION_NOT_ALLOWED, "Type declarations only make sense in the context of a block or other type."));
		}
	}
	
	public void checkVarDefOfCatch(Catch node) {
		BasicChecks.checkVarDefIsSimple(node, node.rawExceptionDeclaration(), "catch blocks", "exception");
	}
	
	public void checkVarDefOfForEach(ForEach node) {
		BasicChecks.checkVarDefIsSimple(node, node.rawVariable(), "for-each statements", "loop");
	}
	
	public void checkCaseChildOfSwitch(Case node) {
		checkChildOfSwitch(node, "case");
	}
	
	public void checkDefaultChildOfSwitch(Default node) {
		checkChildOfSwitch(node, "default");
	}
	
	private void checkChildOfSwitch(Statement node, String desc) {
		if (node.getParent() == null) return;
		
		Block p = node.upToBlock();
		Switch gp = p == null ? null : p.upToSwitch();
		boolean genError = false;
		
		genError = p == null;
		genError |= gp == null && p.getParent() != null;
		
		if (genError) {
			node.addMessage(error(STATEMENT_ONLY_LEGAL_IN_SWITCH, desc + " statements are only legal directly inside switch statements."));
		}
	}
	
	public void checkSwitchStartsWithDefaultOrCase(Switch node) {
		Block body = node.astBody();
		if (body != null) {
			Statement first = body.astContents().first();
			if (first != null && !(first instanceof Case) && !(first instanceof Default)) {
				node.addMessage(error(SWITCH_DOES_NOT_START_WITH_CASE, "switch statements should start with a default or case statement."));
			}
		}
	}
}
