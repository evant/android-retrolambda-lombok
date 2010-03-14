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
		IntegralLiteral n = new IntegralLiteral().setIntValue(0);
		assertFalse(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		n.setLongValue(0);
		assertTrue(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testIntMinValue() {
		IntegralLiteral n = new IntegralLiteral().setIntValue(Integer.MIN_VALUE);
		testMinValue(n);
	}
	
	@Test
	public void testLongMinValue() {
		IntegralLiteral n = new IntegralLiteral().setLongValue(Long.MIN_VALUE);
		testMinValue(n);
	}
	
	private void testMinValue(IntegralLiteral n) {
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.OCTAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.HEXADECIMAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.DECIMAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		
		UnaryExpression unary = new UnaryExpression().setOperand(n);
		assertNotNull(n.getErrorReasonForValue());
		unary.setOperator(UnaryOperator.UNARY_MINUS);
		assertNull(n.getErrorReasonForValue());
		n.getParensPositions().add(Position.UNPLACED);
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testIntNegativeValue() {
		IntegralLiteral n = new IntegralLiteral().setIntValue(-1);
		testNegativeValue(n);
	}
	
	@Test
	public void testLongNegativeValue() {
		IntegralLiteral n = new IntegralLiteral().setLongValue(-1L);
		testNegativeValue(n);
	}
	
	private void testNegativeValue(IntegralLiteral n) {
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.OCTAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.HEXADECIMAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.DECIMAL);
		assertFalse("raw value starts with -", n.getRawValue().startsWith("-"));
		assertNotNull(n.getErrorReasonForValue());
		
		new UnaryExpression().setOperator(UnaryOperator.UNARY_MINUS).setOperand(n);
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testToRaw() {
		IntegralLiteral n = new IntegralLiteral().setLongValue(32);
		assertEquals("32L", n.getRawValue());
		n.setLiteralType(LiteralType.HEXADECIMAL);
		assertEquals("0x20L", n.getRawValue());
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.OCTAL);
		assertEquals("040L", n.getRawValue());
		
		n.setIntValue(0);
		n.setLiteralType(LiteralType.DECIMAL);
		assertEquals("0", n.getRawValue());
		n.setLiteralType(LiteralType.HEXADECIMAL);
		assertEquals("0x0", n.getRawValue());
		assertNull(n.getErrorReasonForValue());
		n.setLiteralType(LiteralType.OCTAL);
		assertEquals("00", n.getRawValue());
		
		n.setLiteralType(LiteralType.DECIMAL);
		n.setIntValue(Integer.MIN_VALUE);
		assertEquals("2147483648", n.getRawValue());
		n.setLongValue(Long.MIN_VALUE);
		assertEquals("9223372036854775808L", n.getRawValue());
	}
	
	@Test
	public void testFromRaw() {
		IntegralLiteral n = new IntegralLiteral();
		
		n.setRawValue("0");
		assertEquals(LiteralType.DECIMAL, n.getLiteralType());
		assertEquals(0, n.intValue());
		assertFalse(n.isMarkedAsLong());
		n.setRawValue("00");
		assertEquals(LiteralType.OCTAL, n.getLiteralType());
		assertEquals(0, n.intValue());
		assertFalse(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		n.setRawValue("0x0");
		assertEquals(LiteralType.HEXADECIMAL, n.getLiteralType());
		assertEquals(0, n.intValue());
		assertFalse(n.isMarkedAsLong());
		n.setRawValue("00L");
		assertEquals(LiteralType.OCTAL, n.getLiteralType());
		assertEquals(0, n.longValue());
		assertTrue(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.setRawValue("0x801291FD");
		assertEquals(LiteralType.HEXADECIMAL, n.getLiteralType());
		assertEquals(0x801291FD, n.intValue());
		assertFalse(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.setRawValue("0xAC45005D12DEAAFFL");
		assertEquals(LiteralType.HEXADECIMAL, n.getLiteralType());
		assertEquals(0xAC45005D12DEAAFFL, n.longValue());
		assertTrue(n.isMarkedAsLong());
		assertNull(n.getErrorReasonForValue());
		
		n.setRawValue("0xAC45005D12DEAAFF");
		assertEquals(LiteralType.HEXADECIMAL, n.getLiteralType());
		assertFalse(n.isMarkedAsLong());
		assertNotNull(n.getErrorReasonForValue());
	}
	
	@Test
	public void testOversizedNumbers() {
		IntegralLiteral n = new IntegralLiteral().setRawValue("100000000000");
		assertNotNull(n.getErrorReasonForValue());
		
		n.setRawValue("100000000000L");
		assertNull(n.getErrorReasonForValue());
		
		n.setRawValue("100000000000000000000000000L");
		assertNotNull(n.getErrorReasonForValue());
	}
}
