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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import lombok.ast.Comment;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.JavadocContainer;
import lombok.ast.Node;
import lombok.ast.Position;

import org.parboiled.Context;
import org.parboiled.RecoveringParseRunner;
import org.parboiled.errors.ParseError;
import org.parboiled.support.ParsingResult;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

public class Source {
	@Getter private final String name;
	@Getter private final String rawInput;
	private List<Node> nodes;
	private List<ParseProblem> problems;
	private List<Comment> comments;
	private boolean parsed;
	private ParsingResult<Node> parsingResult;
	
	private TreeMap<Integer, Integer> positionDeltas;
	private Map<org.parboiled.Node<Node>, Node> registeredStructures;
	private Map<org.parboiled.Node<Node>, List<Comment>> registeredComments;
	private String preprocessed;
	private Map<Node, Collection<SourceStructure>> cachedSourceStructures;
	private List<Integer> lineEndings;
	
	public Source(String rawInput, String name) {
		this.rawInput = rawInput;
		this.name = name;
		clear();
	}
	
	public List<Node> getNodes() {
		parseCompilationUnit();
		if (!parsed) throw new IllegalStateException("Code hasn't been parsed yet.");
		return nodes;
	}
	
	public List<ParseProblem> getProblems() {
		parseCompilationUnit();
		return problems;
	}
	
	public void clear() {
		nodes = Lists.newArrayList();
		problems = Lists.newArrayList();
		comments = Lists.newArrayList();
		lineEndings = ImmutableList.of();
		parsed = false;
		parsingResult = null;
		positionDeltas = Maps.newTreeMap();
		registeredComments = new MapMaker().weakKeys().makeMap();
		registeredStructures = new MapMaker().weakKeys().makeMap();
		cachedSourceStructures = null;
	}
	
	public String getOverviewProfileInformation() {
		clear();
		preProcess();
		ParserGroup group = new ParserGroup(this);
		ProfilerParseRunner<Node> runner = new ProfilerParseRunner<Node>(group.structures.compilationUnitEoi(), preprocessed);
		this.parsingResult = runner.run();
		StringBuilder out = new StringBuilder();
		out.append(runner.getOverviewReport());
		postProcess();
		return out.toString();
	}
	
	public List<String> getDetailedProfileInformation(int top) {
		clear();
		preProcess();
		ParserGroup group = new ParserGroup(this);
		ProfilerParseRunner<Node> runner = new ProfilerParseRunner<Node>(group.structures.compilationUnitEoi(), preprocessed);
		this.parsingResult = runner.run();
		List<String> result = Lists.newArrayList();
		result.add(runner.getOverviewReport());
		result.addAll(runner.getExtendedReport(top));
		postProcess();
		return result;
	}
	
	private List<Integer> calculateLineEndings() {
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		
		boolean atCR = false;
		for (int i = 0; i < rawInput.length(); i++) {
			char c = rawInput.charAt(i);
			if (c == '\n' && !atCR) builder.add(i);
			atCR = c == '\r';
			if (atCR) builder.add(i);
		}
		return builder.build();
	}
	
	public void parseCompilationUnit() {
		if (parsed) return;
		preProcess();
		ParserGroup group = new ParserGroup(this);
		parsingResult = RecoveringParseRunner.run(group.structures.compilationUnitEoi(), preprocessed);
		postProcess();
	}
	
	private void postProcess() {
		nodes.add(parsingResult.parseTreeRoot.getValue());
		for (ParseError error : parsingResult.parseErrors) {
			int errStart = error.getStartIndex();
			int errEnd = error.getEndIndex();
			problems.add(new ParseProblem(new Position(mapPosition(errStart), mapPosition(errEnd)), error.toString()));
		}
		
		gatherComments(parsingResult.parseTreeRoot);
		
		comments = Collections.unmodifiableList(comments);
		nodes = Collections.unmodifiableList(nodes);
		problems = Collections.unmodifiableList(problems);
		
		rtrimPositions(nodes, comments);
		
		//TODO Write test case with javadoc intermixed with empty declares.
		//TODO test javadoc on a package declaration.
		//TODO javadoc in between keywords.
		
		associateJavadoc(comments, nodes);
		
		fixPositions(nodes);
		fixPositions(comments);
		
		parsed = true;
	}
	
	void registerStructure(Node node, org.parboiled.Node<Node> pNode) {
		registeredStructures.put(pNode, node);
	}
	
	public Map<Node, Collection<SourceStructure>> getSourceStructures() {
		if (cachedSourceStructures != null) return cachedSourceStructures;
		parseCompilationUnit();
		ListMultimap<Node, SourceStructure> map = LinkedListMultimap.create();
		
		org.parboiled.Node<Node> pNode = parsingResult.parseTreeRoot;
		
		buildSourceStructures(pNode, null, map);
		
		Map<Node, Collection<SourceStructure>> result = map.asMap();
		
		for (Collection<SourceStructure> structures : result.values()) {
			for (SourceStructure structure : structures) {
				structure.setPosition(new Position(
						mapPosition(structure.getPosition().getStart()),
						mapPosition(structure.getPosition().getEnd())));
			}
		}
		
		return cachedSourceStructures = result;
	}
	
