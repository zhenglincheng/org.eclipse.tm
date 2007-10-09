<!doctype html public "-//w3c//dtd html 4.0 transitional//en">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="http://www.eclipse.org/default_style.css" type="text/css">
<title>Build Notes for TM @buildId@</title>
</head>

<body>
<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" width="80%">
		<p><b><font class=indextop>Build Notes for TM @buildId@</font></b><br>
		@dateLong@ </p>
		</td>
	</tr>
</table>
<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#0080C0"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">New and Noteworthy</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
<ul>
<li>TM @buildId@ <b>requires Eclipse 3.3 later for the SSH component</b>.
  Other components may work with earlier Eclipse versions, but these have not been tested.
  Platform Runtime is the minimum requirement for core RSE and Terminal.
  Discovery needs EMF, and the RemoteCDT integration needs CDT.</li>
<!--
<li><b>Apache Commons.Net and ORO</b> are now distributed as verbatim compies
  from the Orbit project, so they will not be changed any more.</li>
-->
<li>Highlights of Bugs fixed since TM 2.0.1:
    <ul>
    <li>Terminal: System Property <b>-Dorg.eclipse.tm.terminal.OldImplementation=true</b> can now be used to fall back to old terminal implementation [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205385">bug 205385</a>]</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205393">bug 205393</a> - <font color="red"><b>cri - [terminal] stack overflow</b></font></li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205772">bug 205772</a> - <font color="red"><b>cri - [terminal] crash on linux (division by zero)</b></font></li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205297">bug 205297</a> - <font color="red"><b>cri - SystemTempFileListener calls upload() in the dispatch thread</b></font></li>
<!--
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=204943">bug 204943</a> - nor - [terminal][regression] Cannot expand selection with shift+click</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205385">bug 205385</a> - nor - [terminal] Use system property to switch to old implementation</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205389">bug 205389</a> - nor - [terminal] null pointer exception when opening a view</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205443">bug 205443</a> - nor - [terminal] view refresh problem between perspectives</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205679">bug 205679</a> - nor - [terminal] Initial cursor and scrollbar are wrong</li>
    <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=205592">bug 205592</a> - nor - [regression] ClassCastException when trying to expand folder that doesn't have read permissions</li>
-->
</ul></li>
<li>Plugins and Features changed since TM 2.0.1:
<ul><li>Features: org.eclipse.tm.terminal-feature, org.eclipse.tm.terminal.sdk-feature
    <ul><li>Plugin: <b>org.eclipse.tm.terminal</b> (<a href="https://bugs.eclipse.org/bugs/buglist.cgi?quicksearch=205385,205389,205393,205443,205679,205772">205385,205389,205393,205443,205679,205772</a>)</li>
        <li>Plugin: org.eclipse.tm.terminal.test (added)</li>
    </ul></li>
    <li>Features: org.eclipse.rse.core-feature, org.eclipse.rse-feature, org.eclipse.rse.sdk-feature
    <ul><li>Plugin: <b>org.eclipse.rse.files.ui</b> (<a href="https://bugs.eclipse.org/bugs/buglist.cgi?quicksearch=205297">205297</a>)</li>
        <li>Plugin: <b>org.eclipse.rse.ui</b> (<a href="https://bugs.eclipse.org/bugs/buglist.cgi?quicksearch=205592">205592</a>)</li>
    </ul></li>
</ul></li>
<li>Use 
  <!-- <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WONTFIX&resolution=INVALID&resolution=WORKSFORME&chfieldfrom=2007-09-29&chfieldto=2007-10-10&chfield=resolution&cmdtype=doit">  -->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&target_milestone=2.0.2&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WONTFIX&resolution=INVALID&resolution=WORKSFORME&cmdtype=doit">
  this query</a> to show the list of bugs fixed since the last milestone,
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/R-2.0.1-200709270920/index.php">
  TM 2.0.1</a>
  [<a href="http://download.eclipse.org/dsdp/tm/downloads/drops/R-2.0.1-200709270920/buildNotes.php">build notes</a>].</li>
<li>For details on checkins, see the
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/N-changelog/index.html">
  RSE CVS changelog</a>, and the
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/N-changelog/core/index.html">
  TM Core CVS changelog</a>.</li>
<li>For other questions, please check the
  <a href="http://wiki.eclipse.org/index.php/TM_and_RSE_FAQ">TM and RSE FAQ</a>
  as well as the
  <a href="http://wiki.eclipse.org/index.php/TM_2.0_Known_Issues_and_Workarounds">
  TM 2.0 Known Issues and Workarounds</a>.</li>
</ul>
</td></tr></tbody></table>

<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#0080C0"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">Getting Started</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
<p>The RSE User Documentation now has a
<a href="http://dsdp.eclipse.org/help/latest/index.jsp?topic=/org.eclipse.rse.doc.user/gettingstarted/g_start.html">
Tutorial</a> that guides you through installation, first steps,
connection setup and important tasks.</p>
<p>
If you want to know more about future directions of the Target Management
Project, developer documents, architecture or how to get involved,<br/>
the online
<a href="http://www.eclipse.org/dsdp/tm/tutorial/index.php">Getting Started page</a>
as well as the
<a href="http://wiki.eclipse.org/index.php/TM_and_RSE_FAQ">TM and RSE FAQ</a>
are the best places for you to get started.
</p>
</td></tr></tbody></table>

<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#0080C0"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">API Status</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
<p>No API changes are allowed in the TM 2.0.x maintenance stream.
Therefore, <b>TM 2.0.2 is fully upward and backward compatible with TM 2.0 and TM 2.0.1</b>,
and can be fully exchanged for TM 2.0 in any product based on it.</p>

<p>For the upcoming TM 3.0 release, some API changes will be inevitable.
Although we completed a great deal of API cleanup for TM 2.0, we decided
to still mark all API as <i>provisional</i> since we expect more work to do.
If anyhow possible, we will avoid breaking API changes after TM 2.0, but please 
be prepared for future changes, and especially take care of API marked as 
<b>@deprecated</b> in the Javadoc.
Such API is prime candidate to be removed in the future. All
API changes will be voted by committers on the 
<a href="https://dev.eclipse.org/mailman/listinfo/dsdp-tm-dev">
dsdp-tm-dev</a> developer mailing list, and documented in a migration guide
for future releases. Early migration information can also be found right
in the bug reports. Look for those that are tagged [api][breaking].</p>
</td></tr></tbody></table>

<!--
<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#808080"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">API Changes since TM 2.0 - newest changest first</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
The following lists those API changes that are not backward compatible and require
user attention. A short hint on what needs to change is given directly in the list.
More information can be found in the associated bugzilla items.

<ul>
<li>TM @buildId@ Breaking API Changes
<ul>
<li>None</li> 
</ul>
</li>
</ul>

-->
Use 
<!--
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi%5D&classification=DSDP&product=Target+Management&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WONTFIX&resolution=INVALID&resolution=WORKSFORME&chfieldfrom=2007-06-28&chfieldto=2008-07-01&chfield=resolution&cmdtype=doit">
-->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi%5D&classification=DSDP&product=Target+Management&target_milestone=---&target_milestone=2.0.1&target_milestone=Future&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&cmdtype=doit">
  this query</a> to show the full list of API changes proposed for TM 3.0.
</td></tr></tbody></table>

<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#0080C0"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">Known Problems and Workarounds</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
The following critical or major bugs are currently known.
We'll strive to fix these as soon as possible.
<ul>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=198143">bug 198143</a> - maj - [dstore][performance] Refresh a big directory takes very long time, and freezes workbench</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=198395">bug 198395</a> - maj - [dstore] Can connect to DStore with expired password</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=203501">bug 203501</a> - maj - NPE in PFMetadataLocation when saving RSEDOM</li>
</ul>
<!--
<p>No major or critical bugs are known at the time of release.
-->
Use 
<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&bug_severity=blocker&bug_severity=critical&bug_severity=major&cmdtype=doit">this query</a>
for an up-to-date list of major or critical bugs.</p>

<p>The 
<a href="http://wiki.eclipse.org/index.php/TM_2.0_Known_Issues_and_Workarounds">
TM 2.0 Known Issues and Workarounds</a> Wiki page gives an up-to-date list
of the most frequent and obvious problems, and describes workarounds for them.<br/>
If you have other questions regarding TM or RSE, please check the
<a href="http://wiki.eclipse.org/index.php/TM_and_RSE_FAQ">TM and RSE FAQ</a>
</p>

<p>Click 
<a href="https://bugs.eclipse.org/bugs/report.cgi?x_axis_field=bug_severity&y_axis_field=op_sys&z_axis_field=&query_format=report-table&classification=DSDP&product=Target+Management&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&format=table&action=wrap">here</a>
for a complete up-to-date bugzilla status report, or
<a href="https://bugs.eclipse.org/bugs/report.cgi?x_axis_field=bug_severity&y_axis_field=op_sys&z_axis_field=&query_format=report-table&classification=DSDP&product=Target+Management&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&format=table&action=wrap">here</a>
for a report on bugs fixed so far.
</p>
</td></tr></tbody></table>

</body>
</html>
