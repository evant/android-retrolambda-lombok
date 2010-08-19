package lombok.ast;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ListAccessorTest {
	@Test
	public void testEmptyRaw() {
		Node n = new Identifier();
		RawListAccessor<Node, Node> raw = ListAccessor.emptyRaw("foo", n);
		emptyRawTest(n, raw);
		emptyStrictTest(n, raw.asStrictAccessor());
	}
	
	@Test
	public void testEmptyStrict() {
		Node n = new Identifier();
		StrictListAccessor<Node,Node> strict = ListAccessor.emptyStrict("foo", n);
		emptyStrictTest(n, strict);
		emptyRawTest(n, strict.asRawAccessor());
	}
	
	private void emptyRawTest(Node owner, RawListAccessor<Node, Node> raw) {
		assertEquals("empty raw size is not 0", 0L, raw.size());
		assertTrue("isEmpty on empty raw is false", raw.isEmpty());
		assertNull("first() on empty raw is not null", raw.first());
		assertNull("last() on empty raw is not null", raw.last());
		assertEquals(owner, raw.up());
		assertEquals(owner, raw.owner());
		assertFalse(raw.iterator().hasNext());
		try {
			raw.iterator().next();
			fail("emptyRaw.iterator().next() did not throw");
		} catch (NoSuchElementException expected) {}
		try {
			raw.addToStart(new Identifier());
			fail("emptyRaw.addToStart did not throw");
		} catch (UnsupportedOperationException expected) {}
	}
	
	private void emptyStrictTest(Node owner, StrictListAccessor<Node, Node> strict) {
		assertEquals("empty strict size is not 0", 0L, strict.size());
		assertTrue("isEmpty on empty strict is false", strict.isEmpty());
		assertNull("first() on empty strict is not null", strict.first());
		assertNull("last() on empty strict is not null", strict.last());
		assertEquals(owner, strict.up());
		assertEquals(owner, strict.owner());
		assertFalse(strict.iterator().hasNext());
		try {
			strict.iterator().next();
			fail("emptyStrict.iterator().next() did not throw");
		} catch (NoSuchElementException expected) {}
		try {
			strict.addToStart(new Identifier());
			fail("emptyStrict.addToStart did not throw");
		} catch (UnsupportedOperationException expected) {}
	}
	
	@Test
	public void testWrap() {
		Modifiers parent = new Modifiers();
		Modifiers p2 = new Modifiers();
		ListAccessor<Identifier, Modifiers> acc = ListAccessor.of(parent, Identifier.class, "identifiers");
		List<?> originalList = acc.backingList();
		Identifier n1 = new Identifier();
		Identifier n2 = new Identifier();
		Identifier n3 = new Identifier();
		Identifier n4 = new Identifier();
		assertTrue(originalList == acc.backingList());
		acc.asStrict().addToStart(n1, n2);
		assertTrue(originalList == acc.backingList());
		ListAccessor<Identifier, Modifiers> acc2 = acc.wrap(p2);
		assertTrue(originalList == acc2.backingList());
		assertEquals(parent, acc.asRaw().up());
		assertEquals(p2, acc2.asRaw().up());
		assertEquals(parent, acc.asStrict().up());
		assertEquals(p2, acc2.asStrict().up());
		assertEquals(parent, acc.asRaw().owner());
		assertEquals(parent, acc2.asRaw().owner());
		assertEquals(parent, acc.asStrict().owner());
		assertEquals(parent, acc2.asStrict().owner());
		assertEquals(p2, acc2.asStrict().addToEnd(n3));
		assertEquals(3, acc.asStrict().size());
		assertTrue(originalList == acc2.backingList());
		assertTrue(originalList == acc.backingList());
		acc.asIterable();
		assertEquals(parent, acc.asStrict().addToEnd(n4));
		assertFalse(originalList == acc.backingList());
		assertFalse(originalList == acc2.backingList());
		assertTrue(acc.backingList() == acc2.backingList());
	}
	
	@Test
	public void testInplaceModification() {
		Modifiers parent = new Modifiers();
		ListAccessor<Identifier, Modifiers> acc = ListAccessor.of(parent, Identifier.class, "identifiers");
		
		Identifier n1 = new Identifier();
		Identifier n2 = new Identifier();
		
		acc.asStrict().addToStart(n1, n2);
		
		Iterator<Identifier> it = acc.asStrict().iterator();
		assertTrue(it.hasNext());
		assertEquals(n1, it.next());
		acc.asStrict().remove(n1);
		assertTrue(it.hasNext());
		assertEquals(1, acc.asStrict().size());
		assertEquals(n2, it.next());
		assertFalse(it.hasNext());
	}
}
