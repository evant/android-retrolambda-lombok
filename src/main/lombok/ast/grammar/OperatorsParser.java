package lombok.ast.grammar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import lombok.Lombok;
import lombok.ast.Node;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

public class OperatorsParser extends BaseParser<Node, OperatorsActions>{
	private final BasicsParser basics = Parboiled.createParser(BasicsParser.class);
	private final LiteralsParser literals = Parboiled.createParser(LiteralsParser.class);
	
	public OperatorsParser() {
		super(Parboiled.createActions(OperatorsActions.class));
	}
	
	/* operators:
	 *      =       >    <    !       ~       ?       :
        ==      <=   >=   !=      &&      ||      ++      --
        +       -       *       /       &   |       ^       %       <<        >>        >>>
        +=      -=      *=      /=      &=  |=      ^=      %=      <<=       >>=       >>>=
        
        as well as:
        cast
        paren-grouping
        instanceof
	 */
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private static @interface PrecedencePlayer {
		int value();
	}
	
	@PrecedencePlayer(0)
	public Rule primaryExpression() {
		return literals.anyLiteral();
	}
	
	public Rule anyExpression() {
		return firstOf(
				assignmentOperation(),
				inlineIfOperation(),
				conditionalOrOperation(),
				conditionalXorOperation(),
				conditionalAndOperation(),
				bitwiseOrOperation(),
				bitwiseXorOperation(),
				bitwiseAndOperation(),
				equalityOperation(),
				relationalOperation(),
				shiftOperation(),
				additiveOperation(),
				multiplicativeOperation(),
				primaryExpression());
	}
	
	@PrecedencePlayer(300)
	public Rule multiplicativeOperation() {
		return forBinaryOperation(firstOf(ch('*'), solitarySymbol('/'), ch('%')), true);
	}
	
	@PrecedencePlayer(400)
	public Rule additiveOperation() {
		return forBinaryOperation(firstOf(solitarySymbol('+'), solitarySymbol('-')), true);
	}
	
	@PrecedencePlayer(500)
	public Rule shiftOperation() {
		return forBinaryOperation(firstOf(string(">>>"), string("<<<"), string("<<"), string(">>")), true);
	}
	
	@PrecedencePlayer(600)
	public Rule relationalOperation() {
		return forBinaryOperation(firstOf(string("<="), string(">="), solitarySymbol('<'), solitarySymbol('>'), sequence(string("instanceof"), basics.testLexBreak())), true);
	}
	
	@PrecedencePlayer(700)
	public Rule equalityOperation() {
		return forBinaryOperation(firstOf(string("==="), string("!=="), string("=="), string("!=")), true);
	}
	
	@PrecedencePlayer(800)
	public Rule bitwiseAndOperation() {
		return forBinaryOperation(solitarySymbol('&'), true);
	}
	
	@PrecedencePlayer(900)
	public Rule bitwiseXorOperation() {
		return forBinaryOperation(solitarySymbol('^'), true);
	}
	
	@PrecedencePlayer(1000)
	public Rule bitwiseOrOperation() {
		return forBinaryOperation(solitarySymbol('|'), true);
	}
	
	@PrecedencePlayer(1100)
	public Rule conditionalAndOperation() {
		return forBinaryOperation(string("&&"), true);
	}
	
	@PrecedencePlayer(1150)
	public Rule conditionalXorOperation() {
		return forBinaryOperation(string("^^"), true);
	}
	
	@PrecedencePlayer(1200)
	public Rule conditionalOrOperation() {
		return forBinaryOperation(string("||"), true);
	}
	
	@PrecedencePlayer(1300)
	public Rule inlineIfOperation() {
		return sequence(
				higher("inlineIfOperation").label("head"),
				basics.optWS(),
				oneOrMore(
						sequence(
								sequence(ch('?'), testNot(firstOf(ch('.'), ch(':'), ch('?')))).label("operator1"),
								basics.optWS(),
								higher("inlineIfOperation").label("tail1"),
								basics.optWS(),
								ch(':').label("operator2"),
								basics.optWS(),
								higher("inlineIfOperation").label("tail2")
								)),
				SET(actions.createInlineIfOperation(VALUE("head"),
						TEXTS("oneOrMore/sequence/operator1"), TEXTS("oneOrMore/sequence/operator2"),
						VALUES("oneOrMore/sequence/tail1"), VALUES("oneOrMore/sequence/tail2"))),
				basics.optWS());
	}
	
	@PrecedencePlayer(1400)
	public Rule assignmentOperation() {
		return forBinaryOperation(firstOf(
				solitarySymbol('='),
				string("*="), string("/="), string("+="), string("-="), string("%="),
				string(">>>="), string("<<<="), string("<<="), string(">>="),
				string("&="), string("^="), string("|="),
				string("&&="), string("^^="), string("||=")), false);
	}
	
	private List<Method> precedenceList;
	
	private void initReflectionMagic() {
		if (precedenceList != null) return;
		
		TreeMap<Integer, Method> precedenceOperations = new TreeMap<Integer, Method>();
		
		for (Method m : OperatorsParser.class.getDeclaredMethods()) {
			PrecedencePlayer pp = m.getAnnotation(PrecedencePlayer.class);
			if (pp == null) continue;
			
			int order = pp.value();
			
			if (precedenceOperations.get(order) != null) throw new IllegalStateException(String.format(
					"You have 2 @PrecedencePlayer methods that have the same value: %s and %s are both %d",
					m.getName(), precedenceOperations.get(order).getName(), order));
			
			precedenceOperations.put(order, m);
		}
		
		this.precedenceList = new ArrayList<Method>(precedenceOperations.values());
	}
	
	private Rule forBinaryOperation(Rule operator, boolean leftAssociative) {
		StackTraceElement callerFrame = Thread.currentThread().getStackTrace()[2];
		String methodName = callerFrame.getMethodName();
		
		return sequence(
				higher(methodName).label("head"),
				basics.optWS(),
				oneOrMore(
						sequence(
								operator.label("operator"),
								basics.optWS(),
								higher(methodName).label("tail"),
								basics.optWS())),
				SET(leftAssociative ?
						actions.createLeftAssociativeBinaryOperation(VALUE("head"), TEXTS("oneOrMore/sequence/operator"), VALUES("oneOrMore/sequence/tail")) :
						actions.createRightAssociativeBinaryOperation(VALUE("head"), TEXTS("oneOrMore/sequence/operator"), VALUES("oneOrMore/sequence/tail"))
						),
				basics.optWS());
	}
	
	private Rule higher(String methodName) {
		initReflectionMagic();
		
		List<Rule> firstOfParts = new ArrayList<Rule>();
		
		for (Method m : precedenceList) {
			if (m.getName().equals(methodName)) {
				break;
			}
			firstOfParts.add(invokeRuleMethod(m));
		}
		
		Collections.reverse(firstOfParts);
		
		switch (firstOfParts.size()) {
		case 0:
			return empty();
		case 1:
			return firstOfParts.get(0);
		default:
			Object[] extras = new Rule[firstOfParts.size() -2];
			for (int i = 2; i < firstOfParts.size(); i++) {
				extras[i-2] = firstOfParts.get(i);
			}
			return firstOf(firstOfParts.get(0), firstOfParts.get(1), extras);
		}
	}
	
	private Rule invokeRuleMethod(Method m) {
		try {
			return (Rule)m.invoke(this);
		} catch (Exception e) {
			throw Lombok.sneakyThrow(e);
		}
	}
	
	private Rule solitarySymbol(char c) {
		return sequence(ch(c), testNot(ch(c)));
	}
}
