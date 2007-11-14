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
# This must be run in $HOME/ws3 in order for the mkTestUpdateSite.sh
# script to find the published packages
#
# Bootstrapping: Get this script by
# wget -O setup.sh "http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.tm.rse/releng/org.eclipse.rse.build/setup.sh?rev=HEAD&cvsroot=DSDP_Project&content-type=text/plain"
# sh setup.sh
# ./doit_ibuild.sh
# cd testPatchUpdates/bin
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
if [ ! -f eclipse/plugins/org.eclipse.swt_3.3.1.v3346j.jar ]; then
  curdir2=`pwd`
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -d eclipse-3.3.1-${ep_arch} ]; then
      rm -rf eclipse-3.3.1-${ep_arch}
    fi
    mkdir eclipse-3.3.1-${ep_arch}
    cd eclipse-3.3.1-${ep_arch}
  else
    rm -rf eclipse
  fi
  # Eclipse SDK 3.3.1: Need the SDK so we can link into docs
  echo "Getting Eclipse SDK..."
  wget "http://download.eclipse.org/eclipse/downloads/drops/R-3.3.1-200709211145/eclipse-SDK-3.3.1-${ep_arch}.tar.gz"
  tar xfvz eclipse-SDK-3.3.1-${ep_arch}.tar.gz
  rm eclipse-SDK-3.3.1-${ep_arch}.tar.gz
  cd "${curdir2}"
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -e eclipse ]; then 
      rm eclipse
    fi
    ln -s eclipse-3.3.1-${ep_arch}/eclipse eclipse
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
if [ ! -f eclipse/plugins/org.eclipse.cdt.core_4.0.1.200709241202.jar ]; then
  # CDT 4.0.1 Runtime
  echo "Getting CDT Runtime..."
  wget "http://download.eclipse.org/tools/cdt/releases/europa/dist/cdt-master-4.0.1.zip"
  CDTTMP=`pwd`/tmp.$$
  mkdir ${CDTTMP}
  cd ${CDTTMP}
  unzip ../cdt-master-4.0.1.zip
  cd ..
  CMD="java -jar eclipse/plugins/org.eclipse.equinox.launcher_1.0.1.R33x_v20070828.jar"
  CMD="${CMD} -application org.eclipse.update.core.standaloneUpdate"
  CMD="${CMD} -command install"
  CMD="${CMD} -from file://${CDTTMP}"
  CMD="${CMD} -featureId org.eclipse.cdt"
  CMD="${CMD} -version 4.0.1.200709241202"
  ${CMD}
  rc=$?
  if [ ${rc} != 0 ]; then
    echo "${CMD}"
    exit 1
  fi  
  rm -rf ${CDTTMP}
  rm cdt-master-4.0.1.zip
fi
if [ ! -f eclipse/plugins/org.eclipse.emf.ecore.editor_2.3.1.v200709252135.jar ]; then
  # Need EMF 2.3.0 SDK for Service Discovery ISV Docs Backlinks
  echo "Getting EMF SDK..."
  wget "http://download.eclipse.org/modeling/emf/emf/downloads/drops/2.3.1/R200709252135/emf-sdo-xsd-SDK-2.3.1.zip"
  unzip -o emf-sdo-xsd-SDK-2.3.1.zip
  rm emf-sdo-xsd-SDK-2.3.1.zip 
fi
if [ ! -f eclipse/plugins/org.junit_3.8.2.v200706111738/junit.jar ]; then
  # Eclipse Test Framework
  echo "Getting Eclipse Test Framework..."
  wget "http://download.eclipse.org/eclipse/downloads/drops/R-3.3.1-200709211145/eclipse-test-framework-3.3.1.zip"
  unzip -o eclipse-test-framework-3.3.1.zip
  rm eclipse-test-framework-3.3.1.zip
fi

# checkout the basebuilder
baseBuilderTag=v20070614
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
  cvs -q update -dPR -r R2_0_maintenance 
  cd ..
else
  if [ -d org.eclipse.rse.build ]; then
    echo "Re-getting org.eclipse.rse.build from CVS"
    rm -rf org.eclipse.rse.build
  else
    echo "Getting org.eclipse.rse.build from CVS"
  fi
  cvs -q -d :pserver:anonymous@dev.eclipse.org:/cvsroot/dsdp co -Rd -r R2_0_maintenance org.eclipse.rse.build org.eclipse.tm.rse/releng/org.eclipse.rse.build
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
if [ ! -d testPatchUpdates ]; then
  D=/home/data/httpd/download.eclipse.org/dsdp/tm/testPatchUpdates
  if [ -d ${D} ]; then ln -s ${D} testPatchUpdates; else mkdir testPatchUpdates; fi
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
  ln -s org.eclipse.rse.build/bin/setup.sh .
fi
chmod a+x doit_irsbuild.sh doit_nightly.sh
cd org.eclipse.rse.build
chmod a+x build.pl build.rb go.sh nightly.sh setup.sh
cd ..

echo "Your build environment is now created."
echo ""
echo "Run \"./doit_irsbuild.sh I\" to create an I-build."
echo ""
echo "Test the testPatchUpdates, then copy them to updates:"
echo "cd updates"
echo "rm -rf plugins features"
echo "cp -R ../testPatchUpdates/plugins ."
echo "cp -R ../testPatchUpdates/features ."
echo "cd bin"
echo "cvs update"
echo "./mkTestUpdates.sh"

exit 0
