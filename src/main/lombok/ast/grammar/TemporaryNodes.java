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
package lombok.ast.grammar;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import lombok.ast.AstVisitor;
import lombok.ast.Message;
import lombok.ast.Node;
import lombok.ast.Position;
import lombok.ast.PositionFactory;

abstract class TemporaryNode implements Node {
	private Position position = Position.UNPLACED;
	
	@Override public void accept(AstVisitor visitor) {
		if (!visitor.visitParseArtefact(this)) visitor.endVisit(this);
	}
	
	@Override public List<Node> getChildren() {
		return Collections.emptyList();
	}
	
	@Override public boolean detach(Node child) {
		return false;
	}
	
	@Override public boolean replaceChild(Node original, Node replacement) {
		return false;
	}
	
	@Override public boolean replace(Node replacement) {
		return false;
	}
	
	@Override public void unparent() {
	}
	
	@Override public Node addMessage(Message message) {
		return null;
	}
	
	@Override public Node copy() {
		return null;
	}
	
	@Override public List<Message> getMessages() {
		return Collections.emptyList();
	}
	
	@Override public boolean hasMessage(String key) {
		return false;
	}
	
	static class MethodParameters extends TemporaryNode {
		List<Node> parameters = Lists.newArrayList();
	}
	
	static class MethodArguments extends TemporaryNode {
		List<Node> arguments = Lists.newArrayList();
	}
	
	static class TypeArguments extends TemporaryNode {
		List<Node> arguments = Lists.newArrayList();
	}
	
	static class OrphanedTypeVariables extends TemporaryNode {
		List<Node> variables = Lists.newArrayList();
	}
	
	static class StatementExpressionList extends TemporaryNode {
		List<Node> expressions = Lists.newArrayList();
	}
	
	static class ExtendsClause extends TemporaryNode {
		List<Node> superTypes = Lists.newArrayList();
	}
	
	static class ImplementsClause extends TemporaryNode {
		List<Node> superInterfaces = Lists.newArrayList();
	}
	
	@Override public Node getGeneratedBy() {
		return null;
	}
	
	@Override public boolean hasParent() {
		return false;
	}
	
	@Override public boolean isGenerated() {
		return false;
	}
	
	@Override public Node setPosition(Position position) {
		this.position = position;
		return this;
	}
	
	@Override public Position getPosition() {
		return position;
	}
	
	@Override public Node getParent() {
		return null;
	}
	
	@Override
	public Object getNativeNode() {
		return null;
	}
	
	@Override
	public void setNativeNode(Object node) {
	}
	
	@Override
	public PositionFactory getPositionFactory() {
		return null;
	}
	
	@Override
	public void setPositionFactory(PositionFactory positionFactory) {
	}
}
