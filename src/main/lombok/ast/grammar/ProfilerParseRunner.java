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
package lombok.ast.grammar;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import lombok.Data;

import org.parboiled.BasicParseRunner;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;

import com.google.common.collect.Lists;

/**
 * Like the {@code BasicParseRunner} but will also track statistics on the parse run which you can retrieve by calling {@see #getReport(boolean)} after
 * a parse run.
 */
public class ProfilerParseRunner<V> extends BasicParseRunner<V> {
	private ReportEntry<V> rootReport;
	
	public ProfilerParseRunner(Rule rule, String input) {
		super(rule, input);
	}
	
	protected boolean runRootContext() {
		return runRootContext(new Handler());
	}
	
	protected boolean runRootContext(MatchHandler<V> handler) {
		createRootContext(handler);
		return handler.matchRoot(rootContext);
	}
	
	protected void createRootContext(MatchHandler<V> matchHandler) {
		rootContext = new MatcherContext<V>(inputBuffer, startLocation, parseErrors, matchHandler, rootMatcher);
	}
	
	/**
	 * Returns a string describing, in order of 'expensiveness', the top-level failed rule chains in the parse run.
	 */
	public String getOverviewReport() {
		TreeSet<ReportEntry<V>> topLevelFailed = new TreeSet<ReportEntry<V>>();
		fillReport(topLevelFailed, rootReport);
		StringBuilder out = new StringBuilder();
		for (ReportEntry<V> entry : topLevelFailed) {
			if (entry.getSubSteps() < 100) break;
			out.append(formatReport(entry, false));
		}
		
		return out.toString();
	}
	
	/**
	 * Lists the work done by the most expensive failed rules.
	 * 
	 * First all failed rules are sorted according to how long they took, then, for each such rule,
	 * a string is produced listing it and all its child rules. These are returned.
	 * 
	 * @param topEntries Produce reports for the top {@code topEntries} most expensive failed rules.
	 *     a negative number means: All of them.
	 */
	public List<String> getExtendedReport(int topEntries) {
		TreeSet<ReportEntry<V>> topLevelFailed = new TreeSet<ReportEntry<V>>();
		fillReport(topLevelFailed, rootReport);
		int count = topEntries;
		List<String> result = Lists.newArrayList();
		StringBuilder out = new StringBuilder();
		for (ReportEntry<V> entry : topLevelFailed) {
			if (count-- == 0) return result;
			out.setLength(0);
			fillExtendedReport(out, 0, entry);
			result.add(out.toString());
		}
		
		return result;
	}
	
	private static int countInnerNodes(ReportEntry<?> entry) {
		int count = 1;
		for (ReportEntry<?> child : entry.getChildren()) count += countInnerNodes(child);
		return count;
	}
	
	private void fillExtendedReport(StringBuilder out, int spaces, ReportEntry<V> report) {
		for (int i = 0; i < spaces; i++) out.append(" ");
		out.append(formatReport(report, true));
		for (ReportEntry<V> child : report.getChildren()) {
			fillExtendedReport(out, spaces + 1, child);
		}
	}
	
	private static String formatReport(ReportEntry<?> report, boolean withSuccess) {
		return String.format("%s[%07d] %s\n",
				withSuccess ? (report.isSucceeded() ? "!" : " ") : "",
				report.getSubSteps(), report.getPath());
	}
	
	private void fillReport(Collection<ReportEntry<V>> failed, ReportEntry<V> report) {
		if (!report.isSucceeded()) {
			failed.add(report);
		} else {
			for (ReportEntry<V> child : report.getChildren()) {
				fillReport(failed, child);
			}
		}
	}
	
	@Data
	private static class ReportEntry<V> implements Comparable<ReportEntry<V>> {
		private final String path;
		private boolean succeeded;
		private final List<ReportEntry<V>> children = Lists.newArrayList();
		private int subSteps = 0;
		
		@Override public int compareTo(ReportEntry<V> o) {
			if (o.getSubSteps() < getSubSteps()) return -1;
			else if (o.getSubSteps() > getSubSteps()) return +1;
			
			if (System.identityHashCode(o) < System.identityHashCode(this)) return -1;
			if (System.identityHashCode(o) > System.identityHashCode(this)) return +1;
			return 0;
		}
	}
	
	public final class Handler implements MatchHandler<V> {
		private final List<ReportEntry<V>> stack = Lists.newArrayList();
		
		public boolean matchRoot(MatcherContext<V> rootContext) {
			return rootContext.runMatcher();
		}
		
		public boolean match(MatcherContext<V> context) {
			String path = stack.isEmpty() ? "" : stack.get(stack.size() - 1).getPath();
			path += String.format("/%s[%d]", context.getMatcher().getLabel(), context.getCurrentLocation().getIndex());
			ReportEntry<V> report = new ReportEntry<V>(path);
			stack.add(report);
			boolean result = context.getMatcher().match(context);
			report.setSucceeded(result);
			stack.remove(stack.size() -1);
			if (stack.isEmpty()) rootReport = report;
			else {
				ReportEntry<V> parent = stack.get(stack.size() - 1);
				parent.getChildren().add(report);
				parent.setSubSteps(parent.getSubSteps() + 1 + report.getSubSteps());
			}
			return result;
		}
	}
}
