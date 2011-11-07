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
package lombok.ast.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method or a class to indicate a syntactic check.
 * 
 *  There are two options:<ul>
 *  <li>mark a static method that takes 2 arguments; the first a concrete type that subclasses {@link lombok.ast.Node}, the second a {@code List<SyntaxProblem>}.
 *  <li>mark a class containing non-static methods that take 1 argument, a concrete type that subclasses {@link lombok.ast.Node}, which has a constructor that
 *  takes one parameter, a {@code List<SyntaxProblem>}.</ul>
 *  <p>
 *  Each method should analyse the node (but not its children), and if there are any syntactic problems with it, create {@link lombok.ast.SyntaxProblem} objects
 *  and add them to the list.
 *  <p>
 *  Chaining to children has already been taken care of, as well as the expected type of children, so there's no need to e.g. test if the {@code condition} part of
 *  an {@code If} node is in fact an instance of {@link lombok.at.Expression}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SyntaxCheck {
}
