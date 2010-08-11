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
package lombok.ast.grammar;

import static org.junit.Assert.*;

import lombok.ast.IntegralLiteral;
import lombok.ast.LiteralType;
import lombok.ast.Position;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;

import org.junit.Test;

public class IntegralLiteralTest {
	@Test
	public void testBasics() {
		IntegralLiteral n = new IntegralLiteral().astIntValue(0);
		assertFalse(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		n.astLongValue(0);
		assertTrue(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testIntMinValue() {
		IntegralLiteral n = new IntegralLiteral().astIntValue(Integer.MIN_VALUE);
		testMinValue(n);
	}
	
	@Test
	public void testLongMinValue() {
		IntegralLiteral n = new IntegralLiteral().astLongValue(Long.MIN_VALUE);
		testMinValue(n);
	}
	
	private void testMinValue(IntegralLiteral n) {
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.OCTAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.HEXADECIMAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.DECIMAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		
		UnaryExpression unary = new UnaryExpression().astOperand(n);
		assertNotNull(n.getErrorReasonForValue());
		unary.astOperator(UnaryOperator.UNARY_MINUS);
		assertNull(n.getErrorReasonForValue());
		n.astParensPositions().add(Position.UNPLACED);
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testIntNegativeValue() {
		IntegralLiteral n = new IntegralLiteral().astIntValue(-1);
		testNegativeValue(n);
	}
	
	@Test
	public void testLongNegativeValue() {
		IntegralLiteral n = new IntegralLiteral().astLongValue(-1L);
		testNegativeValue(n);
	}
	
	private void testNegativeValue(IntegralLiteral n) {
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.OCTAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.HEXADECIMAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.DECIMAL);
		assertFalse("raw value starts with -", n.rawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		
		new UnaryExpression().astOperator(UnaryOperator.UNARY_MINUS).astOperand(n);
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testToRaw() {
		IntegralLiteral n = new IntegralLiteral().astLongValue(32);
		assertEquals("32L", n.rawValue());
		n.astLiteralType(LiteralType.HEXADECIMAL);
		assertEquals("0x20L", n.rawValue());
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.OCTAL);
		assertEquals("040L", n.rawValue());
		
		n.astIntValue(0);
		n.astLiteralType(LiteralType.DECIMAL);
		assertEquals("0", n.rawValue());
		n.astLiteralType(LiteralType.HEXADECIMAL);
		assertEquals("0x0", n.rawValue());
		assertNull(n.getErrorReasonForValue());
		n.astLiteralType(LiteralType.OCTAL);
		assertEquals("00", n.rawValue());
		
		n.astLiteralType(LiteralType.DECIMAL);
		n.astIntValue(Integer.MIN_VALUE);
		assertEquals("2147483648", n.rawValue());
		n.astLongValue(Long.MIN_VALUE);
		assertEquals("9223372036854775808L", n.rawValue());
	}
	
	@Test
	public void testFromRaw() {
		IntegralLiteral n = new IntegralLiteral();
		
		n.rawValue("0");
		assertEquals(LiteralType.DECIMAL, n.astLiteralType());
		assertEquals(0, n.astIntValue());
		assertFalse(n.astMarkedAsLong());
		n.rawValue("00");
		assertEquals(LiteralType.OCTAL, n.astLiteralType());
		assertEquals(0, n.astIntValue());
		assertFalse(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		n.rawValue("0x0");
		assertEquals(LiteralType.HEXADECIMAL, n.astLiteralType());
		assertEquals(0, n.astIntValue());
		assertFalse(n.astMarkedAsLong());
		n.rawValue("00L");
		assertEquals(LiteralType.OCTAL, n.astLiteralType());
		assertEquals(0, n.astLongValue());
		assertTrue(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.rawValue("0x801291FD");
		assertEquals(LiteralType.HEXADECIMAL, n.astLiteralType());
		assertEquals(0x801291FD, n.astIntValue());
		assertFalse(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.rawValue("0xAC45005D12DEAAFFL");
		assertEquals(LiteralType.HEXADECIMAL, n.astLiteralType());
		assertEquals(0xAC45005D12DEAAFFL, n.astLongValue());
		assertTrue(n.astMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.rawValue("0xAC45005D12DEAAFF");
		assertEquals(LiteralType.HEXADECIMAL, n.astLiteralType());
		assertFalse(n.astMarkedAsLong());
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testOversizedNumbers() {
		IntegralLiteral n = new IntegralLiteral().rawValue("100000000000");
		assertNotNull(n.getErrorReasonForValue());
		
		n.rawValue("100000000000L");
		assertNull(n.getErrorReasonForValue());
		
		n.rawValue("100000000000000000000000000L");
		assertNotNull(n.getErrorReasonForValue());
	}
}
