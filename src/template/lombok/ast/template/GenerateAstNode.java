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
package lombok.ast.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells the templating system to take the annotated type and use it to generate a {@link lombok.ast.AbstractNode} subclass.
 * <p>
 *     See the other annotations in this package for the various ways you can configure the way the class is generated.
 * </p>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateAstNode {
	/**
	 * By default AST nodes extend {@link lombok.ast.AbstractNode}, but you can pick a more specific subclass if you'd like.
	 */
	Class<?> extending() default Object.class;
	
	/**
	 * List any interfaces this node is to implement.
	 */
	Class<?>[] implementing() default {};
	
	/**
	 * A nifty feature that treats members in any listed type as virtually part of this node as well. You can include fields as well as
	 * methods annotated with {@code @CopyMethod} in the mixed in types. Types in the original class (the one annotated with this {@code @GenerateAstNode}
	 * annotation) win out over duplicates - like overriding.
	 */
	Class<?>[] mixin() default {};
}
