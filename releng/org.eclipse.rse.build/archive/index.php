<html><head>
<link rel="stylesheet" href="http://www.eclipse.org/default_style.css">
<title>Target Management Project Archived Downloads</title></head>
<body>

<table border=0 cellspacing=5 cellpadding=2 width="100%" >
  <tr> 
    <td align=left width="72%"> <font class=indextop>Target Management project<br/>archived downloads</font> 
      <br>
      <font class=indexsub>archived downloads from the TM project</font><br>
</td>
    <td width="28%"><img src="http://www.eclipse.org/images/Idea.jpg" height=86 width=120></td>
  </tr>

</table>
<table border=0 cellspacing=5 cellpadding=2 width="100%" >
	
	<tr> 
    <td align=LEFT valign=TOP colspan="2" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">Archived Downloads</font></b></td>
  </tr>
  
	<tr> <td> <p>On this
page you can find the archived 
<a href="build_types.html">builds</a> for the Remote System Explorer (RSE),
provided by the 
<a href="http://www.eclipse.org/tm/">Target Management</a> project.
Archived builds consist of older releases and not propagated to Eclipse mirrors. 
<!--
To get started run the program and go through the
user and developer documentation provided in the online help system.
-->
To get started, see the build notes provided with each drop, run the
program and go through the online user and developer documentation,
or take a look at the project's online
<a href="http://www.eclipse.org/tm/tutorial/index.php">getting started</a>
pages.

If you have problems downloading the drops, contact the 
<font face="arial,helvetica,geneva" size="-1"><a href="mailto:webmaster@eclipse.org">webmaster</a></font>.
If you have problems installing or getting the workbench to run, 
check out the 
<a href="http://wiki.eclipse.org/TM_and_RSE_FAQ">Target Management FAQ</a>,
or try posting a question to the 
<a href="http://www.eclipse.org/newsgroups">newsgroup</a>,
<a href="news://news.eclipse.org/eclipse.tm">eclipse.tm</a>.
All downloads are provided under the terms and conditions of the 
<a href="http://www.eclipse.org/legal/epl/notice.php">Eclipse.org
Software User Agreement</a> unless otherwise specified. </p>

<p>
Current builds are available 
<a href="http://download.eclipse.org/tm/downloads/index.php">here</a>.
For information about different kinds of builds look
<a href="build_types.html" target="_top">here</a>. 
</p>

<p/>
</td></tr>
	
  <tr> 
    <td align=LEFT valign=TOP colspan="2" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">Archived
      Downloads</font></b></td>
  </tr>
  </table>
  <?php
	$fileHandle = fopen("dlconfig.txt", "r");
	while (!feof($fileHandle)) {
		$aLine = fgets($fileHandle, 4096);
		parse_str($aLine);
	}
	fclose($fileHandle);
	
	for ($i = 0; $i < count($dropType); $i++) {
		$typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
	}
	
	$aDirectory = dir("drops");
	while ($anEntry = $aDirectory->read()) {

		// Short cut because we know aDirectory only contains other directories.
		if ($anEntry != "." && $anEntry!="..") {
			$aDropDirectory = dir("drops/".$anEntry);
			$fileCount = 0;
			while ($aDropEntry = $aDropDirectory->read()) {
				if (stristr($aDropEntry, ".zip") || stristr($aDropEntry, ".tar")) {
					// Count the files in the directory
					$fileCount = $fileCount + 1;
				}
			}
			//See http://at2.php.net/manual/en/class.dir.php
			$aDropDirectory->close();
			// Read the count file
			$countFile = "drops/".$anEntry."/package.count";
			$indexFile = "drops/".$anEntry."/index.php";
			if (file_exists($countFile) && file_exists($indexFile)) {
				$anArray = file($countFile);
	
				// If a match - process the directory
				if ($anArray[0] == $fileCount) {
					$parts = explode("-", $anEntry);
					if (count($parts) == 2) {

						//N-builds and I-builds	
						$datePart = $parts[0];
						$timePart = $parts[1];
						$buildtype = substr($datePart, 0, 1);
						$buckets[$buildtype][] = $anEntry;
			
						$year = substr($datePart, 1, 4);
						$month = substr($datePart, 5, 2);
						$day = substr($datePart, 7, 2);
						$hour = substr($timePart,0,2);
						$minute = substr($timePart,2,2);
						$timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
					
						$timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
			
						if ($timeStamp > $latestTimeStamp[$buildtype]) {
							$latestTimeStamp[$buildtype] = $timeStamp;
							$latestFile[$buildtype] = $anEntry;
						}

					} else if (count($parts) == 3) {

						//S-builds and R-builds
						$buckets[$parts[0]][] = $anEntry;
			
						$timePart = $parts[2];
						$year = substr($timePart, 0, 4);
						$month = substr($timePart, 4, 2);
						$day = substr($timePart, 6, 2);
						$hour = substr($timePart,8,2);
						$minute = substr($timePart,10,2);
						$timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
					
						$timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
			
						if ($timeStamp > $latestTimeStamp[$parts[0]]) {
							$latestTimeStamp[$parts[0]] = $timeStamp;
							$latestFile[$parts[0]] = $anEntry;
						}
					}
				}
			}
		}
	}
	$aDirectory->close();
 ?>
 
