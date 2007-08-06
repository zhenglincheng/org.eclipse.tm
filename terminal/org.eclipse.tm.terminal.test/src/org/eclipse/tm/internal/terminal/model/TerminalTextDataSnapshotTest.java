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

import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.Style;
import org.eclipse.tm.terminal.model.StyleColor;

public class TerminalTextDataSnapshotTest extends TestCase {

	public void testTerminalTextDataSnapshot() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		assertEquals(term.textToString(), snapshot.textToString());
		
		// new snapshots are fully changed
		assertEquals(0,snapshot.getFirstChangedLine());
		assertEquals(term.getHeight()-1,snapshot.getLastChangedLine());
		for (int y = 0; y <= snapshot.getLastChangedLine(); y++) {
			assertTrue(snapshot.hasLineChanged(y));
		}
		// nothing has scrolled
		assertEquals(0, snapshot.getScrollChangeN());
	}

	public void testDetach() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		assertEquals(term.textToString(),snapshot.textToString());
		snapshot.detach();
		// after detach changes to the term has no effect
		term.setChar(0, 0, '?', null);
		assertEquals(s, snapshot.textToString());
		term.setDimensions(2, 2);
		assertEquals(s, snapshot.textToString());
	}

	public void testHasChanged() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		assertFalse(snapshot.hasChanged());
		
		// make a change and expect it to be changed
		term.setChar(0, 0, '?', null);
		assertTrue(snapshot.hasChanged());
		
		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
		
		// make a change and expect it to be changed
		term.setChars(1, 1, new char[]{'?','!','.'},null);
		assertTrue(snapshot.hasChanged());
		
		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
		
		// scroll
		term.scroll(1, 2, -1);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
		
		// scroll
		term.scroll(1, 2, 1);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());

		// scroll
		term.scroll(1, 2, -1);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(true);
		assertFalse(snapshot.hasChanged());
		
		// scroll
		term.scroll(1, 2, 1);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(true);
		assertFalse(snapshot.hasChanged());
		
		// setDimensions
		term.setDimensions(2, 2);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
		
		// setDimensions
		term.setDimensions(20, 20);
		assertTrue(snapshot.hasChanged());

		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
		
	}
	TerminalTextDataSnapshot snapshot(String text, TerminalTextData term) {
		TerminalTextTestHelper.fill(term,text);
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		return snapshot;
		
	}
	public void testUpdateSnapshot() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		String termString=term.textToString();
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		assertEquals(termString,snapshot.textToString());
		
		// make changes and assert that the snapshot has not changed
		// then update the snapshot and expect it to be the
		// same as the changed terminal
		
		// make a change 
		term.setChar(0, 0, '?', null);
		assertEquals(termString,snapshot.textToString());
		
		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// make a change 
		term.setChars(1, 1, new char[]{'?','!','.'},null);
		assertEquals(termString,snapshot.textToString());
		
		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// scroll
		term.scroll(1, 2, -1);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// scroll
		term.scroll(1, 2, -1);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(true);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(true);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
		
		// set dimensions
		term.setDimensions(2, 2);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());

		// set dimensions
		term.setDimensions(20, 20);
		assertEquals(termString,snapshot.textToString());

		snapshot.updateSnapshot(false);
		termString=term.textToString();
		assertEquals(termString,snapshot.textToString());
}

	public void testGetChar() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		TerminalTextData termUnchanged=new TerminalTextData();
		TerminalTextTestHelper.fill(termUnchanged,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		for (int y = 0; y < snapshot.getHeight(); y++) {
			for (int x = 0; x < snapshot.getWidth(); x++) {
				assertEquals(term.getChar(x, y),snapshot.getChar(x, y));
			}
		}
		// make a change 
		term.setChar(0, 0, '?', null);
		// check against unchanged data
		for (int y = 0; y < snapshot.getHeight(); y++) {
			for (int x = 0; x < snapshot.getWidth(); x++) {
				assertEquals(termUnchanged.getChar(x, y),snapshot.getChar(x, y));
			}
		}
		// update and compare against the terminal
		snapshot.updateSnapshot(true);
		for (int y = 0; y < snapshot.getHeight(); y++) {
			for (int x = 0; x < snapshot.getWidth(); x++) {
				assertEquals(term.getChar(x, y),snapshot.getChar(x, y));
			}
		}
		
	}

	public void testGetHeight() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		int expectedHeight=term.getHeight();
		assertEquals(expectedHeight, snapshot.getHeight());
		term.setDimensions(term.getWidth(), term.getHeight()-1);
		assertEquals(expectedHeight, snapshot.getHeight());
		
		//
		snapshot.updateSnapshot(false);
		expectedHeight=term.getHeight();
		assertEquals(expectedHeight, snapshot.getHeight());
		term.setDimensions(term.getWidth(), term.getHeight()-1);
		assertEquals(expectedHeight, snapshot.getHeight());
	}
