#!/bin/sh
#*******************************************************************************
# Copyright (c) 2006, 2007 Wind River Systems, Inc.
# All rights reserved. This program and the accompanying materials 
# are made available under the terms of the Eclipse Public License v1.0 
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html 
# 
# Contributors: 
# Martin Oberhuber - initial API and implementation 
#*******************************************************************************
#
# setup.sh : Set up an environment for building TM / RSE
# Works on build.eclipse.org -- may need to be adjusted
# for other hosts.
#
# This must be run in $HOME/ws2_0_patches in order for the mkTestUpdateSite.sh
# script to find the published packages
#
# Bootstrapping: Get this script by
# wget -O setup.sh "http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.tm.rse/releng/org.eclipse.rse.build/setup.sh?rev=HEAD&cvsroot=DSDP_Project&content-type=text/plain"
# sh setup.sh
# ./doit_ibuild.sh
# cd testUpdates/bin
# mkTestUpdates.sh

curdir=`pwd`

uname_s=`uname -s`
uname_m=`uname -m`
case ${uname_s}${uname_m} in
  Linuxppc*) ep_arch=linux-gtk-ppc
             cdt_arch=linux.ppc
             ;;
  Linuxx86_64*) ep_arch=linux-gtk-x86_64 
                cdt_arch=linux.x86_64
                ;;
  Linuxx86*) ep_arch=linux-gtk
             cdt_arch=linux.x86
             ;;
esac

# prepare the base Eclipse installation in folder "eclipse"
if [ ! -f eclipse/plugins/org.eclipse.swt_3.3.0.v3346.jar ]; then
  curdir2=`pwd`
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -d eclipse-3.3-${ep_arch} ]; then
      rm -rf eclipse-3.3-${ep_arch}
    fi
    mkdir eclipse-3.3-${ep_arch}
    cd eclipse-3.3-${ep_arch}
  else
    rm -rf eclipse
  fi
  ## Eclipse Platform 3.3
  #wget "http://download.eclipse.org/eclipse/downloads/drops/R-3.3-200706251500/eclipse-platform-3.3-${ep_arch}.tar.gz"
  #tar xfvz eclipse-platform-3.3-${ep_arch}.tar.gz
  #rm eclipse-platform-3.3-${ep_arch}.tar.gz
  # Eclipse SDK 3.3: Need the SDK so we can link into docs
  echo "Getting Eclipse SDK..."
  wget "http://download.eclipse.org/eclipse/downloads/drops/R-3.3-200706251500/eclipse-SDK-3.3-${ep_arch}.tar.gz"
  echo "Extracting eclipse-SDK..."
  tar xfz eclipse-SDK-3.3-${ep_arch}.tar.gz
  rm eclipse-SDK-3.3-${ep_arch}.tar.gz
  cd "${curdir2}"
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -e eclipse ]; then 
      rm eclipse
    fi
    ln -s eclipse-3.3-${ep_arch}/eclipse eclipse
  fi
fi
if [ ! -f eclipse/startup.jar ]; then
  curdir2=`pwd`
  cd eclipse/plugins
  if [ -h ../startup.jar ]; then
    rm ../startup.jar
  fi
  LAUNCHER=`ls org.eclipse.equinox.launcher*.jar | sort | tail -1`
  if [ "${LAUNCHER}" != "" ]; then
    echo "eclipse LAUNCHER=${LAUNCHER}" 
    ln -s plugins/${LAUNCHER} ../startup.jar
  else
    echo "Eclipse: NO startup.jar LAUNCHER FOUND!"
  fi
  cd ${curdir2}
fi
if [ ! -f eclipse/plugins/org.eclipse.cdt.core_4.0.0.200706261300.jar ]; then
  # CDT 4.0.0 Runtime
  echo "Getting CDT Runtime..."
  CDT_OK=0
  wget "http://download.eclipse.org/tools/cdt/releases/europa/dist/cdt-master-4.0.0.zip"
  #java \
  #  -classpath eclipse/plugins/org.eclipse.help.base_3.3.0.v20070606.jar \
  CDTTMP=`pwd`/tmp.$$
  mkdir ${CDTTMP}
  cd ${CDTTMP}
  echo "Extracting CDT-master..."
  unzip -q ../cdt-master-4.0.0.zip
  cd ..
  CMD="java -jar eclipse/plugins/org.eclipse.equinox.launcher_1.0.0.v20070606.jar"
  CMD="${CMD} -application org.eclipse.update.core.standaloneUpdate"
  LCMD="${CMD} -command search -from file://${CDTTMP}"
  echo ""
  echo ${LCMD}
  result=`${LCMD} | grep 'org.eclipse.cdt 4'`
  rc=$?
  echo "rc=${rc}"
  echo $result
  if [ "${rc}" = "0" ]; then
    VERSION=`echo ${result} | sed -e 's,".*org\.eclipse\.cdt ,,'`
    echo "VERSION=${VERSION}"
    UCMD="${CMD} -command install"
    UCMD="${UCMD} -featureId org.eclipse.cdt"
    UCMD="${UCMD} -version 4.0.0.200706261300"
    UCMD="${UCMD} -from file://${CDTTMP}"
    echo ""
    echo ${UCMD}
    ${UCMD}
    rc=$?
    echo "rc=${rc}"
    if [ "${rc}" = "0" ]; then
      rm -rf ${CDTTMP}
      rm cdt-master-4.0.0.zip
      CDT_OK=1
    fi
  fi
  if [ ${CDT_OK} != 1 ]; then
    echo "CDT Install failed!"
    exit 1
  fi
