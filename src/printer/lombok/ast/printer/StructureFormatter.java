package lombok.ast.printer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.ast.DescribedNode;
import lombok.ast.Node;
import lombok.ast.grammar.Source;
import lombok.ast.grammar.SourceStructure;

import com.google.common.base.Joiner;

public class StructureFormatter implements SourceFormatter {
	private static final String INDENT = "    ";
	@Getter private final Source source;
	private final StringBuilder sb = new StringBuilder();
	private final List<String> errors = new ArrayList<String>();
	private int indent;
	private final Map<Node, Collection<SourceStructure>> sourceStructures;
	private String name;
	
	public StructureFormatter(Source source) {
		this.source = source;
		sourceStructures = source.getSourceStructures();
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
		a("[%s %s%s (%d-%d)]\n", type, name, description, node.getPosition().getStart(), node.getPosition().getEnd());
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