//
//	public void testGetLineSegments() {
//		fail("Not yet implemented");
//	}
//
	public void testGetStyle() {
		TerminalTextData term=new TerminalTextData();
		Style style=Style.getStyle(StyleColor.getStyleColor("fg"), StyleColor.getStyleColor("bg"), false, false, false, false);
		term.setDimensions(3, 6);
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				term.setChar(x, y, c, style.setForground(StyleColor.getStyleColor(""+c)));
			}
		}
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		
		for (int y = 0; y < term.getHeight(); y++) {
			for (int x = 0; x < term.getWidth(); x++) {
				char c=(char)('a'+x+y);
				assertSame(style.setForground(StyleColor.getStyleColor(""+c)), snapshot.getStyle(x, y));
			}
		}
		
	}

	public void testGetWidth() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		int expectedWidth=term.getWidth();
		assertEquals(expectedWidth, snapshot.getWidth());
		term.setDimensions(term.getWidth()-1, term.getHeight());
		assertEquals(expectedWidth, snapshot.getWidth());
		
		//
		snapshot.updateSnapshot(false);
		expectedWidth=term.getWidth();
		assertEquals(expectedWidth, snapshot.getWidth());
		term.setDimensions(term.getWidth()-1, term.getHeight());
		assertEquals(expectedWidth, snapshot.getWidth());
	}

	public void testGetFirstChangedLine() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextDataSnapshot snapshot=snapshot(s,term);

		
		assertEquals(0, snapshot.getFirstChangedLine());
		
		// if nothing has changed the first changed line i height
		snapshot.updateSnapshot(false);
		assertEquals(snapshot.getHeight(), snapshot.getFirstChangedLine());
		
		snapshot=snapshot(s,term);
		term.setChar(0, 0, 'x', null);
		snapshot.updateSnapshot(false);
		assertEquals(0, snapshot.getFirstChangedLine());
		
		snapshot=snapshot(s,term);		
		term.setChar(0, 3, 'x', null);
		term.setChar(0, 4, 'x', null);
		snapshot.updateSnapshot(false);
		assertEquals(3, snapshot.getFirstChangedLine());
		
		snapshot=snapshot(s,term);
		term.scroll(0, 1, -1);
		snapshot.updateSnapshot(false);
		assertEquals(0, snapshot.getFirstChangedLine());
		
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		snapshot.updateSnapshot(false);
		assertEquals(2, snapshot.getFirstChangedLine());
		
		// when scrolling the end of the region 'has changed'
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		snapshot.updateSnapshot(true);
		assertEquals(3, snapshot.getFirstChangedLine());
		
		// when scrolling the end of the region 'has changed'
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		term.setChar(0, 1, 'x', null);
		snapshot.updateSnapshot(true);
		assertEquals(1, snapshot.getFirstChangedLine());
		
	}
	public void testGetLastChangedLine() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextDataSnapshot snapshot=snapshot(s,term);

		
		assertEquals(4, snapshot.getLastChangedLine());
		
		// if nothing has changed the first changed line i height
		snapshot.updateSnapshot(false);
		assertEquals(-1, snapshot.getLastChangedLine());
		
		snapshot=snapshot(s,term);
		term.setChar(0, 0, 'x', null);
		snapshot.updateSnapshot(false);
		assertEquals(0, snapshot.getLastChangedLine());
		
		snapshot=snapshot(s,term);		
		term.setChar(0, 3, 'x', null);
		term.setChar(0, 4, 'x', null);
		snapshot.updateSnapshot(false);
		assertEquals(4, snapshot.getLastChangedLine());
		
		snapshot=snapshot(s,term);
		term.scroll(0, 1, -1);
		snapshot.updateSnapshot(false);
		assertEquals(0, snapshot.getLastChangedLine());
		
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		snapshot.updateSnapshot(false);
		assertEquals(3, snapshot.getLastChangedLine());
		
		// when scrolling the end of the region 'has changed'
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		snapshot.updateSnapshot(true);
		assertEquals(3, snapshot.getLastChangedLine());
		
		// when scrolling the end of the region 'has changed'
		snapshot=snapshot(s,term);
		term.scroll(2, 2, -1);
		term.setChar(0, 1, 'x', null);
		snapshot.updateSnapshot(true);
		assertEquals(3, snapshot.getLastChangedLine());
		
	}
	/**
	 * @param snapshot
	 * @param expected a string of 0 and 1 (1 means changed)
	 */
	void assertChangedLines(ITerminalTextDataSnapshot snapshot, String expected) {
		assertEquals(expected.length(),snapshot.getHeight());
		StringBuffer buffer=new StringBuffer();
		for (int y = 0; y < expected.length(); y++) {
			if(snapshot.hasLineChanged(y))
				buffer.append('1');
			else
				buffer.append('0');
		}
		assertEquals(expected, buffer.toString());
	}
	public void testHasLineChangedScroll() {
		TerminalTextData term=new TerminalTextData();
		String s="00\n" +
				 "11\n" +
				 "22\n" +
				 "33\n" +
				 "44\n" +
				 "55\n" +
				 "66\n" +
				 "77\n" +
				 "88\n" +
				 "99";
		TerminalTextDataSnapshot snapshot=snapshot(s,term);
		
		term.scroll(2,3,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0000100000");
		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-2);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0001100000");

		snapshot=snapshot(s,term);
		term.scroll(2,4,-1);
		term.scroll(2,4,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0000110000");

		term.scroll(2,3,1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0011100000");
		
		snapshot=snapshot(s,term);
		term.scroll(2,3,2);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0011100000");

		snapshot=snapshot(s,term);
		term.scroll(2,4,1);
		term.scroll(2,4,1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0011110000");

		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-1);
		snapshot.updateSnapshot(false);
		assertChangedLines(snapshot, "0011100000");
		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-2);
		snapshot.updateSnapshot(false);
		assertChangedLines(snapshot, "0011100000");

		snapshot=snapshot(s,term);
		term.scroll(2,4,-1);
		term.scroll(2,4,-1);
		snapshot.updateSnapshot(false);
		assertChangedLines(snapshot, "0011110000");
	}
	public void testMultiScrollWithDifferentSizes() {
		TerminalTextData term=new TerminalTextData();
		String s="00\n" +
				 "11\n" +
				 "22\n" +
				 "33\n" +
				 "44\n" +
				 "55\n" +
				 "66\n" +
				 "77\n" +
				 "88\n" +
				 "99";
		TerminalTextDataSnapshot snapshot;

		snapshot=snapshot(s,term);
		term.scroll(2,6,-1);
		term.scroll(2,5,-1);
		snapshot.updateSnapshot(false);
		assertChangedLines(snapshot, "0011111100");
		assertEquals(2, snapshot.getFirstChangedLine());
		assertEquals(7, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeShift());
		
		// scrolls with different ranges cause no scroll
		// optimization
		snapshot=snapshot(s,term);
		term.scroll(2,6,-1);
		term.scroll(2,5,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0011111100");
		assertEquals(2, snapshot.getFirstChangedLine());
		assertEquals(7, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeShift());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeY());
	}
	public void testHasLineChanged() {
		TerminalTextData term=new TerminalTextData();
		String s="000000\n" +
				"111111\n" +
				"222222\n" +
				"333333\n" +
				"444444\n" +
				"555555\n" +
				"666666\n" +
				"777777\n" +
				"888888\n" +
				"999999";
		TerminalTextDataSnapshot snapshot;
		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-1);
		term.setChar(0, 7, '.', null);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0000100100");
		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-2);
		term.setChar(0, 9, '.', null);
		term.setChars(0, 0, new char[]{'.','!'}, null);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1001100001");

		snapshot=snapshot(s,term);
		term.scroll(2,4,-1);
		term.scroll(2,4,-1);
		term.setChars(2, 2, new char[]{'.','!','*'},1,1, null);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0010110000");

		snapshot=snapshot(s,term);
		term.scroll(2,7,-1);
		term.setChar(2, 5, '.', null);
		term.scroll(2,7,-2);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0001001110");

		
		snapshot=snapshot(s,term);
		term.scroll(2,7,-1);
		term.setChar(2, 5, '.', null);
		term.scroll(2,7,-2);
		snapshot.updateSnapshot(false);
		assertChangedLines(snapshot, "0011111110");

	}

	public void testScroll() {
		TerminalTextData term=new TerminalTextData();
		String s="00\n" +
				 "11\n" +
				 "22\n" +
				 "33\n" +
				 "44\n" +
				 "55\n" +
				 "66\n" +
				 "77\n" +
				 "88\n" +
				 "99";
		TerminalTextDataSnapshot snapshot=snapshot(s,term);
		
		term.scroll(2,3,-1);
		snapshot.updateSnapshot(true);
		assertEquals(2, snapshot.getScrollChangeY());
		assertEquals(3, snapshot.getScrollChangeN());
		assertEquals(-1, snapshot.getScrollChangeShift());
		assertEquals(4, snapshot.getFirstChangedLine());
		assertEquals(4, snapshot.getLastChangedLine());
		
		term.scroll(2,3,-2);
		snapshot.updateSnapshot(true);
		assertEquals(2, snapshot.getScrollChangeY());
		assertEquals(3, snapshot.getScrollChangeN());
		assertEquals(-2, snapshot.getScrollChangeShift());
		assertEquals(3, snapshot.getFirstChangedLine());
		assertEquals(4, snapshot.getLastChangedLine());

		term.scroll(2,4,-1);
		term.scroll(2,4,-1);
		snapshot.updateSnapshot(true);
		assertEquals(2, snapshot.getScrollChangeY());
		assertEquals(4, snapshot.getScrollChangeN());
		assertEquals(-2, snapshot.getScrollChangeShift());
		assertEquals(4, snapshot.getFirstChangedLine());
		assertEquals(5, snapshot.getLastChangedLine());

		
		snapshot=snapshot(s,term);
		term.scroll(2,3,-1);
		snapshot.updateSnapshot(false);
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
		assertEquals(2, snapshot.getFirstChangedLine());
		assertEquals(4, snapshot.getLastChangedLine());
		
	}
	public void testDisjointScroll() {
		TerminalTextData term=new TerminalTextData();
		String s="000000\n" +
				"111111\n" +
				"222222\n" +
				"333333\n" +
				"444444\n" +
				"555555\n" +
				"666666\n" +
				"777777\n" +
				"888888\n" +
				"999999";
		TerminalTextDataSnapshot snapshot;
		
		snapshot=snapshot(s,term);
		term.scroll(0,2,-1);
		term.scroll(4,2,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1100110000");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.scroll(0,3,-1);
		term.scroll(2,2,-2);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111000000");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.scroll(0,3,-1);
		term.scroll(2,2,-2);
		term.scroll(0,3,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111000000");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.scroll(0,3,-1);
		term.scroll(2,2,-2);
		term.scroll(0,3,-10);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111000000");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.scroll(1,3,-1);
		term.scroll(1,3,1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "0111000000");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
	}
	public void testResize() {
		TerminalTextData term=new TerminalTextData();
		String s="000000\n" +
				"111111\n" +
				"222222\n" +
				"333333";
		TerminalTextDataSnapshot snapshot;
		
		snapshot=snapshot(s,term);
		term.setDimensions(term.getWidth()+1, term.getHeight());
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111");
		assertEquals(0, snapshot.getFirstChangedLine());
		assertEquals(3, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.setDimensions(term.getWidth(), term.getHeight()+1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "11111");
		assertEquals(0, snapshot.getFirstChangedLine());
		assertEquals(4, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
	
		snapshot=snapshot(s,term);
		term.setDimensions(term.getWidth(), term.getHeight()-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "111");
		assertEquals(0, snapshot.getFirstChangedLine());
		assertEquals(2, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
	
		snapshot=snapshot(s,term);
		term.setDimensions(0, 0);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "");
		assertEquals(0, snapshot.getFirstChangedLine());
		assertEquals(-1, snapshot.getLastChangedLine());
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
	
	}
	public void testResizeAfterScroll() {
		TerminalTextData term=new TerminalTextData();
		String s="000000\n" +
				"111111\n" +
				"222222\n" +
				"333333\n" +
				"444444\n" +
				"555555\n" +
				"666666\n" +
				"777777\n" +
				"888888\n" +
				"999999";
		TerminalTextDataSnapshot snapshot;
		
		snapshot=snapshot(s,term);
		term.scroll(1,2,-1);
		term.setDimensions(4, 5);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "11111");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());

		snapshot=snapshot(s,term);
		term.scroll(1,2,-1);
		term.setDimensions(2, 7);
		term.scroll(4,2,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111111");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
		snapshot=snapshot(s,term);

		term.scroll(1,2,-1);
		term.setDimensions(term.getWidth()+1,term.getHeight());
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "1111111111");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
	}
	public void testScrollAfterResize() {
		TerminalTextData term=new TerminalTextData();
		String s="000000\n" +
				"111111\n" +
				"222222\n" +
				"333333\n" +
				"444444\n" +
				"555555\n" +
				"666666\n" +
				"777777\n" +
				"888888\n" +
				"999999";
		TerminalTextDataSnapshot snapshot;
		
		snapshot=snapshot(s,term);
		term.setDimensions(6, 14);
		term.scroll(0,14,-1);
		snapshot.updateSnapshot(true);
		assertChangedLines(snapshot, "11111111111111");
		assertEquals(0, snapshot.getScrollChangeY());
		assertEquals(0, snapshot.getScrollChangeN());
		assertEquals(0, snapshot.getScrollChangeShift());
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
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		TerminalTextDataSnapshot snapshot1=(TerminalTextDataSnapshot)term.makeSnapshot();
		term.scroll(y, n, shift);
		assertEquals(result, TerminalTextTestHelper.toSimple(term));
		snapshot.updateSnapshot(false);
		assertEquals(result, TerminalTextTestHelper.toSimple(snapshot.textToString()));
		snapshot1.updateSnapshot(true);
		assertEquals(result, TerminalTextTestHelper.toSimple(snapshot1.textToString()));
		
		
	}
	private final class SnapshotListener implements ITerminalTextDataSnapshot.SnapshotNeedUpdateListener {
		int N;
		public void changed() {
			N++;
		}
		public void reset() {
			N=0;
		}
	}

	public void testAddListener() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		SnapshotListener listener=new SnapshotListener();
		snapshot.addListener(listener);
		assertEquals(0, listener.N);
		
		// make a change and expect it to be changed
		term.setChar(0, 0, '?', null);
		assertEquals(1, listener.N);
		term.setChar(1, 1, '?', null);
		assertEquals(1, listener.N);
		
		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();

		// make a change and expect it to be changed
		term.setChars(1, 1, new char[]{'?','!','.'},null);
		assertEquals(1, listener.N);
		term.setChars(1, 2, new char[]{'?','!','.'},null);
		assertEquals(1, listener.N);
		
		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();
		
		// scroll
		term.scroll(1, 2, -1);
		assertEquals(1, listener.N);
		term.scroll(1, 2, -1);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();
		
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(1, listener.N);
		term.scroll(1, 2, 1);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();

		// scroll
		term.scroll(1, 2, -1);
		assertEquals(1, listener.N);
		term.scroll(1, 2, -1);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();
		
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();
		
		// setDimensions
		term.setDimensions(2, 2);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertEquals(1, listener.N);
		listener.reset();
		
		// setDimensions
		term.setDimensions(20, 20);
		assertEquals(1, listener.N);

		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
	}

	public void testRemoveListener() {
		TerminalTextData term=new TerminalTextData();
		String s="12345\n" +
				 "abcde\n" +
				 "ABCDE\n" +
				 "vwxzy\n" +
				 "VWXYZ";
		TerminalTextTestHelper.fill(term,s);
		
		TerminalTextDataSnapshot snapshot=(TerminalTextDataSnapshot)term.makeSnapshot();
		SnapshotListener listener1=new SnapshotListener();
		SnapshotListener listener2=new SnapshotListener();
		SnapshotListener listener3=new SnapshotListener();
		snapshot.addListener(listener1);
		snapshot.addListener(listener2);
		snapshot.addListener(listener3);
		assertEquals(0, listener1.N);
		
		// make a change and expect it to be changed
		term.setChar(0, 0, '?', null);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);
		term.setChar(1, 1, '?', null);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);
		
		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);
		listener1.reset();
		listener2.reset();
		listener3.reset();

		// make a change and expect it to be changed
		term.setChars(1, 1, new char[]{'?','!','.'},null);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);
		term.setChars(1, 2, new char[]{'?','!','.'},null);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);

		
		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);

		listener1.reset();
		listener2.reset();
		listener3.reset();

		snapshot.removeListener(listener2);

		// scroll
		term.scroll(1, 2, -1);
		assertEquals(1, listener1.N);
		assertEquals(0, listener2.N);
		assertEquals(1, listener3.N);

		term.scroll(1, 2, -1);
		assertEquals(1, listener1.N);
		assertEquals(0, listener2.N);
		assertEquals(1, listener3.N);


		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(0, listener2.N);
		assertEquals(1, listener3.N);

		snapshot.addListener(listener2);
		listener1.reset();
		listener2.reset();
		listener3.reset();

		
		snapshot.removeListener(listener3);
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(0, listener3.N);

		term.scroll(1, 2, 1);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(0, listener3.N);


		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(0, listener3.N);

		snapshot.addListener(listener3);
		listener1.reset();
		listener2.reset();
		listener3.reset();

		// add listener multiple times
		snapshot.addListener(listener3);
		
		// scroll
		term.scroll(1, 2, -1);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(2, listener3.N);

		term.scroll(1, 2, -1);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(2, listener3.N);


		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(2, listener3.N);

		listener1.reset();
		listener2.reset();
		listener3.reset();
		// remove the duplicate listerner
		snapshot.removeListener(listener3);

		
		// scroll
		term.scroll(1, 2, 1);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);


		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);

		listener1.reset();
		listener2.reset();
		listener3.reset();

		
		// setDimensions
		term.setDimensions(2, 2);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);


		snapshot.updateSnapshot(false);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);

		listener1.reset();
		listener2.reset();
		listener3.reset();

		
		// setDimensions
		term.setDimensions(20, 20);
		assertEquals(1, listener1.N);
		assertEquals(1, listener2.N);
		assertEquals(1, listener3.N);


		snapshot.updateSnapshot(false);
		assertFalse(snapshot.hasChanged());
	}

}
