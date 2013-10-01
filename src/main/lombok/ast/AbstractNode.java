/*
 * Copyright (C) 2010-2011 The Project Lombok Authors.
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
package lombok.ast;

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;
import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.TextFormatter;

abstract class AbstractNode implements Node {
	private Position position = Position.UNPLACED;
	@Getter private Node parent;
	private List<Node> danglings;
	private Map<String, Position> conversionPositions;
	private Map<MessageKey, Message> messagesMap;
	private List<Message> messages;
	@Getter @Setter private Object nativeNode;
	@Getter @Setter private PositionFactory positionFactory;
	
	@Override public boolean isGenerated() {
		return position.getGeneratedBy() != null;
	}
	
	@Override public Node getGeneratedBy() {
		return position.getGeneratedBy();
	}
	
	@Override public boolean hasParent() {
		return parent != null;
	}
	
	@Override public List<Node> getChildren() {
		return emptyList();
	}
	
	@Override public boolean replace(Node replacement) throws AstException {
		if (this.getParent() == null) return false;
		return this.parent.replaceChild(this, replacement);
	}
	
	@Override public void unparent() {
		if (this.parent != null) this.parent.detach(this);
	}
	
	/**
	 * Adopts (accepts as direct child) the provided node.
	 * 
	 * @param child The node to adopt
	 * @return The {@code child} parameter for chaining.
	 * @throws IllegalStateException If {@code child} already has a parent (clone or unparent it first).
	 */
	protected AbstractNode adopt(AbstractNode child) throws IllegalStateException {
		child.ensureParentless();
		child.parent = this;
		return child;
	}
	
	/**
	 * Checks if this node is currently parentless.
	 * 
	 * @throws IllegalStateException if I have a parent.
	 */
	protected void ensureParentless() throws IllegalStateException {
		if (parent == null) return;
		throw new IllegalStateException(String.format(
				"I (%s) already have a parent, so you can't add me to something else; clone or unparent me first.",
				this.getClass().getName()));
	}
	
	/**
	 * Disowns a direct child (it will be parentless after this call).
	 * 
	 * @param child Child node to disown
	 * @throws IllegalStateException if {@code child} isn't a direct child of myself.
	 */
	protected void disown(AbstractNode child) throws IllegalStateException {
		ensureParentage(child);
		child.parent = null;
	}
	
	/**
	 * Checks if the provided node is a direct child of this node.
	 * 
	 * @param child This node must be a direct child of myself.
	 * @throws IllegalStateException If {@code child} isn't a direct child of myself.
	 */
	protected void ensureParentage(AbstractNode child) throws IllegalStateException {
		if (child.parent == this) return;
		
		throw new IllegalStateException(String.format(
				"Can't disown child of type %s - it isn't my child (I'm a %s)",
				child.getClass().getName(), this.getClass().getName()));
	}
	
	@Override public Node setPosition(Position position) {
		if (position == null) throw new NullPointerException("position");
		this.position = position;
		return this;
	}

	@Override public Position getPosition() {
		if (position == Position.UNPLACED && positionFactory != null) {
			position = positionFactory.getPosition(this);
		}
		return position;
	}

	@Override public String toString() {
		TextFormatter formatter = new TextFormatter();
		SourcePrinter printer = new SourcePrinter(formatter);
		accept(printer);
		return formatter.finish();
	}
	
	@Override public boolean replaceChild(Node original, Node replacement) {
		// Intentionally left blank - custom implementations are usually terminal nodes that can't have children.
		return false;
	}
	
	@Override public boolean detach(Node child) {
		// Intentionally left blank - custom implementations are usually terminal nodes that can't have children.
		return false;
	}
	
	void addDanglingNode(Node dangling) {
		if (dangling == null) return;
		if (danglings == null) danglings = Lists.newArrayList();
		danglings.add(dangling);
	}
	
	void removeDanglingNode(Node dangling) {
		if (danglings != null) danglings.remove(dangling);
	}
	
	List<Node> getDanglingNodes() {
		return danglings == null ? Collections.<Node>emptyList() : Collections.unmodifiableList(danglings);
	}
	
	void addConversionPositionInfo(String key, Position position) {
		if (conversionPositions == null) conversionPositions = Maps.newHashMap();
		conversionPositions.put(key, position);
	}
	
	Position getConversionPositionInfo(String key) {
		if (conversionPositions == null) return null;
		return conversionPositions.get(key);
	}
	
	public Node addMessage(Message message) {
		if (messagesMap == null) {
			messagesMap = Maps.newHashMap();
			messages = Lists.newArrayList();
		}
		
		if (message.getKey() == null) {
			messages.add(message);
		} else {
			if (!messagesMap.containsKey(message.getKey())) {
				messagesMap.put(message.getKey(), message);
				messages.add(message);
			}
		}
		return this;
	}
	
	public boolean hasMessage(String key) {
		if (messagesMap == null) return false;
		return messagesMap.containsKey(key);
	}
	
	public List<Message> getMessages() {
		return messages == null ? Collections.<Message>emptyList() : Collections.unmodifiableList(messages);
	}
	
	abstract static class WithParens extends AbstractNode implements Expression {
		private List<Position> parensPositions = Lists.newArrayList();
		
		@Override
		public boolean needsParentheses() {
			return false;
		}
		
		@Override
		public List<Position> astParensPositions() {
			return parensPositions;
		}
		
		@Override
		public int getParens() {
			return this.parensPositions.size();
		}
		
		@Override
		public int getIntendedParens() {
			return this.parensPositions.size();
		}
	}
}
