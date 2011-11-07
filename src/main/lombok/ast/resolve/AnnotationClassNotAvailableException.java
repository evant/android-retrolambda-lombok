/*
 * Copyright (C) 2009-2010 The Project Lombok Authors.
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
package lombok.ast.resolve;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ast.Node;
import lombok.ast.ResolutionException;

/**
 * This exception can be thrown when a {@code Class} returning method is called on an
 * annotation instance from {@link Resolver#toAnnotationInstance(Class, lombok.ast.Annotation)}.
 * 
 * You should use {@link Resolver#getAnnotationClassAsString(java.lang.annotation.Annotation, String)} instead.
 */
public class AnnotationClassNotAvailableException extends ResolutionException {
	@Getter private final String className;
	@Getter @Setter(AccessLevel.PACKAGE) private List<String> classNames;
	
	public AnnotationClassNotAvailableException(Node problemNode, String className) {
		super(problemNode, "Class not available: " + className + " - use Resolver.getAnnotationClassAsString instead.");
		this.className = className;
	}
}
