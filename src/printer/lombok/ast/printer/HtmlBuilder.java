package lombok.ast.printer;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.ast.ForwardingASTVisitor;
import lombok.ast.Node;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.parboiled.support.InputLocation;

abstract class HtmlBuilder extends ForwardingASTVisitor {
	static final char FAIL = '‽';
	
	private final StringBuilder sb = new StringBuilder();
	private final List<String> inserts = new ArrayList<String>();
	private final String rawSource;
	private final List<String> errors = new ArrayList<String>();
	
	public HtmlBuilder(String rawSource) {
		this.rawSource = rawSource;
	}
	
	void reportAssertionFailureNext(Node node, String message, Throwable error) {
		inserts.add("<span class=\"assertionError\">" + escapeHtml(message) + "</span>");
	}
	
	private void handleInserts() {
		for (String insert : inserts) sb.append(insert);
		inserts.clear();
	}
	
	private static final String OPENERS = "{([<", CLOSERS = "})]>";
	private int parenCounter = 0;
	private final ArrayDeque<Integer> parenStack = new ArrayDeque<Integer>();
	
	
	void fail(String fail) {
		sb.append("<span class=\"fail\">").append(FAIL).append(escapeHtml(fail)).append(FAIL).append("</span>");
	}
	
	void keyword(String text) {
		sb.append("<span class=\"keyword\">").append(escapeHtml(text)).append("</span>");
	}
	
	void operator(String text) {
		sb.append("<span class=\"operator\">").append(escapeHtml(text)).append("</span>");
	}
	
	void newline() {
		sb.append("<br />");
	}
	
	void space() {
		sb.append(" ");
	}
	
	void append(String text) {
		if (" ".equals(text)) {
			space();
			return;
		}
		if ("\n".equals(text)) {
			newline();
			return;
		}
		if (text.length() == 1) {
			if (OPENERS.contains(text)) {
				parenCounter++;
				parenStack.push(parenCounter);
				sb.append("<span class=\"open\" id=\"open_").append(parenCounter).append("\">").append(escapeHtml(text)).append("</span>");
				return;
			}
			if (CLOSERS.contains(text)) {
				Integer n = parenStack.poll();
				if (n == null) {
					n = ++parenCounter;
				}
				sb.append("<span class=\"clos\" id=\"clos_").append(n).append("\">").append(escapeHtml(text)).append("</span>");
				return;
			}
		}
		
		sb.append(escapeHtml(text));
	}
	
	void buildInline(Node node, String representation) {
		generateOpenTag(node, "span");
		append(representation);
	}
	
	void closeInline() {
		sb.append("</span>");
	}
	
	private static final Pattern HTML_CLASS_SIGNIFICANT_NODE = Pattern.compile("^lombok\\.ast\\.(\\w+)$");
	
	void buildBlock(Node node, String representation) {
		generateOpenTag(node, "div");
		append(representation);
	}
	
	private void generateOpenTag(Node node, String tagName) {
		Set<String> classes = new HashSet<String>();
		findHtmlClassSignificantNodes(classes, node == null ? null : node.getClass());
		
		sb.append("<").append(tagName);
		if (!classes.isEmpty()) {
			sb.append(" class=\"").append(StringUtils.join(classes, " ")).append("\"");
		}
		sb.append(">");
		handleInserts();
	}
	
	private static void findHtmlClassSignificantNodes(Set<String> names, Class<?> c) {
		if (c == null) return;
		Matcher m = HTML_CLASS_SIGNIFICANT_NODE.matcher(c.getName());
		if (m.matches()) names.add(c.getSimpleName());
		findHtmlClassSignificantNodes(names, c.getSuperclass());
		for (Class<?> i : c.getInterfaces()) findHtmlClassSignificantNodes(names, i);
	}
	
	void closeBlock() {
		sb.append("</div>");
	}
	
	@Override public boolean visitNode(Node node) {
		buildBlock(node, HtmlPrinter.FAIL + "NOT_IMPLEMENTED: " + node.getClass().getSimpleName() + HtmlPrinter.FAIL);
		closeBlock();
		return false;
	}
	
	public void addError(InputLocation errorStart, InputLocation errorEnd, String errorMessage) {
		errors.add(String.format("<div class=\"parseError\">[(%d %d), (%d %d)] %s</div>", errorStart.row, errorStart.column, errorEnd.row, errorEnd.column, escapeHtml(errorMessage)));
	}
	
	public String toHtml() throws IOException {
		String template;
		{
			@Cleanup InputStream in = getClass().getResourceAsStream("ast.html");
			template = IOUtils.toString(in, "UTF-8");
		}
		
		return template
				.replace("{{@title}}", "AST nodes")
				.replace("{{@file}}", "source file name goes here")
				.replace("{{@script}}", "")
				.replace("{{@css}}", "")
				.replace("{{@body}}", sb.toString())
				.replace("{{@errors}}", printErrors())
				.replace("{{@timeTaken}}", "" + timeTaken);
	}
	
	private String printErrors() {
		if (errors.isEmpty()) return "<div class=\"allClear\">No parse errors!</div>";
		StringBuilder sb = new StringBuilder();
		for (String x : errors) {
			sb.append(x);
		}
		return sb.toString();
	}
	
	private String timeTaken = "(Unknown)";
	
	public void setTimeTaken(long taken) {
		timeTaken = taken + " milliseconds.";
	}
}
