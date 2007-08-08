/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.internal.terminal.model;

import junit.framework.TestCase;

import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.ITerminalTextHistory;
import org.eclipse.tm.terminal.model.Style;
import org.eclipse.tm.terminal.model.StyleColor;

public class TerminalTextDataTest extends TestCase {

	public void testGetWidth() {
		TerminalTextData term=new TerminalTextData();
		assertEquals(0, term.getWidth());
		term.setDimensions(10, term.getHeight());
		assertEquals(10, term.getWidth());
		term.setDimensions(0, term.getHeight());
		assertEquals(0, term.getWidth());
	}

	public void testGetHeight() {
		TerminalTextData term=new TerminalTextData();
		assertEquals(0, term.getHeight());
		term.setDimensions(term.getWidth(), 10);
		assertEquals(10, term.getHeight());
		term.setDimensions(term.getWidth(), 0);
		assertEquals(0, term.getHeight());
	}

	public void testSetDimensions() {
		TerminalTextData term=new TerminalTextData();
		assertEquals(0, term.getHeight());
		term.setDimensions(5, 10);
		assertEquals(5, term.getWidth());
		assertEquals(10, term.getHeight());
		term.setDimensions(10, 5);
		assertEquals(10, term.getWidth());
		assertEquals(5, term.getHeight());
		term.setDimensions(0, 15);
		assertEquals(0, term.getWidth());
		assertEquals(15, term.getHeight());
		term.setDimensions(12, 0);
		assertEquals(12, term.getWidth());
		assertEquals(0, term.getHeight());
		term.setDimensions(0, 0);
		assertEquals(0, term.getWidth());
		assertEquals(0, term.getHeight());
	}
	public void testResize() {
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(5, 3);
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE";
		TerminalTextTestHelper.fill(term,0,0,s);
		assertEquals(s, term.textToString());
		term.setDimensions(4, 3);
		assertEquals(
				 "1234\n" +
				 "abcd\n" +
				 "ABCD", term.textToString());
		// the columns should be restored
		term.setDimensions(5, 3);
		assertEquals(
				 "12345\n" +
				 "abcde\n" +
				 "ABCDE", term.textToString());
		term.setDimensions(6, 3);
		assertEquals(
				 "12345\000\n" +
				 "abcde\000\n" +
				 "ABCDE\000", term.textToString());
		term.setChar(5, 0, 'x', null);
		term.setChar(5, 1, 'y', null);
		term.setChar(5, 2, 'z', null);
		assertEquals(
				 "12345x\n" +
				 "abcdey\n" +
				 "ABCDEz", term.textToString());
		term.setDimensions(4, 2);
		assertEquals(
				 "1234\n" +
				 "abcd", term.textToString());
	}

	public void testResizeFailure() {
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(5, 3);
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE";
		TerminalTextTestHelper.fill(term,0,0,s);
		assertEquals(s, term.textToString());
		try {
			term.setDimensions(4, -3);
			fail();
		} catch (RuntimeException e) {
			// OK
		}
		assertEquals(5, term.getWidth());
		assertEquals(3, term.getHeight());
		assertEquals(s, term.textToString());
	}

