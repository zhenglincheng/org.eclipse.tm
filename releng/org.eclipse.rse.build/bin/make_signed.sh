#!/bin/sh
#*******************************************************************************
#  Copyright (c) 2008, 2009 Wind River Systems, Inc. and others.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
# 
#  Contributors:
#  Martin Oberhuber (Wind River) - initial API and implementation
#*******************************************************************************
#
# Usage: make sure that appropriate signed update site is referenced in UPDATE_SITE,
# and current directory is a download. Will create signed downloads in subdirectory.
#

curdir=`pwd`
cd `dirname $0`
mydir=`pwd`
cd "${curdir}"

if [ "${UPDATE_SITE}" = "" ]; then
  UPDATE_SITE=$HOME/downloads-tm/updates/3.1milestones
fi
if [ "${SIGNED_JAR_SOURCE}" = "" ]; then
  SIGNED_JAR_SOURCE=${curdir}/eclipse_ext/tm
fi
if [ "${BASEBUILDER}" = "" ]; then
  BASEBUILDER=$HOME/ws_31x/eclipse
fi
if [ "${DROPDIR}" = "" ]; then
  DROPDIR=${curdir}
fi
have_sdk=`ls ${DROPDIR} | grep 'RSE-SDK.*zip$'`
if [ "${have_sdk}" = "" ]; then
  echo "No drop found in DROPDIR. Please cd to your drop, or setenv DROPDIR."
  exit 1
fi
echo "UPDATE_SITE: ${UPDATE_SITE}"
echo "SIGNED_JAR_SOURCE: ${SIGNED_JAR_SOURCE}"
echo "BASEBUILDER: ${BASEBUILDER}"
echo "DROPDIR: ${DROPDIR}"

#Use Java5 on build.eclipse.org
#export PATH=/shared/dsdp/tm/ibm-java2-ppc64-50/bin:$PATH
export PATH=/shared/dsdp/tm/ibm-java2-ppc64-50/jre/bin:/shared/dsdp/tm/ibm-java2-ppc64-50/bin:$PATH
#export PATH=${HOME}/s/IBMJava2-ppc-142/bin:$PATH

OUTPUT=${curdir}/output.$$
RESULT=${curdir}/result.$$
TMPD=${curdir}/tmp.$$

# Provision update site into SIGNED_JAR_SOURCE
if [ ! -d "${SIGNED_JAR_SOURCE}/eclipse" ]; then
  mkdir -p "${SIGNED_JAR_SOURCE}/eclipse"
  echo "Provisioning with repo2runnable..."
  ${BASEBUILDER}/eclipse -nosplash \
    -data install-ws -consolelog -clean \
    -application org.eclipse.equinox.p2.repository.repo2runnable \
    -source file:${UPDATE_SITE} \
    -destination file:${SIGNED_JAR_SOURCE}/eclipse \
    -vmargs \
      -Xms128M -Xmx256M -XX:PermSize=128M -XX:MaxPermSize=256M 
  retval=$?
  echo "result: ${retval}"
fi

if [ ! -d ${SIGNED_JAR_SOURCE}/server ]; then
  mkdir ${SIGNED_JAR_SOURCE}/server
