package lombok.ast;

import static org.junit.Assert.*;

import java.util.Arrays;

import lombok.ast.printer.SourcePrinter;
import lombok.ast.printer.StructureFormatter;

import org.junit.Test;

public class TemplateTest {
	private static String literal(String... args) {
		StringBuilder sb = new StringBuilder();
		for (String arg : args) sb.append(arg).append("\n");
		return sb.toString();
	}
	
	@Test
	public void testIdentifier() {
		MethodDeclaration method = Template.parseMethod("public static int testing(int foo) { return 5; }");
		Template<MethodDeclaration> template = Template.of(method);
		template.setStartPosition(10);
		template.replaceIdentifier("testing", "floobargle", new Position(50, 60));
		template.replaceIdentifier("foo", "bar", new Position(80, 90));
		MethodDeclaration finish = template.finish();
		StructureFormatter sf = StructureFormatter.formatterWithPositions();
		finish.accept(new SourcePrinter(sf));
		assertEquals(literal(
"[B MethodDeclaration floobargle (10-90)]",
"    [I Modifiers (10-10)]",
"        [I KeywordModifier public (10-10)]",
"            PROPERTY: modifier = public",
"        [I KeywordModifier static (10-10)]",
"            PROPERTY: modifier = static",
"    returnType: [I TypeReference int (10-10)]",
"        PROPERTY: WildcardKind = NONE",
"        PROPERTY: arrayDimensions = 0",
"        [I TypeReferencePart (10-10)]",
"            [I Identifier int (10-10)]",
"                PROPERTY: name = int",
"    methodName: [I Identifier floobargle (50-60)]",
"        PROPERTY: name = floobargle",
"    parameter: [I VariableDefinition (60-90)]",
"        PROPERTY: varargs = false",
"        [I Modifiers (60-60)]",
"        type: [I TypeReference int (60-60)]",
"            PROPERTY: WildcardKind = NONE",
"            PROPERTY: arrayDimensions = 0",
"            [I TypeReferencePart (60-60)]",
"                [I Identifier int (60-60)]",
"                    PROPERTY: name = int",
"        [I VariableDefinitionEntry (60-90)]",
"            PROPERTY: arrayDimensions = 0",
"            varName: [I Identifier bar (80-90)]",
"                PROPERTY: name = bar",
"    [B Block (90-90)]",
"            [B Return (90-90)]",
"                [I IntegralLiteral 5 (90-90)]",
"                    PROPERTY: value = 5"
				), sf.finish());
	}
	
	@Test
	public void testExpression() {
		Expression expr = Template.parseExpression("5 + (a ? b : 10)");
		Template<Expression> template = Template.of(expr);
		Expression replacement = new FloatingPointLiteral().astDoubleValue(0.5);
		template.setStartPosition(10);
		template.replaceExpression("b", replacement, new Position(20, 30));
		Expression finish = template.finish();
		StructureFormatter sf = StructureFormatter.formatterWithPositions();
		finish.accept(new SourcePrinter(sf));
		assertEquals(literal(
"[I BinaryExpression + (10-30)]",
"    PROPERTY: operator = +",
"    left: [I IntegralLiteral 5 (10-10)]",
"        PROPERTY: value = 5",
"    right: [I InlineIfExpression (10-30)]",
"        condition: [I VariableReference (10-10)]",
"            [I Identifier a (10-10)]",
"                PROPERTY: name = a",
"        ifTrue: [I FloatingPointLiteral 0.5 (20-30)]",
"            PROPERTY: value = 0.5",
"        ifFalse: [I IntegralLiteral 10 (30-30)]",
"            PROPERTY: value = 10"
				), sf.finish());
	}
	
	@Test
	public void testStatements() {
		String expected1 = literal(
"[B ClassDeclaration X (20-80)]",
"    [I Modifiers (20-20)]",
"    typeName: [I Identifier X (20-20)]",
"        PROPERTY: name = X",
"    [B NormalTypeBody (20-80)]",
"            [B InstanceInitializer (20-80)]",
"                [B Block (20-80)]",
"                        [B Synchronized (60-80)]",
"                            lock: [I This (60-80)]",
"                            [B Block (60-80)]");
		
		ClassDeclaration classDecl = (ClassDeclaration) Template.parseMember("class X {{foo:;}}");
		{
			Template<ClassDeclaration> template = Template.of(classDecl);
			Statement x = new Synchronized().astBody(new Block()).astLock(new This());
			Ast.setAllPositions(x, new Position(60, 80));
			template.setStartPosition(20);
			template.replaceStatement("foo", x);
			ClassDeclaration finish = template.finish();
			StructureFormatter sf = StructureFormatter.formatterWithPositions();
			finish.accept(new SourcePrinter(sf));
			assertEquals(expected1, sf.finish());
		}
		
		{
			Template<ClassDeclaration> template = Template.of(classDecl);
			Statement x = new Synchronized().astBody(new Block()).astLock(new This());
			template.setStartPosition(20);
			template.replaceStatement("foo", x, new Position(60, 80));
			ClassDeclaration finish = template.finish();
			StructureFormatter sf = StructureFormatter.formatterWithPositions();
			finish.accept(new SourcePrinter(sf));
			assertEquals(expected1, sf.finish());
		}
		
		String expected2 = literal(
"[B ClassDeclaration X (20-200)]",
"    [I Modifiers (20-20)]",
"    typeName: [I Identifier X (20-20)]",
"        PROPERTY: name = X",
"    [B NormalTypeBody (20-200)]",
"            [B InstanceInitializer (20-200)]",
"                [B Block (20-200)]",
"                        [B Synchronized (40-50)]",
"                            lock: [I This (40-50)]",
"                            [B Block (40-50)]",
"                        [B While (100-200)]",
"                            condition: [I BooleanLiteral (100-200)]",
"                                PROPERTY: value = true",
"                            [B EmptyStatement (100-200)]");
		
		{
			Template<ClassDeclaration> template = Template.of(classDecl);
			Statement x = new Synchronized().astBody(new Block()).astLock(new This());
			Statement y = new While().astCondition(new BooleanLiteral().astValue(true)).astStatement(new EmptyStatement());
			Ast.setAllPositions(x, new Position(40, 50));
			Ast.setAllPositions(y, new Position(100, 200));
			template.setStartPosition(20);
			template.replaceStatement("foo", Arrays.asList(x, y));
			ClassDeclaration finish = template.finish();
			StructureFormatter sf = StructureFormatter.formatterWithPositions();
			finish.accept(new SourcePrinter(sf));
			assertEquals(expected2, sf.finish());
		}
	}
}