	public void testGetLineSegments() {
		Style s1=getDefaultStyle();
		Style s2=s1.setBold(true);
		Style s3=s1.setUnderline(true);
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(8, 8);
		LineSegment[] segments;
		
		term.setChars(0, 0,"0123".toCharArray(), s1);
		term.setChars(4, 0,"abcd".toCharArray(), null);
		segments=term.getLineSegments(0, 0, term.getWidth());
		assertEquals(2, segments.length);
		assertSegment(0, "0123", s1, segments[0]);
		assertSegment(4, "abcd", null, segments[1]);
		
		
		segments=term.getLineSegments(4, 0, term.getWidth()-4);
		assertEquals(1, segments.length);
		assertSegment(4, "abcd", null, segments[0]);
		
		segments=term.getLineSegments(3, 0, 2);
		assertEquals(2, segments.length);
		assertSegment(3, "3", s1, segments[0]);
		assertSegment(4, "a", null, segments[1]);
		
		segments=term.getLineSegments(7, 0, 1);
		assertEquals(1, segments.length);
		assertSegment(7, "d", null, segments[0]);
		
		segments=term.getLineSegments(0, 0, 1);
		assertEquals(1, segments.length);
		assertSegment(0, "0", s1, segments[0]);
		
		// line 1
		term.setChars(0, 1,"x".toCharArray(), s1);
		term.setChars(1, 1,"y".toCharArray(), s2);
		term.setChars(2, 1,"z".toCharArray(), s3);
		
		segments=term.getLineSegments(0, 1, term.getWidth());
		assertEquals(4, segments.length);
		assertSegment(0, "x", s1, segments[0]);
		assertSegment(1, "y", s2, segments[1]);
		assertSegment(2, "z", s3, segments[2]);
		assertSegment(3, "\000\000\000\000\000", null, segments[3]);
		
		// line 2
		term.setChars(4, 2,"klm".toCharArray(), s1);		
		segments=term.getLineSegments(0, 2, term.getWidth());
		assertEquals(3, segments.length);
		assertSegment(0, "\000\000\000\000", null, segments[0]);
		assertSegment(4, "klm", s1, segments[1]);
		assertSegment(7, "\000", null, segments[2]);
		
		// line 3
		segments=term.getLineSegments(0, 3, term.getWidth());
		assertEquals(1, segments.length);
		assertSegment(0, "\000\000\000\000\000\000\000\000", null, segments[0]);
		
	}
	void assertSegment(int col,String text, Style style,LineSegment segment) {
		assertEquals(col, segment.getColumn());
		assertEquals(text, segment.getText());
		assertEquals(style, segment.getStyle());
		
	}
	public void testGetChar() {
		String s="12345\n" +
		 "abcde\n" +
		 "ABCDE";
		TerminalTextData term=new TerminalTextData();
		TerminalTextTestHelper.fill(term, s);
		assertEquals('1', term.getChar(0,0));
		assertEquals('2', term.getChar(1,0));
		assertEquals('3', term.getChar(2,0));
		assertEquals('4', term.getChar(3,0));
		assertEquals('5', term.getChar(4,0));
		assertEquals('a', term.getChar(0,1));
		assertEquals('b', term.getChar(1,1));
		assertEquals('c', term.getChar(2,1));
		assertEquals('d', term.getChar(3,1));
		assertEquals('e', term.getChar(4,1));
		assertEquals('A', term.getChar(0,2));
		assertEquals('B', term.getChar(1,2));
		assertEquals('C', term.getChar(2,2));
		assertEquals('D', term.getChar(3,2));
		assertEquals('E', term.getChar(4,2));
		try {
			term.getChar(-1,0);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			term.getChar(-1,-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			term.getChar(0,-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			term.getChar(5,0);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			term.getChar(5,3);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			term.getChar(0,3);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGetStyle() {
		TerminalTextData term=new TerminalTextData();
		Style style=getDefaultStyle();
		term.setDimensions(3, 6);
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				term.setChar(x, y, c, style.setForground(StyleColor.getStyleColor(""+c)));
			}
		}
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				assertSame(style.setForground(StyleColor.getStyleColor(""+c)), term.getStyle(x, y));
			}
		}
		
	}

	private Style getDefaultStyle() {
		return Style.getStyle(StyleColor.getStyleColor("fg"), StyleColor.getStyleColor("bg"), false, false, false, false);
	}