	private void addSourceStructure(ListMultimap<Node, SourceStructure> map, Node node, SourceStructure structure) {
		if (structure.getPosition().size() > 0 && structure.getContent().trim().length() > 0 &&
				!structure.getPosition().equals(node.getPosition())) {
			
			map.put(node, structure);
		}
	}
	
	private void buildSourceStructures(org.parboiled.Node<Node> pNode, Node owner, ListMultimap<Node, SourceStructure> map) {
		Node target = registeredStructures.remove(pNode);
		if (target != null || pNode.getChildren().isEmpty()) {
			int start = pNode.getStartIndex();
			int end = pNode.getEndIndex();
			String text = preprocessed.substring(start, end);
			SourceStructure structure = new SourceStructure(new Position(start, end), text);
			if (target != null) addSourceStructure(map, target, structure);
			else if (pNode.getValue() != null && !(pNode.getValue() instanceof TemporaryNode)) addSourceStructure(map, pNode.getValue(), structure);
			else if (owner != null) addSourceStructure(map, owner, structure);
		} else {
			Node possibleOwner = pNode.getValue();
			if (possibleOwner instanceof TemporaryNode) possibleOwner = null;
			for (org.parboiled.Node<Node> child : pNode.getChildren()) {
				if (child.getValue() == null || child.getValue() instanceof TemporaryNode) continue;
				/* If the next if holds true, then we aren't the true generator; the child generated the node and this pNode adopted it */
				if (child.getValue() == possibleOwner) possibleOwner = null;
			}
			
			if (possibleOwner != null) owner = possibleOwner;
			
			for (org.parboiled.Node<Node> child : pNode.getChildren()) {
				buildSourceStructures(child, owner, map);
			}
		}
	}
	
	/**
	 * The end positions of all nodes include their trailing whitespace which isn't very convenient.
	 * We'll 'fix' the end marker of each node by trimming it back. This is somewhat complicated as comments also need to be trimmed across.
	 * We also adjust all positions to conform with the raw input (undoing any positional shifts caused by preprocessing).
	 */
	private void rtrimPositions(List<Node> nodes, List<Comment> comments) {
		final boolean[] whitespace = new boolean[preprocessed.length()];
		for (Comment comment : comments) {
			Position p = comment.getPosition();
			if (!p.isUnplaced()) {
				for (int i = p.getStart(); i < p.getEnd(); i++) whitespace[i] = true;
			}
		}
		
		/* Process actual whitespace in preprocessed source data */ {
			char[] chars = preprocessed.toCharArray();
			for (int i = 0; i < chars.length; i++) if (Character.isWhitespace(chars[i])) whitespace[i] = true;
		}
		
		for (Node node : nodes) node.accept(new ForwardingAstVisitor() {
			@Override public boolean visitNode(Node node) {
				Position p = node.getPosition();
				if (p.isUnplaced()) return false;
				
				int trimmed = Math.min(whitespace.length, p.getEnd());
				while (trimmed > 0 && whitespace[trimmed-1]) trimmed--;
				
				int start, end;
				
				if (p.getEnd() - p.getStart() == 0) {
					if (node.getParent() != null) {
						start = Math.min(node.getParent().getPosition().getEnd(), Math.max(node.getParent().getPosition().getStart(), p.getStart()));
						end = start;
					} else {
						start = p.getStart();
						end = start;
					}
				} else {
					start = p.getStart();
					end = Math.max(trimmed, start);
				}
				
				node.setPosition(new Position(start, end));
				
				return false;
			}
		});
	}
	
	private void fixPositions(List<? extends Node> nodes) {
		for (Node node : nodes) node.accept(new ForwardingAstVisitor() {
			@Override public boolean visitNode(Node node) {
				Position p = node.getPosition();
				if (!p.isUnplaced()) {
					node.setPosition(new Position(mapPosition(p.getStart()), mapPosition(p.getEnd())));
				}
				if (node instanceof Expression) {
					List<Position> list = ((Expression)node).astParensPositions();
					if (list != null) {
						ListIterator<Position> li = list.listIterator();
						while (li.hasNext()) {
							Position parenPos = li.next();
							if (!parenPos.isUnplaced()) {
								parenPos = new Position(mapPosition(parenPos.getStart()), mapPosition(parenPos.getEnd()));
								li.set(parenPos);
							}
						}
					}
				}
				return false;
			}
		});
	}
	
