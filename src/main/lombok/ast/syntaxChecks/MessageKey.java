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
package lombok.ast.syntaxChecks;

public enum MessageKey implements lombok.ast.MessageKey {
	/** Signals an identifier node has zero characters in it. */
	IDENTIFIER_EMPTY("empty"),
	
	/** Signals an identifier node is not a valid java identifier. */
	IDENTIFIER_INVALID("invalid"),
	
	/** Signals that only one variable name is legal in a given definition, but there is more than one. */
	VARIABLEDEFINITION_ONLY_ONE("only one"),
	
	VARIABLEDEFINITION_EMPTY("empty"),
	
	VARIABLEDEFINITION_VARARGS_NOT_LEGAL_HERE("not legal here"),
	
	/** Signals some variable definition has an initializer but those aren't allowed there. */
	VARIABLEDEFINITIONENTRY_INITIALIZER_NOT_ALLOWED("initializer not allowed"),
	
	VARIABLEDEFINITIONENTRY_EXTENDED_DIMENSIONS_NOT_LEGAL("extended dimensions not legal"),
	
	DECLARATION_NOT_ALLOWED("Declarations are not allowed here"),
	
	MODIFIERS_DUPLICATE_KEYWORD("duplicate keyword"),
	
	MODIFIERS_STATIC_CHAIN("static chain"),
	
	MODIFIERS_ABSTRACT_NOT_ALLOWED("Abstract is not allowed here"),
	
	INITIALIZER_STATIC_IN_NON_STATIC_TYPE("static initializer in non static type"),
	
	MODIFIERS_EMPTY_MODIFIER("empty modifier"),
	
	MODIFIERS_UNKNOWN_MODIFIER("unknown modifier"),
	
	MODIFIERS_MODIFIER_NOT_ALLOWED("modifier not allowed"),
	
	MODIFIERS_MODIFIER_CONFLICT("modifier conflicts with another modifier"),
	
	TRY_LONE_TRY("try without catch or finally"),
	
	STATEMENT_ONLY_LEGAL_IN_SWITCH("statement only legal inside switch"),
	
	SWITCH_DOES_NOT_START_WITH_CASE("switch does not start with case or default"),
	
	INITIALIZERS_INITIALIZER_MUST_COMPLETE_NORMALLY("initializer blocks must complete normally"),
	
	CONSTRUCTOR_INVOCATION_NOT_LEGAL_HERE("constructor invocation not legal here"),
	
	TYPEARGUMENT_PRIMITIVE_NOT_ALLOWED("primitive not allowed"),
	
	TYPEVARIABLE_PRIMITIVE_NOT_ALLOWED("primitive not allowed"),
	
	TYPEREFERENCE_VOID_NOT_ALLOWED("void not allowed"),
	
	STATEMENT_UNREACHABLE("unreachable"),
	
	NODE_MISSING_MANDATORY_CHILD("missing child node"),
	
	NODE_CHILD_TYPE_INCORRECT("child type incorrect"),
	
	PARSEARTEFACT("parse artefact"),
	
	TERMINAL_MISSING("missing"),
	
	TERMINAL_INVALID("invalid"),
	
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
