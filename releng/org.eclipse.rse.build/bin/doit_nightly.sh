#!/bin/sh
#*******************************************************************************
# Copyright (c) 2006, 2011 Wind River Systems, Inc.
# All rights reserved. This program and the accompanying materials 
# are made available under the terms of the Eclipse Public License v1.0 
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html 
# 
# Contributors: 
# Martin Oberhuber - initial API and implementation 
#*******************************************************************************
#Bootstrapping script to perform N-builds on build.eclipse.org

#nothing we do should be hidden from the world
umask 22

#Use Java5 on build.eclipse.org
#export PATH=/shared/tools/tm/jdk-1.5/bin:$PATH
export PATH=/shared/tools/tm/jdk-1.5/jre/bin:/shared/tools/tm/jdk-1.5/bin:$PATH
#export PATH=${HOME}/ws2/IBMJava2-ppc-142/bin:$PATH

curdir=`pwd`

#Remove old logs and builds
echo "Removing old logs and builds..."
cd $HOME/ws_31x
rm log-N*.txt
if [ -d working/build ]; then
  rm -rf working/build
fi
if [ -d working/package ]; then
  rm -rf working/package
fi

#Do the main job
echo "Updating builder from CVS..."
cd org.eclipse.rse.build
stamp=`date +'%Y%m%d-%H%M'`
log=$HOME/ws_31x/log-N$stamp.txt
touch $log
cvs -q update -RPd >> $log 2>&1
daystamp=`date +'%Y%m%d-%H'`

echo "Running the builder..."
./nightly.sh >> $log 2>&1
tail -30 $log

#update the main download and archive pages: build.eclipse.org only
if [ -d /home/data/httpd/archive.eclipse.org/tm/downloads ]; then
  cd /home/data/httpd/archive.eclipse.org/tm/downloads
  cvs -q update -RPd >> $log 2>&1
  chgrp tools.tm * CVS/* 2>/dev/null
  cd /home/data/httpd/download.eclipse.org/tm/downloads
  cvs -q update -RPd >> $log 2>&1
  chgrp tools.tm * CVS/* 2>/dev/null

  #Fixup permissions and group id on download.eclpse.org (just to be safe)
  chgrp -R tools.tm drops/${buildType}*${daystamp}* 2>/dev/null
  chmod -R g+w drops/${buildType}*${daystamp}* 2>/dev/null
fi

#Copy latest SDK in order to give access to DOC server
cd $HOME/ws_31x/publish
if [ -d N.latest ]; then
  FILES=`ls -t N${daystamp}*/RSE-SDK-N${daystamp}*.zip | head -1 2>/dev/null`
  echo "FILES=$FILES"
  if [ "$FILES" != "" ]; then
    rm N.latest/RSE-SDK-N*.zip 2>/dev/null
    cd `dirname ${FILES}`
    cp -f RSE-SDK-N*.zip ../N.latest/RSE-SDK-latest.zip
    cp -f TM-discovery-*.zip ../N.latest/TM-discovery-latest.zip
    cd ../N.latest
    chgrp tools.tm *.zip
    chmod g+w *.zip
    if [ -d /shared/tools/tm/public_html/tm/downloads/drops/N.latest ]; then
      cp -f * /shared/tools/tm/public_html/tm/downloads/drops/N.latest/
      chmod -R g+w /shared/tools/tm/public_html/tm/downloads/drops
    fi
  fi
fi

#Cleanup old nightly builds (leave only last 5 in place)
cd $HOME/ws_31x/publish
ls -d N200* | sort | head -n-5 | xargs rm -rf

