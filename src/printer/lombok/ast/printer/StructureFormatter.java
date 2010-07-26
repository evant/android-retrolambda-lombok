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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.DescribedNode;
import lombok.ast.Node;
import lombok.ast.grammar.Source;
import lombok.ast.grammar.SourceStructure;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class StructureFormatter implements SourceFormatter {
	private static final String INDENT = "    ";
	private final StringBuilder sb = new StringBuilder();
	private final List<String> errors = Lists.newArrayList();
	private int indent;
	private final Map<Node, Collection<SourceStructure>> sourceStructures;
	private String name;
	private final String nodeFormatString;
	
	
	public static StructureFormatter formatterWithoutPositions() {
		return new StructureFormatter(Collections.<Node, Collection<SourceStructure>>emptyMap(), false);
	}
	
	public static StructureFormatter formatterWithPositions() {
		return new StructureFormatter(Collections.<Node, Collection<SourceStructure>>emptyMap(), true);
	}
	
	public static StructureFormatter formatterWithEverything(Source source) {
		return new StructureFormatter(source.getSourceStructures(), true);
	}
	
	private StructureFormatter(Map<Node, Collection<SourceStructure>> sourceStructures, boolean printPositions) {
		this.sourceStructures = sourceStructures;
		this.nodeFormatString = printPositions ? "[%s %s%s (%d-%d)]\n" : "[%s %s%s]\n";
	}
	
	private void a(String in, Object... args) {
		for (int i = 0; i < indent; i++) sb.append(INDENT);
		if (name != null) {
			sb.append(name).append(": ");
			name = null;
		}
		if (args.length == 0) sb.append(in);
		else sb.append(String.format(in, args));
	}
	
	@Override public void buildInline(Node node) {
		buildNode("I", node);
	}
	
	@Override public void buildBlock(Node node) {
		buildNode("B", node);
	}
	
	private void buildNode(String type, Node node) {
		if (node == null) {
			indent++;
			return;
		}
		String name = node.getClass().getSimpleName();
		String description = "";
		if (node instanceof DescribedNode) description = " " + ((DescribedNode)node).getDescription();
		a(nodeFormatString, type, name, description, node.getPosition().getStart(), node.getPosition().getEnd());
		indent++;
		if (sourceStructures.containsKey(node)) {
			for (SourceStructure struct : sourceStructures.get(node)) {
				a("STRUCT: %s (%d-%d)\n", struct.getContent(), struct.getPosition().getStart(), struct.getPosition().getEnd());
			}
		}
	}
	
	@Override public void fail(String fail) {
		a("FAIL: " + fail);
	}
	
	@Override public void keyword(String text) {
	}
	
	@Override public void operator(String text) {
	}
	
	@Override public void verticalSpace() {
	}
	
	@Override public void space() {
	}
	
	@Override public void append(String text) {
	}
	
	@Override public void startSuppressBlock() {
	}
	
	@Override public void endSuppressBlock() {
	}
	
	@Override public void startSuppressIndent() {
	}
	
	@Override public void endSuppressIndent() {
	}
	
	@Override public void closeInline() {
		indent--;
	}
	
	@Override public void closeBlock() {
		indent--;
	}
	
	@Override public void addError(int errorStart, int errorEnd, String errorMessage) {
		errors.add(String.format("%d-%d: %s", errorStart, errorEnd, errorMessage));
	}
	
	@Override public String finish() {
		if (!errors.isEmpty()) {
			indent = 0;
			a("\n\n\nERRORS: \n");
			a(Joiner.on('\n').join(errors));
			errors.clear();
		}
		return sb.toString();
	}
	
	@Override public void setTimeTaken(long taken) {
	}
	
	@Override public void nameNextElement(String name) {
		this.name = name;
	}
}
