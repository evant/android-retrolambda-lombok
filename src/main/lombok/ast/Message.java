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
package lombok.ast;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Message {
	public enum MessageType {
		ERROR, WARNING;
	}
	
	private final MessageType type;
	private final MessageKey key;
	private final String message;
	
	public static Message warning(MessageKey key, String message) {
		return new Message(MessageType.WARNING, key, message);
	}
	
	public static Message error(MessageKey key, String message) {
		return new Message(MessageType.ERROR, key, message);
	}
	
	public static Message warning(String message) {
		return new Message(MessageType.WARNING, null, message);
	}
	
	public static Message error(String message) {
		return new Message(MessageType.ERROR, null, message);
	}
	
	public boolean isError() {
		return MessageType.ERROR == type;
	}
	
	public boolean isWarning() {
		return MessageType.WARNING == type;
	}
	
	@Override
	public String toString() {
		return key == null ? message : String.format("[%s %s] %s", type, key, message);
	}
}