	/**
	 * Associates comments that are javadocs to the node they belong to, by checking if the node that immediately follows a javadoc node is a JavadocContainer.
	 */
	private void associateJavadoc(List<Comment> comments, List<Node> nodes) {
		final TreeMap<Integer, Node> startPosMap = Maps.newTreeMap();
		for (Node node : nodes) node.accept(new ForwardingAstVisitor() {
			@Override public boolean visitNode(Node node) {
				if (node.isGenerated()) return false;
				int startPos = node.getPosition().getStart();
				Node current = startPosMap.get(startPos);
				if (current == null || !(current instanceof JavadocContainer)) {
					startPosMap.put(startPos, node);
				}
				
				return false;
			}
		});
		
		for (Comment comment : comments) {
			if (!comment.isJavadoc()) continue;
			Map<Integer, Node> tailMap = startPosMap.tailMap(comment.getPosition().getEnd());
			if (tailMap.isEmpty()) continue;
			Node assoc = tailMap.values().iterator().next();
			if (!(assoc instanceof JavadocContainer)) continue;
			JavadocContainer jc = (JavadocContainer) assoc;
			if (jc.rawJavadoc() != null) {
				if (jc.rawJavadoc().getPosition().getEnd() >= comment.getPosition().getEnd()) continue;
			}
			jc.rawJavadoc(comment);
		}
	}
	
	void registerComment(Context<Node> context, Comment c) {
		List<Comment> list = registeredComments.get(context);
		if (list == null) {
			list = Lists.newArrayList();
			registeredComments.put(context.getSubNodes().get(0), list);
		}
		list.add(c);
	}
	
	/**
	 * Delves through the parboiled node tree to find comments.
	 */
	private boolean gatherComments(org.parboiled.Node<Node> parsed) {
		boolean foundComments = false;
		for (org.parboiled.Node<Node> child : parsed.getChildren()) {
			foundComments |= gatherComments(child);
		}
		
		List<Comment> cmts = registeredComments.get(parsed);
		if (cmts != null) for (Comment c : cmts) {
			comments.add(c);
			return true;
		}
		
		return foundComments;
	}
	
	private void setPositionDelta(int position, int delta) {
		Integer i = positionDeltas.get(position);
		if (i == null) i = 0;
		positionDeltas.put(position, i + delta);
	}
	
	public List<Integer> getLineEndingsTable() {
		return lineEndings;
	}
	
	public long lineColumn(int index) {
		//Possible efficiency improvement: Store in a list the index into the line table with the first line that's over 500, 1000, 1500, ... chars.
		//Or just binary search.
		int oldIdx = 0;
		int line = 0;
		
		for (; line < lineEndings.size(); line++) {
			int pos = lineEndings.get(line);
			if (pos > index) break;
			oldIdx = pos;
		}
		return ((long) line << 32 | index - oldIdx);
	}
	
	/**
	 * Maps a position in the {@code preprocessed} string to the equivalent character in the {@code rawInput}.
	 * 
	 * The difference is caused by decoding backslash-U unicode escapes, for example.
	 */
	int mapPosition(int position) {
		int out = position;
		for (int delta : positionDeltas.headMap(position, true).values()) {
			out += delta;
		}
		return out;
	}
	
	private String preProcess() {
		preprocessed = rawInput;
		this.lineEndings = calculateLineEndings();
		applyBackslashU();
//		applyBraceMatching();
		return preprocessed;
	}
	
	/**
	 * @see http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.3
	 */
	private void applyBackslashU() {
		StringBuilder buffer = new StringBuilder();
		StringBuilder out = new StringBuilder();
		
		int state = 0;
		int idx = 0;
		for (char c : preprocessed.toCharArray()) {
			idx++;
			switch (state) {
			case 0:	//normal mode. Anything that isn't a backslash is not interesting.
				if (c != '\\') {
					out.append(c);
					break;
				}
				
				state = 1;
				break;
			case 1:	//Last character read is an (uneven amount of) backslash.
				if (c != 'u') {
					out.append('\\');
					out.append(c);
					state = 0;
				} else {
					buffer.setLength(0);
					buffer.append("\\u");
					state = 2;
				}
				break;
				//TODO add a test for more backslash-U stuff.
			default:
				//Gobbling hex digits. state-2 is our current position. We want 4.
				buffer.append(c);
				if (c == 'u') {
					//JLS Puzzler: backslash-u-u-u-u-u-u-u-u-u-4hexdigits means the same thing as just 1 u.
					//So, we just keep going as if nothing changed.
				} else if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
					state++;
					if (state == 6) {
						//We've got our 4 hex digits.
						out.append((char)Integer.parseInt(buffer.substring(buffer.length()-4), 0x10));
						int delta = buffer.length() -1;	//Buffer goes away but 1 character appears in its place.
						setPositionDelta(idx - delta, delta);
						buffer.setLength(0);
						//We don't have to check if this char is a backslash and set state to 1; JLS says backslash-u is not recursively applied.
						state = 0;
					}
				} else {
					//Invalid unicode escape.
					problems.add(new ParseProblem(new Position(idx-buffer.length(), idx), "Invalid backslash-u escape: \\u is supposed to be followed by 4 hex digits."));
					out.append(buffer.toString());
					state = 0;
				}
				break;
			}
		}
		
		preprocessed = out.toString();
	}
}