<table width="100%" cellspacing=0 cellpadding=3 align=center>
<td align=left>
<TABLE  width="100%" CELLSPACING=0 CELLPADDING=3>
<tr>
	<td width=\"30%\"><b>Build Type</b></td>
	<td><b>Build Name</b></td>
	<td><b>Build Date</b></td>
</tr>

<?php
	foreach($dropType as $value) {
		$prefix=$typeToPrefix[$value];
		$fileName = $latestFile[$prefix];
		echo "<tr>
			<td width=\"30%\">$value</td>";
		
		$parts = explode("-", $fileName);
		if (count($parts) == 2) {
			$buildName=$fileName;
		} else {
			$buildName=$parts[1];
		}
		
		// Uncomment the line below if we need click through licenses.
		// echo "<td><a href=license.php?license=drops/$fileName>$buildName</a></td>";
		
		// Comment the line below if we need click through licenses.
		echo "<td><a href=\"drops/$fileName/index.php\">$buildName</a></td>";
		
		echo "<td>$timeStamps[$fileName]</td>";
		echo "</tr>";
	}
?>
</table>
</table>
&nbsp;
<?php
	foreach($dropType as $value) {
		$prefix=$typeToPrefix[$value];
		echo "
		<table width=\"100%\" cellspacing=0 cellpadding=3 align=center>
		<tr bgcolor=\"#999999\">
		<td align=left width=\"30%\"><b><a name=\"$value\"><font color=\"#FFFFFF\" face=\"Arial,Helvetica\">$value";
		echo "s</font></b></a></td>
		</TR>
		<TR>
		<td align=left>
		<TABLE  width=\"100%\" CELLSPACING=0 CELLPADDING=3>
		<tr>
		<td width=\"30%\"><b>Build Name</b></td>
		<td><b>Build Date</b></td>
		</tr>";
		
		$aBucket = $buckets[$prefix];
		if (isset($aBucket)) {
			rsort($aBucket);
			foreach($aBucket as $innerValue) {
				$parts = explode("-", $innerValue);
				if (count($parts) == 2) {
					$buildName=$innerValue;
				} else {
					$buildName=$parts[1];
				}
				echo "<tr>";
				
					// Uncomment the line below if we need click through licenses.
					// echo "<td><a href=\"license.php?license=drops/$innerValue\">$buildName</a></td>";
					
					// Comment the line below if we need click through licenses.
					echo "<td><a href=\"drops/$innerValue/index.php\">$buildName</a></td>";

					echo "<td>$timeStamps[$innerValue]</td>
					</tr>";
			}
		}
		echo "</table></table>&nbsp;";
	}
?>

&nbsp;
</body></html>
