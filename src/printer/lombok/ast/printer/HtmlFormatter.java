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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.ast.DescribedNode;
import lombok.ast.Node;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

public class HtmlFormatter implements SourceFormatter {
	private final StringBuilder sb = new StringBuilder();
	private final String rawSource;
	private final List<String> errors = Lists.newArrayList();
	private String nextElementName;
	
	private static String escapeHtml(String in) {
		return in.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	public HtmlFormatter(String rawSource) {
		this.rawSource = rawSource;
	}
	
	private static final String OPENERS = "{([<", CLOSERS = "})]>";
	private int parenCounter = 0;
	private final ArrayDeque<Integer> parenStack = new ArrayDeque<Integer>();
	
	
	@Override public void fail(String fail) {
		sb.append("<span class=\"fail\">").append(FAIL).append(escapeHtml(fail)).append(FAIL).append("</span>");
	}
	
	@Override public void property(String name, Object value) {
	}
	
	@Override public void keyword(String text) {
		sb.append("<span class=\"keyword\">").append(escapeHtml(text)).append("</span>");
	}
	
	@Override public void operator(String text) {
		sb.append("<span class=\"operator\">").append(escapeHtml(text)).append("</span>");
	}
	
	@Override public void verticalSpace() {
		sb.append("<br />");
	}
	
	@Override public void space() {
		sb.append(" ");
	}
	
	@Override public void append(String text) {
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
	
	@Override public void buildInline(Node node) {
		generateOpenTag(node, "span");
	}
	
	@Override public void closeInline() {
		sb.append("</span>");
	}
	
	@Override public void startSuppressBlock() {
		sb.append("<span class=\"blockSuppress\">");
	}
	
	@Override public void endSuppressBlock() {
		sb.append("</span>");
	}
	
	@Override
	public void startSuppressIndent() {
		sb.append("<div class=\"indentSuppress\">");
	}
	
	@Override
	public void endSuppressIndent() {
		sb.append("</div>");
	}
	
	private static final Pattern HTML_CLASS_SIGNIFICANT_NODE = Pattern.compile("^lombok\\.ast\\.(\\w+)$");
	
	@Override public void buildBlock(Node node) {
		generateOpenTag(node, "div");
	}
	
	private void generateOpenTag(Node node, String tagName) {
		Set<String> classes = Sets.newHashSet();
		AtomicReference<String> kind = new AtomicReference<String>();
		findHtmlClassSignificantNodes(classes, kind, node == null ? null : node.getClass());
		String description = node instanceof DescribedNode ? ((DescribedNode)node).getDescription() : null;
		
		sb.append("<").append(tagName);
		if (!classes.isEmpty()) {
			sb.append(" class=\"");
			Joiner.on(' ').appendTo(sb, classes);
			sb.append("\"");
		}
		if (nextElementName != null) {
			sb.append(" relation=\"").append(escapeHtml(nextElementName)).append("\"");
			nextElementName = null;
		}
		if (kind.get() != null) {
			sb.append(" kind=\"").append(escapeHtml(kind.get())).append("\"");
		}
		if (description != null) {
			sb.append(" description=\"").append(escapeHtml(description)).append("\"");
		}
		
		sb.append(">");
	}
	
	private static void findHtmlClassSignificantNodes(Set<String> names, AtomicReference<String> kind, Class<?> c) {
		if (c == null) return;
		if (java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
			Matcher m = HTML_CLASS_SIGNIFICANT_NODE.matcher(c.getName());
			if (m.matches()) {
				names.add(c.getSimpleName());
				if (kind.get() == null) kind.set(c.getSimpleName());
			}
		}
		findHtmlClassSignificantNodes(names, kind, c.getSuperclass());
		for (Class<?> i : c.getInterfaces()) findHtmlClassSignificantNodes(names, kind, i);
	}
	
	@Override public void closeBlock() {
		sb.append("</div>");
	}
	
	@Override public void addError(int errorStart, int errorEnd, String errorMessage) {
		errors.add(String.format("<div class=\"parseError\">%s</div>", escapeHtml(errorMessage)));
	}
	
	private String readResource(String resource) throws IOException {
		@Cleanup InputStream in = getClass().getResourceAsStream(resource);
		return new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
	}
	
	@Override public String finish() throws IOException {
		String template = readResource("web/ast.html");
		String cssContent = readResource("web/ast.css");
		String scriptContent = readResource("web/ast.js");
		String jQuery = readResource("web/jquery.js");
		
		return template
				.replace("{{@title}}", "AST nodes")
				.replace("{{@file}}", "source file name goes here")
				.replace("{{@jQuery}}", jQuery)
				.replace("{{@script}}", scriptContent)
				.replace("{{@css}}", cssContent)
				.replace("{{@body}}", sb.toString())
				.replace("{{@errors}}", printErrors())
				.replace("{{@rawSource}}", escapeHtml(rawSource))
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
	
	@Override public void setTimeTaken(long taken) {
		timeTaken = taken + " milliseconds.";
	}
	
	@Override public void nameNextElement(String name) {
		assert nextElementName == null;
		nextElementName = name;
	}
}
