package org.eclipse.tm.terminal.model;

import org.eclipse.tm.internal.terminal.model.TerminalTextData;

public class TerminalTextDataFactory {
	static public ITerminalTextData makeTerminalTextData() {
		return new TerminalTextData();
	}
}
