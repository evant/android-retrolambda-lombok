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

import static lombok.ast.syntaxChecks.MessageKey.*;
import static lombok.ast.Message.*;

import lombok.ast.TypeReference;
import lombok.ast.template.SyntaxCheck;

@SyntaxCheck
public class TypeChecks {
	public void checkNoPrimitivesInGenerics(TypeReference node) {
		if (!node.isPrimitive()) return;
		
		if (node.upIfTypeArgumentToTypeReferencePart() != null) {
			node.addMessage(error(TYPEARGUMENT_PRIMITIVE_NOT_ALLOWED, "Primitive types aren't allowed in type arguments."));
			return;
		}
		
		if (node.upIfTypeVariableBoundToTypeVariable() != null) {
			node.addMessage(error(TYPEVARIABLE_PRIMITIVE_NOT_ALLOWED, "Primitive types aren't allowed in type variable bounds."));
			return;
		}
	}
	
	public void checkVoidNotLegalJustAboutEverywhere(TypeReference node) {
		if (!node.isVoid()) return;
		if (node.astArrayDimensions() > 0) {
			node.addMessage(error(TYPEREFERENCE_VOID_NOT_ALLOWED, "Array of void type is not legal."));
			return;
		}
		
		if (node.upIfReturnTypeToMethodDeclaration() != null) return;
		
		if (node.upToClassLiteral() != null) return;
		
		node.addMessage(error(TYPEREFERENCE_VOID_NOT_ALLOWED, "The void type is not legal here."));
	}
}
