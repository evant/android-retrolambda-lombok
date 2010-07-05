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
package lombok.ast;

import java.util.List;

public interface Node {
	/**
	 * Returns {@code true} if this node is generated (not actually present in the source).
	 */
	boolean isGenerated();
	
	/**
	 * Returns the node that is responsible for generating this node. Returns {@code null} if this node is not generated.
	 */
	Node getGeneratedBy();
	
	boolean hasParent();
	
	List<Node> getChildren();
	
	/**
	 * If the provided <em>child</em> node is a child of this node, the child/parent link will be deleted. The child's parentage is set to unparented,
	 * and whichever property in this node is linking to the child is cleared. If <em>child</em> is not a child of this node, nothing happens.
	 * 
	 * @see #unparent()
	 */
	void detach(Node child);
	
	/**
	 * Severs the child/parent link between this node and its parent. This node's parentage will be set to unparented, and whichever property
	 * in the parent node is linking to this node is cleared. If this node is already unparented nothing happens.
	 * 
	 * @see #detach(Node)
	 */
	void unparent();
	
	Node setPosition(Position position);
	
	void accept(AstVisitor visitor);
	
	Node copy();
	
	String toString();
	
	Node getParent();
	
	Position getPosition();
	
	void addDanglingNode(Node dangler);
	
	List<Node> getDanglingNodes();
	
	void removeDanglingNode(Node dangler);
	
	Node addMessage(Message message);
	
	boolean hasMessage(String key);
	
	List<Message> getMessages();
}