fi
if [ ! -f eclipse/plugins/org.eclipse.emf.doc_2.3.0.v200706262000/doc.zip ]; then
  # Need EMF 2.3.0 SDK for Service Discovery ISV Docs Backlinks
  echo "Getting EMF SDK..."
  wget "http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.3.0/R200706262000/emf-sdo-xsd-SDK-2.3.0.zip"
  echo "Extracting EMF..."
  unzip -q -o emf-sdo-xsd-SDK-2.3.0.zip
  rm emf-sdo-xsd-SDK-2.3.0.zip 
fi
if [ ! -f eclipse/plugins/org.junit_3.8.2.v200706111738/junit.jar ]; then
  # Eclipse Test Framework
  echo "Getting Eclipse Test Framework..."
  wget "http://download.eclipse.org/eclipse/downloads/drops/R-3.3-200706251500/eclipse-test-framework-3.3.zip"
  echo "Extracting eclipse-test-framework..."
  unzip -q -o eclipse-test-framework-3.3.zip
  rm eclipse-test-framework-3.3.zip
fi

# checkout the basebuilder
baseBuilderTag=RC4_33
if [ ! -f org.eclipse.releng.basebuilder/plugins/org.eclipse.pde.core_3.3.0.v20070608-1300.jar \
  -o ! -f org.eclipse.releng.basebuilder/plugins/org.eclipse.pde.build/pdebuild.jar ]; then
  if [ -d org.eclipse.releng.basebuilder ]; then
    echo "Re-getting basebuilder from CVS..."
    rm -rf org.eclipse.releng.basebuilder
  else
    echo "Getting basebuilder from CVS..."
  fi
  cvs -Q -d :pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse co -r ${baseBuilderTag} org.eclipse.releng.basebuilder
fi
if [ ! -f org.eclipse.releng.basebuilder/startup.jar ]; then
  curdir2=`pwd`
  cd org.eclipse.releng.basebuilder/plugins
  if [ -h ../startup.jar ]; then
    rm ../startup.jar
  fi
  LAUNCHER=`ls org.eclipse.equinox.launcher*.jar | sort | tail -1`
  if [ "${LAUNCHER}" != "" ]; then
    echo "basebuilder: LAUNCHER=${LAUNCHER}" 
    ln -s plugins/${LAUNCHER} ../startup.jar
  else 
    echo "basebuilder: NO LAUNCHER FOUND"
  fi
  cd ${curdir2}
fi

# checkout the RSE builder
if [ -f org.eclipse.rse.build/CVS/Entries ]; then
  echo "Updating org.eclipse.rse.build from CVS"
  cd org.eclipse.rse.build
  cvs -q update -dPR -r R2_0_patches
  cd ..
else
  if [ -d org.eclipse.rse.build ]; then
    echo "Re-getting org.eclipse.rse.build from CVS"
    rm -rf org.eclipse.rse.build
  else
    echo "Getting org.eclipse.rse.build from CVS"
  fi
  cvs -q -d :pserver:anonymous@dev.eclipse.org:/cvsroot/dsdp co -R -r R2_0_patches -d org.eclipse.rse.build org.eclipse.tm.rse/releng/org.eclipse.rse.build
fi

# prepare directories for the build
echo "Preparing directories and symbolic links..."
if [ ! -d working/package ]; then
  mkdir -p working/package
fi
if [ ! -d working/build ]; then
  mkdir -p working/build
fi
if [ ! -d publish ]; then
  D=/home/data/httpd/download.eclipse.org/dsdp/tm/downloads/drops
  if [ -d ${D} ]; then ln -s ${D} publish; else mkdir publish; fi
fi
if [ ! -d testUpdates ]; then
  D=/home/data/httpd/download.eclipse.org/dsdp/tm/testUpdates
  if [ -d ${D} ]; then ln -s ${D} testUpdates; else mkdir testUpdates; fi
fi
if [ ! -d updates ]; then
  D=/home/data/httpd/download.eclipse.org/dsdp/tm/updates
  if [ -d ${D} ]; then ln -s ${D} updates; else mkdir updates; fi
fi
if [ ! -d staging ]; then
  D=/home/data/httpd/download-staging.priv/dsdp/tm
  if [ -d ${D} ]; then ln -s ${D} staging; else mkdir staging; fi
fi

# create symlinks as needed
if [ ! -h doit_irsbuild.sh ]; then
  ln -s org.eclipse.rse.build/bin/doit_irsbuild.sh .
fi
if [ ! -h doit_nightly.sh ]; then
  ln -s org.eclipse.rse.build/bin/doit_nightly.sh .
fi
if [ ! -h setup.sh ]; then
  if [ -f setup.sh ]; then rm -f setup.sh; fi
  ln -s org.eclipse.rse.build/setup.sh .
fi
chmod a+x doit_irsbuild.sh doit_nightly.sh
cd org.eclipse.rse.build
chmod a+x build.pl build.rb go.sh nightly.sh setup.sh
cd ..

echo "Your build environment is now created."
echo ""
echo "Run \"./doit_irsbuild.sh I\" to create an I-build."
echo ""
echo "Test the testUpdates, then copy them to updates:"
echo "cd updates"
echo "rm -rf plugins features"
echo "cp -R ../testUpdates/plugins ."
echo "cp -R ../testUpdates/features ."
echo "cd bin"
echo "cvs update"
echo "./mkTestUpdates.sh"

exit 0