fi
have_server=`ls "${SIGNED_JAR_SOURCE}"/server/*.jar 2>/dev/null`
if [ "${have_server}" = "" ]; then
  signed_server=""
  if [ -d "${DROPDIR}/signed" ]; then
    signed_server=`ls ${DROPDIR}/signed | grep 'rseserver-.*\.zip'`
  fi
  if [ "${signed_server}" = "" ]; then
    win_server=`ls ${DROPDIR} | grep 'rseserver-.*-windows\.zip'`
    if [ "${win_server}" = "" ]; then
      echo "Error: No rseserver found in DROPDIR."
      exit 1
    fi
    if [ ! -d /home/data/httpd/download-staging.priv/dsdp/tm ]; then
      echo "Error: Must run on build.eclipse.org to sign"
    fi
    cd /home/data/httpd/download-staging.priv/dsdp/tm
    SIGN_TMP=tmp.$$
    mkdir ${SIGN_TMP}
    cd ${SIGN_TMP}
    cp ${DROPDIR}/${win_server} .
    sign ${win_server} nomail `pwd`/out
    while [ ! -f out/${win_server} ]; do
      echo "Waiting for sign..."
      sleep 30
    done
    unzip out/${win_server} clientserver.jar
    result=`jarsigner -verify clientserver.jar | head -1`
    while [ "$result" != "jar verified." ]; do
      echo "Waiting for verification..."
      sleep 30
      unzip out/${win_server} clientserver.jar
      result=`jarsigner -verify clientserver.jar | head -1`
    done
    signed_server=`echo ${win_server} | sed -e 's,-windows,-windows-signed,'`
    echo "Signing OK, copy to ${DROPDIR}/signed/${signed_server}"
    mkdir -p ${DROPDIR}/signed
    cp out/${win_server} ${DROPDIR}/signed/${signed_server}
    cd ..
    rm -rf ${SIGN_TMP}
  fi
  cd ${SIGNED_JAR_SOURCE}/server
  unzip ${DROPDIR}/signed/${signed_server}
  have_server=`ls *.jar 2>/dev/null`
  cd "${curdir}"
  if [ "${have_server}" = "" ]; then
    echo 'ERROR: signed rseserver-*.zip not found!'
    echo "Please sign a server zip on build.eclipse.org, upload and retry."
    exit 1
  fi
fi

if [ ! -d ${TMPD} ]; then
  mkdir -p ${TMPD} 
fi
if [ ! -d ${OUTPUT} ]; then
  mkdir -p ${OUTPUT} 
fi
if [ ! -d ${RESULT} ]; then
  mkdir -p ${RESULT} 
fi
cd ${DROPDIR}
for zip in `ls *.zip *.tar` ; do
  cd ${TMPD}
  case ${zip} in
    *.zip) unzip -q ${DROPDIR}/${zip} ;;
    *.tar) tar xf ${DROPDIR}/${zip} ;;
  esac
  case ${zip} in
    rseserver*) SIGNED_JARS=${SIGNED_JAR_SOURCE}/server ;;
    *) SIGNED_JARS=${SIGNED_JAR_SOURCE} ;;
  esac
  REF=`find . -name 'epl-v10.html'`
  FILES=`find . -name '*.jar' -o -name 'META-INF'`
  for f in ${FILES} ; do
    printf "${f}: "
    if [ -f ${SIGNED_JARS}/${f} ]; then
      cp -f ${SIGNED_JARS}/${f} ./${f}
      touch -r ${REF} ./${f}
      echo "signed"
    elif [ -d ${SIGNED_JARS}/${f} ]; then
      cp -Rf ${SIGNED_JARS}/${f}/* ${f}
      touch -r ${REF} ${f}/*
      echo "signed"
    else
      echo "."
    fi
  done
  ##cp ${curdir}/${zip} ${OUTPUT}
  case ${zip} in
    *.zip) zip -r -o -q ${OUTPUT}/${zip} * ;;
    *.tar) tar cfv ${OUTPUT}/${zip} * ; touch -r ${REF} ${OUTPUT}/${zip};
  esac
  rm -rf *
  signed_server=`ls "${OUTPUT}" | grep 'rseserver-.*-signed\.zip'`
  if [ "${signed_server}" != "" ]; then
    rm "${OUTPUT}/${signed_server}"
  fi
  cd ${RESULT}
  case ${zip} in
     rseserver*) mkdir ${zip} ; cd ${zip} ;
        case ${zip} in
           *.zip) unzip -q -o ${OUTPUT}/${zip} ;;
           *.tar) tar xf ${OUTPUT}/${zip} ;;
        esac
        ;;
     *) unzip -q -o ${OUTPUT}/${zip} ;;
  esac
done
rm -rf ${TMPD}

echo "--------------------------------------"
echo "DONE"
echo "--------------------------------------"
cd "${DROPDIR}"
echo "MAIN:---------------------------------"
diff -r ${RESULT} ${SIGNED_JAR_SOURCE}
for f in `ls rseserver-*.zip rseserver-*.tar` ; do
  echo "${f}:-----------------------------------"
  diff -r -b ${RESULT}/${f} ${SIGNED_JAR_SOURCE}/server
done
cd "${curdir}"