	public void testSetChar() {
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(3, 6);
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				term.setChar(x, y, (char)('a'+x+y), null);
			}
		}
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				assertEquals(c, term.getChar(x,y));
			}
		}
		assertEquals(
				  "abc\n"
				+ "bcd\n"
				+ "cde\n"
				+ "def\n"
				+ "efg\n"
				+ "fgh", term.textToString());
	}
	public void testSetChars() {
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(3, 6);
		for (int y = 0; y < term.getHeight(); y++) {
			char[] chars=new char[term.getWidth()];
			for (int x = 0; x < term.getWidth(); x++) {
				chars[x]=(char)('a'+x+y);
			}
			term.setChars(0, y, chars, null);
		}
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				assertEquals(c, term.getChar(x,y));
			}
		}
		assertEquals(
				  "abc\n"
				+ "bcd\n"
				+ "cde\n"
				+ "def\n"
				+ "efg\n"
				+ "fgh", term.textToString());
	
		term.setChars(1, 3, new char[]{'1','2'}, null);
		assertEquals(
				  "abc\n"
				+ "bcd\n"
				+ "cde\n"
				+ "d12\n"
				+ "efg\n"
				+ "fgh", term.textToString());
		// check if chars are correctly chopped
		term.setChars(1, 4, new char[]{'1','2','3','4','5'}, null);
		assertEquals(
				  "abc\n"
				+ "bcd\n"
				+ "cde\n"
				+ "d12\n"
				+ "e12\n"
				+ "fgh", term.textToString());
	
	}
	public void testSetCharsLen() {
		TerminalTextData term=new TerminalTextData();
		String s= "ZYXWVU\n"
				+ "abcdef\n"
				+ "ABCDEF";
		TerminalTextTestHelper.fill(term, s);
		char[] chars=new char[]{'1','2','3','4','5','6','7','8'};
		term.setChars(0, 1, chars, 0, 6,null);
		assertEquals(
				  "ZYXWVU\n"
				+ "123456\n"
				+ "ABCDEF", term.textToString());

		TerminalTextTestHelper.fill(term, s);
		term.setChars(0, 1, chars, 0, 5, null);
		assertEquals("ZYXWVU\n"
				+ "12345f\n"
				+ "ABCDEF", term.textToString());

		TerminalTextTestHelper.fill(term, s);
		term.setChars(0, 1, chars, 1, 5, null);
		assertEquals("ZYXWVU\n"
				+ "23456f\n"
				+ "ABCDEF", term.textToString());

		TerminalTextTestHelper.fill(term, s);
		term.setChars(1, 1, chars, 1, 4, null);
		assertEquals("ZYXWVU\n"
				+ "a2345f\n"
				+ "ABCDEF", term.textToString());


		
		TerminalTextTestHelper.fill(term, s);
		term.setChars(2, 1, chars, 3, 4, null);
		assertEquals("ZYXWVU\n"
				+ "ab4567\n"
				+ "ABCDEF", term.textToString());

		TerminalTextTestHelper.fill(term, s);
		try {
			term.setChars(0, 1, chars, 7, 10, null);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
		TerminalTextTestHelper.fill(term, s);
		try {
			term.setChars(-1, 1, chars, 0, 2, null);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
		try {
			term.setChars(1, -1, chars, 0, 2, null);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
		// this one will not fail,because we make sure not to write off bound...
		term.setChars(10, 1, chars, 0, 2, null);
		try {
			term.setChars(1, 10, chars, 0, 2, null);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {}
//		assertEquals(s, term.textToString());
	}
	public void testSetCopyInto() {
		TerminalTextData term=new TerminalTextData();
		term.setDimensions(5, 3);
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE";
		TerminalTextTestHelper.fill(term,0,0,s);
		TerminalTextData termCopy=new TerminalTextData();
		term.copyInto(termCopy);
		assertEquals(s, termCopy.textToString());
		assertEquals(s, term.textToString());
		
		termCopy.setChar(1, 1, 'X', null);
		assertEquals(s, term.textToString());
		term.setDimensions(4, 2);
		assertEquals(5, termCopy.getWidth());
		assertEquals(3, termCopy.getHeight());
		
		assertEquals("12345\n" +
				 "aXcde\n" +
				 "ABCDE", termCopy.textToString());

		assertEquals(4, term.getWidth());
		assertEquals(2, term.getHeight());
	}
	public void testSetCopyIntoWithOffset() {
		TerminalTextData term=new TerminalTextData();
		String s=
			"000\n" +
			"111\n" +
			"222\n" +
			"333\n" +
			"444\n" +
			"555\n" +
			"666\n" +
			"777\n" +
			"888";
		TerminalTextTestHelper.fill(term,s);
		TerminalTextData termCopy=new TerminalTextData();
		term.copyInto(termCopy,2,3);
		assertEquals("222\n" +
				"333\n" +
				"444", termCopy.textToString());
		assertEquals(s, term.textToString());
		
		term.copyInto(termCopy,0,4);
		assertEquals("000\n" +
				"111\n" +
				"222\n" +
				"333", termCopy.textToString());
		assertEquals(s, term.textToString());
		// does copy adjust dimensions
		termCopy.setDimensions(1, 1);
		term.copyInto(termCopy,2,3);
		assertEquals("222\n" +
				"333\n" +
				"444", termCopy.textToString());
		
		// does copy adjust dimensions
		termCopy.setDimensions(100, 100);
		term.copyInto(termCopy,2,3);
		assertEquals("222\n" +
				"333\n" +
				"444", termCopy.textToString());

		// does copy adjust dimensions
		termCopy.setDimensions(100, 100);
		term.copyInto(termCopy,0,9);
		assertEquals(s, termCopy.textToString());

		// is copy independent
		term.setChar(1, 1, 'X', null);
		assertEquals(s, termCopy.textToString());
	}
	public void testSetCopyIntoSelective() {
		TerminalTextData term=new TerminalTextData();
		String s=
			"111\n" +
			"222\n" +
			"333\n" +
			"444\n" +
			"555";
		TerminalTextTestHelper.fill(term, s);
		TerminalTextData termCopy=new TerminalTextData();
		String sCopy=
			"aaa\n" +
			"bbb\n" +
			"ccc\n" +
			"ddd\n" +
			"eee";
		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,new boolean []{true,true,false,false,true});
		assertEquals(s, term.textToString());
		assertEquals(			
				"111\n" +
				"222\n" +
				"ccc\n" +
				"ddd\n" +
				"555", termCopy.textToString());

		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,new boolean []{true,true,true,true,true});
		assertEquals(s, term.textToString());
		assertEquals(s, termCopy.textToString());
	
		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,new boolean []{false,false,false,false,false});
		assertEquals(s, term.textToString());
		assertEquals(sCopy, termCopy.textToString());
	}
	public void testSetCopyIntoSelectiveWithOffset() {
		TerminalTextData term=new TerminalTextData();
		String s=
			"111\n" +
			"222\n" +
			"333\n" +
			"444\n" +
			"555";
		TerminalTextTestHelper.fill(term, s);
		TerminalTextData termCopy=new TerminalTextData();
		String sCopy=
			"aaa\n" +
			"bbb\n" +
			"ccc\n" +
			"ddd\n" +
			"eee";
		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,1,new boolean []{true,false,false,true});
		assertEquals(s, term.textToString());
		assertEquals(			
				"222\n" +
				"bbb\n" +
				"ccc\n" +
				"555\n" +
				"eee", termCopy.textToString());

		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,2,new boolean []{true,true});
		assertEquals(s, term.textToString());
		assertEquals(			
				"333\n" +
				"444\n" +
				"ccc\n" +
				"ddd\n" +
				"eee", termCopy.textToString());

		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,0,new boolean []{true,true,true,true,true});
		assertEquals(s, term.textToString());
		assertEquals(s, termCopy.textToString());
	
		TerminalTextTestHelper.fill(termCopy, sCopy);
		term.copyInto(termCopy,0,new boolean []{false,false,false,false,false});
		assertEquals(s, term.textToString());
		assertEquals(sCopy, termCopy.textToString());
	}
	/**
	 * Stores the history in an instance of TerminalTextData
	 * The oldest history is at position 0
	 *
	 */
	static class TestHistory implements ITerminalTextHistory {
		Style style=Style.getStyle(StyleColor.getStyleColor("xx"), StyleColor.getStyleColor("xx"), false, false, false, false);

		TerminalTextData fData=new TerminalTextData();
		public void addToHistory(char[] chars, Style[] styles) {
			int width=Math.max(chars.length,fData.getWidth());
			fData.setDimensions(width, fData.getHeight()+1);
			for (int i = 0; i < chars.length; i++) {
				fData.setChar(i, fData.getHeight()-1, chars[i], styles[i]);
				// make sure that changing the passed in
				// chars and styles cannot change the TerminalTextData
				for (int j = 0; j < styles.length; j++) {
					styles[i]=style;
					chars[i]='?';
				}
			}
		}
		TerminalTextData getData() {
			return fData;
		}
	}
	public void testScrollWithHistory() {
		String s=
			"000\n" +
			"111\n" +
			"222\n" +
			"333\n" +
			"444\n" +
			"555\n" +
			"666\n" +
			"777\n" +
			"888";
		TerminalTextData term=new TerminalTextData();
		
		TerminalTextTestHelper.fill(term, s);
		TestHistory history= new TestHistory();
		history.getData().setDimensions(0, 0);
		term.scroll(0, 3, -2, history);
		assertEquals(
				"000\n" +
				"111", 
				history.getData().textToString());
		
		assertEquals(
				"222\n" +
				"\0\0\0\n" +
				"\0\0\0\n" +
				"333\n" +
				"444\n" +
				"555\n" +
				"666\n" +
				"777\n" +
				"888", 
				term.textToString());
		
	}
	public void testScrollWithHistoryAndResize() {
		String s=
			"000\n" +
			"111\n" +
			"222\n" +
			"333\n" +
			"444\n" +
			"555\n" +
			"666\n" +
			"777\n" +
			"888";
		TerminalTextData term=new TerminalTextData();
		
		TerminalTextTestHelper.fill(term, s);
		TestHistory history= new TestHistory();
		history.getData().setDimensions(0, 0);
		term.scroll(0, 3, -1, history);
		// shrink by one column!
		term.setDimensions(term.getWidth()-1, term.getHeight());

		term.scroll(0, 3, -1, history);
		assertEquals(
				"000\n" +
				"111", 
				history.getData().textToString());
		
		assertEquals(
				"22\n" +
				"\0\0\n" +
				"\0\0\n" +
				"33\n" +
				"44\n" +
				"55\n" +
				"66\n" +
				"77\n" +
				"88", 
				term.textToString());
		
	}
	public void testScrollNoop() {
		scrollTest(0,0,0, "012345","012345");
		scrollTest(0,1,0, "012345","012345");
		scrollTest(0,6,0, "012345","012345");
	}
	public void testScrollNegative() {
		scrollTest(0,2,-1,"012345","1 2345");
		scrollTest(0,1,-1,"012345"," 12345");
		scrollTest(0,6,-1,"012345","12345 ");
		scrollTest(0,6,-1,"012345","12345 ");
		scrollTest(0,6,-6,"012345","      ");
		scrollTest(0,6,-7,"012345","      ");
		scrollTest(0,6,-8,"012345","      ");
		scrollTest(0,6,-2,"012345","2345  ");
		scrollTest(1,1,-1,"012345","0 2345");
		scrollTest(1,2,-1,"012345","02 345");
		scrollTest(5,1,-1,"012345","01234 ");
		scrollTest(5,1,-1,"012345","01234 ");
	}
	public void testScrollPositive() {
		scrollTest(0,2,1, "012345",     " 02345");
		scrollTest(0,2,2, "012345",     "  2345");
		scrollTest(2,4,2, "012345",     "01  23");
		scrollTest(2,4,2, "0123456",    "01  236");
		scrollTest(0,7,6, "0123456",    "      0");
		scrollTest(0,7,8, "0123456",    "       ");
		scrollTest(0,7,9, "0123456",    "       ");
		scrollTest(2,4,2, "0123456",    "01  236");
		scrollTest(2,5,3, "0123456789", "01   23789");
		scrollTest(2,7,3, "0123456789", "01   23459");
		scrollTest(2,8,3, "0123456789", "01   23456");
		scrollTest(2,8,5, "0123456789", "01     234");
		scrollTest(2,8,9, "0123456789", "01        ");
		scrollTest(0,10,9,"0123456789", "         0");
		scrollTest(0,6,6, "012345",     "      ");
	}
	public void testScrollFail() {
		try {
			scrollTest(5,2,-1,"012345","012345");
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			scrollTest(0,7,1,"012345","      ");
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}
	/**
	 * Makes a simple shift test
	 * @param y scroll start
	 * @param n number of lines to be scrolled
	 * @param shift amount of lines to be shifted
	 * @param start the original data
	 * @param result the expected result
	 */
	void scrollTest(int y,int n, int shift, String start,String result) {
		TerminalTextData term=new TerminalTextData();
		TerminalTextTestHelper.fillSimple(term,start);
		term.scroll(y, n, shift);
		assertEquals(result, TerminalTextTestHelper.toSimple(term));
		
	}
}
