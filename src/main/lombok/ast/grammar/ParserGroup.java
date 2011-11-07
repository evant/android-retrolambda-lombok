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
package lombok.ast.grammar;

import lombok.Getter;

import org.parboiled.Parboiled;

public class ParserGroup {
	@Getter private final Source source;
	public final BasicsParser basics;
	public final LiteralsParser literals;
	public final TypesParser types;
	public final ExpressionsParser expressions;
	public final StatementsParser statements;
	public final StructuresParser structures;
	
	public ParserGroup(Source source) {
		this.source = source;
		basics = Parboiled.createParser(BasicsParser.class, this);
		literals = Parboiled.createParser(LiteralsParser.class, this);
		types = Parboiled.createParser(TypesParser.class, this);
		expressions = Parboiled.createParser(ExpressionsParser.class, this);
		statements = Parboiled.createParser(StatementsParser.class, this);
		structures = Parboiled.createParser(StructuresParser.class, this);
	}
}
