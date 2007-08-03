package org.eclipse.tm.terminal.model;


/**
 * This interface
 *
 * <b>Note:</b> This interface is intended to be implemented by clients.
 */
public interface ITerminalTextHistory {
	/**
	 * @param chars
	 * @param styles
	 */
	void addToHistory(char[] chars,Style[] styles);
}
