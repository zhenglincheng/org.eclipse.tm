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
<li>TM @buildId@ <b>requires Eclipse 3.3 or later for the SSH component</b>.
  Other components may work with earlier Eclipse versions, but these have not been tested.
  Platform Runtime is the minimum requirement for core RSE and Terminal.
  Discovery needs EMF, and the RemoteCDT integration needs CDT.<br>
  <b>Building</b> the RSE SSH service requires <b>Eclipse 3.4</b> or later for the fix
  of <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=224799">bug 224799</a>;
  the fix also requires 3.4 at runtime, but the code contains a backward 
  compatibility fallback to also run on Eclipse 3.3 if that particular fix
  is not required.</li>
<li>Highlights of Bugs fixed since TM 3.0:
<ul>
  <li><b>RSE Startup</b>: Some critical issues with initialization during early RSE startup were fixed
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=240991">240991</a>,
     <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=244454">244454</a>]</li>
  <li>The <b>Shell Processes Subsystem</b> can now be used correctly also on top of a Terminals subsystem only
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=239159">239159</a>,
     <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=247011">247011</a>]</li>
  <li><b>Refresh of a parent node</b> in the RSE SystemView now re-expands subnodes to the same
    state they were in before the refresh (with updated status, or course)
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=187739">187739</a>]</li>
  <li><b>Performance</b> of the RSE SystemView was drastically improved for certain situations
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=238363">238363</a>]</li>
  <li><b>RSE Useractions</b> received some important fixes
    [<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=useraction&classification=DSDP&product=Target+Management&component=RSE&target_milestone=3.0.1&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&cmdtype=doit">query</a>]</li>    
  <li><b>DStore</b> received some important fixes
    [<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=dstore&classification=DSDP&product=Target+Management&component=RSE&target_milestone=3.0.1&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&cmdtype=doit">query</a>]</li>    
  <li><b>RSE Drag and Drop</b> received some fixes to be more pervasive
    [<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=drag&classification=DSDP&product=Target+Management&component=RSE&target_milestone=3.0.1&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&cmdtype=doit">query</a>]</li>    
  <li><b>Terminals</b> can now be shared in multiple perspectives, similar to
    what the Eclipse Console provides
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=172483">172483</a>]</li>
  <li><b>Terminal Paste</b> is now possible by pressing the middle mouse button
    [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=240097">240097</a>]</li>
</ul></li>
<li>At least 118 bugs were fixed in total: Use 
  <!-- <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&component=Core&component=RSE&component=Terminal&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WONTFIX&resolution=WORKSFORME&chfieldfrom=2008-06-20&chfieldto=2008-09-17&chfield=resolution&cmdtype=doit&negate0=1&field0-0-0=target_milestone&type0-0-0=substring&value0-0-0=2.0&field0-0-1=target_milestone&type0-0-1=regexp&value0-0-1=3.0%20M%5B345%5D"> -->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&component=Core&component=RSE&component=Terminal&target_milestone=3.0.1&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WONTFIX&resolution=WORKSFORME&cmdtype=doit">
  this query</a> to show the list of bugs fixed since <!-- the last milestone, -->
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/R-3.0-200806202130/">
  TM 3.0</a>
  [<a href="http://download.eclipse.org/dsdp/tm/downloads/drops/R-3.0-200806202130/buildNotes.php">build notes</a>].</li>
<li>For details on checkins, see
  <a href="http://www.eclipse.org/dsdp/tm/searchcvs.php">TM SearchCVS</a>, the
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/N-changelog/index.html">
  RSE CVS changelog</a>, and the
  <a href="http://download.eclipse.org/dsdp/tm/downloads/drops/N-changelog/core/index.html">
  TM Core CVS changelog</a>.</li>
<li>For other questions, please check the
  <a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">TM and RSE FAQ</a>
  as well as the
  <a href="http://wiki.eclipse.org/TM_3.0_Known_Issues_and_Workarounds">
  TM 3.0 Known Issues and Workarounds</a>.</li>
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
<p>No API changes are allowed in the TM 3.0.x maintenance stream.
Therefore, <b>TM 3.0.x is fully upward and backward compatible with TM 3.0</b>,
and can be fully exchanged for TM 3.0 in any product based on it.
Take care of API specification updates though, where the TM 3.0.x API Docs
have been updated to add clarifications or missing information compared
to 3.0.</p>

<p>For the upcoming TM 3.1 release, only backward compatible API changes
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
		<font face="Arial,Helvetica" color="#FFFFFF">API Specification Updates since TM 3.0</font></b></td>
	</tr>
</table>
<table><tbody><tr><td>
The following lists amendments to API specifications that are worth noticing.
More information can be found in the associated bugzilla items.

<ul>
<li>TM @buildId@ API Specification Updates
<ul>
<li><b><a href="http://dsdp.eclipse.org/help/latest/topic/org.eclipse.rse.doc.isv/reference/api/org/eclipse/rse/services/files/IFileService.html#createFolder(java.lang.String,%20java.lang.String,%20org.eclipse.core.runtime.IProgressMonitor)">
   IFileService#createFolder()</a></b>
   now specifies that parent folders <i>may</i> be created by
   the implementation. Note that this clarification required additional code in
   FileServiceSubSystem in order to make <tt>
   <a href="http://dsdp.eclipse.org/help/latest/topic/org.eclipse.rse.doc.isv/reference/api/org/eclipse/rse/subsystems/files/core/subsystems/IRemoteFileSubSystem.html#createFolder(org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile,%20org.eclipse.core.runtime.IProgressMonitor)">
   IRemoteFileSubSystem.createFolder()</a></tt> and <tt>
   <a href="http://dsdp.eclipse.org/help/latest/topic/org.eclipse.rse.doc.isv/reference/api/org/eclipse/rse/subsystems/files/core/subsystems/IRemoteFileSubSystem.html#createFolders(org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile,%20org.eclipse.core.runtime.IProgressMonitor)">
   IRemoteFileSubSystem.createFolders()</a></tt>
   behave as expected for both parent folders created or not.<br/>
   Service implementations that need to be compatible with both TM 3.0 and TM 3.0.1 <b><i>should</i></b>
   create parent folders in the file service. Subsystem clients that need to be compatible with both
   TM 3.0 and TM 3.0.1 <b><i>should not</i></b> expect that parent folders are created
   [<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=234026">234026</a>].</li> 
</ul>
</li>
</ul>

Use 
  <!-- 
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi&classification=DSDP&product=Target+Management&component=Core&component=RSE&component=Terminal&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WORKSFORME&chfieldfrom=2008-06-20&chfieldto=2008-09-20&chfield=resolution&cmdtype=doit">
   -->
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi&classification=DSDP&product=Target+Management&component=Core&component=RSE&component=Terminal&bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&resolution=FIXED&resolution=WORKSFORME&target_milestone=3.0.1&cmdtype=doit">
  this query</a> to show the full list of API related updates since TM 3.0
  <!--
  , and
  <a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=%5Bapi%5D&classification=DSDP&product=Target+Management&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&cmdtype=doit">
  this query</a> to show the list of additional API changes proposed for TM 3.0
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
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=247059">bug 247059</a> - maj - [dstore] RSE AIX:SSL Connections work with EXPIRED certificate</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=245260">bug 245260</a> - maj - Different user's connections on a single ftp host are mapped to the same temp files cache</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=244070">bug 244070</a> - maj - [dstore] DStoreHostShell#exit() does not terminate child processes</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=238156">bug 238156</a> - maj - Export/Import Connection doesn't create default filters for the specified connection</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=236443">bug 236443</a> - maj - [releng] Using P2 to install "remotecdt" only from update site creates an unusable installation</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=226564">bug 226564</a> - maj - [efs] Deadlock while starting dirty workspace
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=222380">bug 222380</a> - maj - [persistence][migration][team] Subsystem association is lost when creating connection with an installation that does not have subsystem impl</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=218387">bug 218387</a> - maj - [efs] Eclipse hangs on startup of a Workspace with a large efs-shared file system on a slow connection</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=208185">bug 208185</a> - maj - [terminal][serial] terminal can hang the UI when text is entered while the backend side is not reading characters</li>
  <li><a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=198395">bug 198395</a> - maj - [dstore] Can connect to DStore with expired password</li>
</ul>
<!--
<p>No major or critical bugs are known at the time of release.
-->
Use 
<a href="https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&classification=DSDP&product=Target+Management&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&bug_severity=blocker&bug_severity=critical&bug_severity=major&cmdtype=doit">this query</a>
for an up-to-date list of major or critical bugs.</p>

<p>The 
<a href="http://wiki.eclipse.org/TM_3.0_Known_Issues_and_Workarounds">
TM 3.0 Known Issues and Workarounds</a> Wiki page gives an up-to-date list
of the most frequent and obvious problems, and describes workarounds for them.<br/>
If you have other questions regarding TM or RSE, please check the
<a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">TM and RSE FAQ</a>
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
