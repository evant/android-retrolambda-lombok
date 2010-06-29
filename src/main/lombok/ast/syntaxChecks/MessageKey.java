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
package lombok.ast.syntaxChecks;

public enum MessageKey implements lombok.ast.MessageKey {
	/** Signals an identifier node has zero characters in it. */
	IDENTIFIER_EMPTY("empty"),
	
	/** Signals an identifier node is not a valid java identifier. */
	IDENTIFIER_INVALID("invalid"),
	
	/** Signals that only one variable name is legal in a given definition, but there is more than one. */
	VARIABLEDEFINITION_ONLY_ONE("only one"),
	
	/** Signals some variable definition has an initializer but those aren't allowed there. */
	VARIABLEDEFINITIONENTRY_INITIALIZER_NOT_ALLOWED("initializer not allowed"),
	
	MODIFIERS_DUPLICATE_KEYWORD("duplicate keyword"),
	
	MODIFIERS_STATIC_CHAIN("static chain"),
	
	INITIALIZER_STATIC_IN_NON_STATIC_TYPE("static initializer in non static type"),
	
	MODIFIERS_EMPTY_MODIFIER("empty modifier"),
	
	MODIFIERS_UNKNOWN_MODIFIER("unknown modifier"),
	
	MODIFIERS_MODIFIER_NOT_ALLOWED("modifier not allowed"),
	
	;
	
	private final String key;
	
	MessageKey(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
	
	@Override
	public String toString() {
		return key;
	}
}
