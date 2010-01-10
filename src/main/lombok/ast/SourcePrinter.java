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
//package lombok.ast;
//
//public class SourcePrinter implements ASTVisitor {
//	private StringBuilder sb = new StringBuilder();
//	private int indent = 0;
//	private boolean suppressIndent = false;
//	
//	private static final String[] INDENTS = {
//		"",
//		"    ",
//		"        ",
//		"            ",
//		"                ",
//		"                    ",
//		"                        "
//	};
//	
//	private StringBuilder indent() {
//		if (suppressIndent) {
//			suppressIndent = false;
//			return sb;
//		}
//		
//		int c = indent;
//		while (c > 0) {
//			sb.append(INDENTS[Math.min(c, INDENTS.length-1)]);
//			c -= INDENTS.length-1;
//		}
//		
//		return sb;
//	}
//	
//	private StringBuilder printX(Node x, String name, boolean suppressIndent, boolean endWithNewline) {
//		this.suppressIndent = suppressIndent;
//		if (x == null) {
//			return indent().append("/* MISSING ").append(name == null ? "NODE" : name.toUpperCase()).append(" */").append(endWithNewline ? "\n" : "");
//		}
//		
//		indent++;
//		x.accept(this);
//		indent--;
//		return sb;
//	}
//	
//	private StringBuilder printX(String x, String name, boolean endWithNewline) {
//		if (x == null) {
//			return indent().append("/* MISSING ").append(name == null ? "IDENTIFIER" : name.toUpperCase()).append(" */").append(endWithNewline ? "\n" : "");
//		}
//		
//		sb.append(name);
//		return sb;
//	}
//	
//	private StringBuilder printBlock(Block block) {
//		if (block == null) {
//			sb.append("{\n");
//			indent++;
//			indent().append("/*MISSING BLOCK*/\n");
//			indent--;
//			return indent().append("}\n");
//		}
//		
//		this.suppressIndent = true;
//		block.accept(this);
//		return sb;
//	}
//	
//	@Override public boolean visitType(Type node) {
//		indent();
//		if (node.getWildcard() != null) {
//			switch (node.getWildcard()) {
//			case UNBOUND:
//				sb.append("?");
//				return true;
//			case EXTENDS:
//				sb.append("? extends ");
//				break;
//			case SUPER:
//				sb.append("? super ");
//				break;
//			}
//		}
//		printX(node.getTypeName(), "type name", false);
//		if (node.hasGenerics()) {
//			sb.append("<");
//			boolean first = true;
//			for (Type g : node.getGenerics()) {
//				if (first) first = false;
//				else sb.append(", ");
//				suppressIndent = true;
//				g.accept(this);
//			}
//			sb.append(">");
//		}
//		return true;
//	}
//	
//	@Override public boolean visitVariableDeclaration(VariableDeclaration node) {
//		indent();
//		printX(node.getType(), "type", true, false);
//		sb.append(" ");
//		printX(node.getVariableName(), "variable name", false);
//		sb.append(";\n");
//		
//		return true;
//	}
//	
//	@Override public boolean visitAssert(Assert node) {
//		indent().append("assert ");
//		printX(node.getAssertion(), "assertion", true, false);
//		if (node.getMessage() != null) {
//			sb.append(": ");
//			printX(node.getMessage(), "message", true, false);
//		}
//		sb.append(";\n");
//		return true;
//	}
//	
//	public boolean visitIf(If node) {
//		indent().append("if (");
//		printX(node.getCondition(), "condition", true, false).append(") ");
//		printX(node.getStatement(), "statement", true, true);
//		if (node.getElseStatement() != null) {
//			indent().append("else ");
//			printX(node.getElseStatement(), "elseStatement", true, true);
//		}
//		
//		return true;
//	}
//	
//	@Override public boolean visitSynchronized(Synchronized node) {
//		indent().append("synchronized (");
//		printX(node.getLock(), "lock", true, false).append(") ");
//		printBlock(node.getBody());
//		
//		return true;
//	}
//	
//	@Override public String toString() {
//		return sb.toString();
//	}
//	
//	@Override public boolean visitWhile(While node) {
//		indent().append("while (");
//		printX(node.getCondition(), "condition", true, false).append(") ");
//		printX(node.getStatement(), "statement", true, true);
//		
//		return true;
//	}
//	
//	@Override public boolean visitDoWhile(DoWhile node) {
//		indent().append("do ");
//		printX(node.getStatement(), "statement", true, true);
//		indent().append("while (");
//		printX(node.getCondition(), "condition", true, false).append(");");
//		
//		return true;
//	}
//	
//	@Override public boolean visitFor(For node) {
//		indent().append("for (");
//		if (node.getInitialization() != null) {
//			suppressIndent = true;
//			node.getInitialization().accept(this);
//		}
//		sb.append("; ");
//		if (node.getCondition() != null) {
//			suppressIndent = true;
//			node.getCondition().accept(this);
//		}
//		sb.append("; ");
//		if (node.getIncrement() != null) {
//			suppressIndent = true;
//			node.getIncrement().accept(this);
//		}
//		sb.append(") ");
//		printX(node.getStatement(), "statement", true, true);
//		
//		return true;
//	}
//	
//	@Override public boolean visitForEach(ForEach node) {
//		indent().append("for (");
//		printX(node.getCountVariable(), "countvariable", true, false);
//		sb.append(" : ");
//		printX(node.getIterable(), "iterable", true, false);
//		sb.append(") ");
//		printX(node.getStatement(), "statement", true, true);
//		
//		return true;
//	}
//	
//	@Override public boolean visitTry(Try node) {
//		indent().append("try ");
//		printBlock(node.getBody());
//		for (Catch c : node.getCatches()) visitCatch(c);
//		if (node.getFinallyBlock() != null) {
//			indent().append("finally ");
//			printBlock(node.getFinallyBlock());
//		}
//		
//		return true;
//	}
//	
//	@Override public boolean visitCatch(Catch node) {
//		indent().append("catch (");
//		printX(node.getExceptionDeclaration(), "exception", true, false).append(") ");
//		printBlock(node.getBody());
//		
//		return true;
//	}
//	
//	@Override public boolean visitBlock(Block block) {
//		indent().append("{\n");
//		indent++;
//		for (Statement s : block.getContents()) {
//			s.accept(this);
//		}
//		indent--;
//		indent().append("}\n");
//		return true;
//	}
//}
