package lombok.ast.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import lombok.Data;

import org.parboiled.BasicParseRunner;
import org.parboiled.MatchHandler;
import org.parboiled.MatcherContext;
import org.parboiled.Rule;
import org.parboiled.support.MatcherPath;

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
	 * 
	 * @param countInner also counts up how many child rules, succeeded or failed, were part of calculating a listed run.
	 */
	public String getReport(boolean countInner) {
		TreeSet<ReportEntry<V>> topLevelFailed = new TreeSet<ReportEntry<V>>();
		fillReport(topLevelFailed, rootReport);
		StringBuilder out = new StringBuilder();
		for (ReportEntry<V> entry : topLevelFailed) {
			if (countInner) {
				out.append(String.format("[%07d][%07d] %s\n", entry.getTimeTaken(), countInnerNodes(entry), entry.getPath()));
			} else {
				out.append(String.format("[%07d] %s\n", entry.getTimeTaken(), entry.getPath()));
			}
		}
		
		return out.toString();
	}
	
	private int countInnerNodes(ReportEntry<V> entry) {
		int count = 1;
		for (ReportEntry<V> child : entry.getChildren()) count += countInnerNodes(child);
		return count;
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
		private final long start;
		private final MatcherPath<V> path;
		private long end;
		private boolean succeeded;
		private final List<ReportEntry<V>> children = new ArrayList<ReportEntry<V>>();
		
		long getTimeTaken() {
			return getEnd() - getStart();
		}
		
		@Override public int compareTo(ReportEntry<V> o) {
			if (o.getTimeTaken() < getTimeTaken()) return -1;
			else if (o.getTimeTaken() > getTimeTaken()) return +1;
			
			if (System.identityHashCode(o) < System.identityHashCode(this)) return -1;
			if (System.identityHashCode(o) > System.identityHashCode(this)) return +1;
			return 0;
		}
	}
	
	public final class Handler implements MatchHandler<V> {
		private final List<ReportEntry<V>> stack = new ArrayList<ReportEntry<V>>();
		
		public boolean matchRoot(MatcherContext<V> rootContext) {
			return rootContext.runMatcher();
		}
		
		public boolean match(MatcherContext<V> context) {
			ReportEntry<V> report = new ReportEntry<V>(System.currentTimeMillis(), context.getPath());
			stack.add(report);
			boolean result = context.getMatcher().match(context);
			report.setEnd(System.currentTimeMillis());
			report.setSucceeded(result);
			stack.remove(stack.size() -1);
			if (stack.isEmpty()) rootReport = report;
			else stack.get(stack.size() -1).getChildren().add(report);
			return result;
		}
	}
}
