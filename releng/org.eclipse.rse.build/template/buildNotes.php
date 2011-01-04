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
<li>TM @buildId@ <b>requires Eclipse 3.4 or later</b>.
  <b>Import/Export, Telnet and FTP require Java 1.5</b>, the rest of
  RSE runs on Java 1.4.
  Platform Runtime is the minimum requirement for core RSE and Terminal.
  Discovery needs EMF.</li>
<li>Highlights of Bugs fixed since <a href="http://download.eclipse.org/tm/downloads/drops/R-3.1.2-201002152323/index.php">TM 3.1.2</a>:
<ul>
  <li>A regression in the Terminal widget was fixed, which made initial output after login
      invisible above the initial viewport
      [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=294327">294327</a>].</li>
</ul>
</li>
<li>At least 2 bugs were resolved: Use 
  <!-- <a href="https://bugs.eclipse.org/bugs/query.cgi?bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&chfield=resolution&chfieldfrom=2009-09-24&chfieldto=2010-03-01&component=Core&component=RSE&component=Terminal&field0-0-0=target_milestone&field0-0-1=target_milestone&field0-0-2=target_milestone&field0-0-3=target_milestone&negate0=1&product=Target%20Management&query_format=advanced&resolution=FIXED&resolution=WONTFIX&resolution=WORKSFORME&type0-0-0=regexp&type0-0-1=regexp&type0-0-2=equals&type0-0-3=equals&value0-0-0=[23]\.[02].*&value0-0-1=3.2%20M[234567]&value0-0-2=3.1&value0-0-3=3.1.1"> -->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?resolution=FIXED;resolution=WONTFIX;resolution=WORKSFORME;query_format=advanced;bug_status=RESOLVED;bug_status=VERIFIED;bug_status=CLOSED;component=Core;component=RSE;component=Terminal;target_milestone=3.1.2%2B;product=Target%20Management">
  
  this query</a> to show the list of bugs fixed since
  <a href="http://download.eclipse.org/tm/downloads/drops/R-3.1.2-201002152323/">
  TM 3.1.2</a>
  [<a href="http://download.eclipse.org/tm/downloads/drops/R-3.1.2-201002152323/buildNotes.php">build notes</a>].</li>
<li>For details on checkins, see
  <a href="http://dsdp.eclipse.org/dsdp/tm/searchcvs.php">TM SearchCVS</a>, the
  <a href="http://download.eclipse.org/tm/downloads/drops/N-changelog/index.html">
  RSE CVS changelog</a>, and the
  <a href="http://download.eclipse.org/tm/downloads/drops/N-changelog/core/index.html">
  TM Core CVS changelog</a>.</li>
<li>For other questions, please check the
  <a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">TM and RSE FAQ</a>
  as well as the
  <a href="http://wiki.eclipse.org/TM/3.1_Known_Issues_and_Workarounds">
  TM 3.1 Known Issues and Workarounds</a>.</li>
</ul>
</td></tr></tbody></table>

<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#0080C0"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">Getting Started</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
<p>The RSE User Documentation has a
<a href="http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.rse.doc.user/gettingstarted/g_start.html">
Tutorial</a> that guides you through installation, first steps,
connection setup and important tasks.</p>
<p>
If you want to know more about future directions of the Target Management
Project, developer documents, architecture or how to get involved,
the online
<a href="http://www.eclipse.org/tm/tutorial/index.php">Getting Started page</a>
as well as the
<a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">TM and RSE FAQ</a>
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
<p>No API changes are allowed in the TM 3.1.x maintenance stream.
Therefore, <b>TM 3.1.x is fully upward and backward compatible with TM 3.1</b>,
and can be fully exchanged for TM 3.1 in any product based on it.
Take care of API specification updates though, where the TM 3.1.x API Docs
have been updated to add clarifications or missing information compared
to 3.1.</p>

<p>For the upcoming TM 3.2 release, only backward compatible API changes
are planned, especially in order to support improved componentization
and UI/Non-UI splitting.
In the interest of improving the code base, though, please 
take care of API marked as <b>@deprecated</b> in the Javadoc.
Such API is prime candidate to be removed in the future.
Also, observe the API Tooling tags such as <b>@noextend</b> and 
<b>@noimplement</b>.
</p>
</td></tr></tbody></table>

<table border="0" cellspacing="5" cellpadding="2" width="100%">
	<tr>
		<td align="LEFT" valign="TOP" colspan="3" bgcolor="#808080"><b>
		<font face="Arial,Helvetica" color="#FFFFFF">API Specification Updates since TM 3.1</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
The following lists amendments to API specifications that are worth noticing,
and may require changes in client code even though they are binary compatible.
More information can be found in the associated bugzilla items.

<ul>
<li>TM @buildId@ API Specification Updates
<ul>
  <li>None.</li>
</ul></li>
</ul>
</li>
</ul>

Use 
  <!-- 
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi&product=Target+Management&component=Core&component=RSE&component=Terminal&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WORKSFORME&chfieldfrom=2009-06-20&chfieldto=2009-09-25&chfield=resolution&cmdtype=doit">
   -->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi&product=Target+Management&component=Core&component=RSE&component=Terminal&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WORKSFORME&target_milestone=3.1.1&cmdtype=doit">
  this query</a> to show the full list of API related updates since TM 3.1
  <!--
  , and
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi%5D&product=Target+Management&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&cmdtype=doit">
  this query</a> to show the list of additional API changes proposed for TM 3.1
  -->
  .
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
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=279837">bug 279837</a> - maj - [shells] RemoteCommandShellOperation can miss output</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=271015">bug 271015</a> - maj - [ftp] "My Home" with "Empty list" doesn't refresh</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=268463">bug 268463</a> - maj - [ssh] [sftp] SftpFileService.getFile(...) fails with cryptic jsch exception (4)</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=260796">bug 260796</a> - maj - [ftp] Fetching folder job sometimes runs forever with Outpost firewall</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=248913">bug 248913</a> - maj - [ssh] SSH subsystem loses connection</li> 
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=238156">bug 238156</a> - maj - Export/Import Connection doesn't create default filters for the specified connection</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=226564">bug 226564</a> - maj - [efs] Deadlock while starting dirty workspace
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=222380">bug 222380</a> - maj - [persistence][migration][team] Subsystem association is lost when creating connection with an installation that does not have subsystem impl</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=218387">bug 218387</a> - maj - [efs] Eclipse hangs on startup of a Workspace with a large efs-shared file system on a slow connection</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=208185">bug 208185</a> - maj - [terminal][serial] terminal can hang the UI when text is entered while the backend side is not reading characters</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=198395">bug 198395</a> - maj - [dstore] Can connect to DStore with expired password</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=175300">bug 175300</a> - maj - [performance] processes.shell.linux subsystem is slow over ssh</li>
</ul>
<!--
<p>No major or critical bugs are known at the time of release.
-->
Use 
<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&product=Target+Management&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&bug_severity=blocker&bug_severity=critical&bug_severity=major&cmdtype=doit">this query</a>
for an up-to-date list of major or critical bugs.</p>

<p>The 
<a href="http://wiki.eclipse.org/TM/3.1_Known_Issues_and_Workarounds">
TM 3.1 Known Issues and Workarounds</a> Wiki page gives an up-to-date list
of the most frequent and obvious problems, and describes workarounds for them.<br/>
If you have other questions regarding TM or RSE, please check the
<a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">TM and RSE FAQ</a>
</p>

<p>Click 
<a href="https://bugs.eclipse.org/bugs/report.cgi?x_axis_field=bug_severity&y_axis_field=op_sys&z_axis_field=&query_format=report-table&product=Target+Management&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&format=table&action=wrap">here</a>
for a complete up-to-date bugzilla status report, or
<a href="https://bugs.eclipse.org/bugs/report.cgi?x_axis_field=bug_severity&y_axis_field=op_sys&z_axis_field=&query_format=report-table&product=Target+Management&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&format=table&action=wrap">here</a>
for a report on bugs fixed so far.
</p>
</td></tr></tbody></table>

</body>
</html>
