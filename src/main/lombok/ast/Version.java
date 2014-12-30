/*
 * Copyright (C) 2010-2011 The Project Lombok Authors.
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
package lombok.ast;

/**
 * This class just holds lombok.ast's current version.
 */
public class Version {
	// ** CAREFUL ** - this class must always compile with 0 dependencies (it must not refer to any other sources or libraries).
	// Note: In 'X.Y.Z', if Z is odd, its a snapshot build built from the repository, so many different 0.10.3 versions can exist, for example.
	// Official builds always end in an even number.
	private static final String VERSION = "0.2.2";
	
	private Version() {
		//Prevent instantiation
	}
	
	/**
	 * Prints the version followed by a newline, and exits.
	 */
	public static void main(String[] args) {
		System.out.println(VERSION);
	}
	
	/**
	 * Get the current lombok.ast version.
	 */
	public static String getVersion() {
		return VERSION;
	}
}
