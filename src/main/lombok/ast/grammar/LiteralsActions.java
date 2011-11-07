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

import lombok.ast.BooleanLiteral;
import lombok.ast.CharLiteral;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.IntegralLiteral;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.StringLiteral;

public class LiteralsActions extends SourceActions {
	public LiteralsActions(Source source) {
		super(source);
	}
	
	public Node createNullLiteral(String raw) {
		return posify(new NullLiteral().rawValue(raw));
	}
	
	public Node createStringLiteral(String raw) {
		return posify(new StringLiteral().rawValue(raw));
	}
	
	public Node createCharLiteral(String raw) {
		return posify(new CharLiteral().rawValue(raw));
	}
	
	public Node createBooleanLiteral(String raw) {
		return posify(new BooleanLiteral().rawValue(raw));
	}
	
	public Node createNumberLiteral(String raw) {
		if (raw == null) return posify(new IntegralLiteral());
		
		String v = raw.trim().toLowerCase();
		
		if (v.startsWith("0x")) {
			if (v.contains("p")) return posify(new FloatingPointLiteral().rawValue(raw));
			return posify(new IntegralLiteral().rawValue(raw));
		}
		
		if (v.contains(".") || v.endsWith("d") || v.endsWith("f") || v.contains("e")) {
			return posify(new FloatingPointLiteral().rawValue(raw));
		}
		else return posify(new IntegralLiteral().rawValue(raw));
	}
}
