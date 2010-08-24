#!/bin/sh
#*******************************************************************************
# Copyright (c) 2006, 2009 Wind River Systems, Inc.
# All rights reserved. This program and the accompanying materials 
# are made available under the terms of the Eclipse Public License v1.0 
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html 
# 
# Contributors: 
# Martin Oberhuber - initial API and implementation 
#*******************************************************************************
# Convert normal "site.xml" to "testPatchUpdates"
#
# Prerequisites: 
# - Eclipse 3.3Mx installed in $HOME/ws_31x/eclipse
# - Java5 in the PATH or in /shared/dsdp/tm/ibm-java2-ppc64-50

curdir=`pwd`
cd `dirname $0`
mydir=`pwd`

umask 022

#Use Java5 on build.eclipse.org - need JRE for pack200
export PATH=/shared/dsdp/tm/ibm-java2-ppc64-50/jre/bin:/shared/dsdp/tm/ibm-java2-ppc64-50/bin:$PATH
basebuilder=${HOME}/ws_31x/org.eclipse.releng.basebuilder

# patch site.xml
cd ..
SITE=`pwd`
if [ -f index.html.new ]; then
  rm -f index.html.new
fi
if [ -f site.xml.new ]; then
  rm -f site.xml.new
fi
if [ -f web/site.xsl.new ]; then
  rm -f web/site.xsl.new
fi

# get newest plugins and features: to be done manually on real update site
TPVERSION="Target Management"
VERSION=3.1.2+
TYPE=none
SITEDIR=`basename ${SITE}`
case ${SITEDIR} in
  test*Updates)   TYPE=test ;;
  signed*Updates) TYPE=testSigned ;;
  *milestones)    TYPE=milestone ;;
  *interim)       TYPE=interim ;;
  *)              TYPE=unknown ;;
esac
case ${SITEDIR} in
  3.2*)  VERSION=3.2 ;;
esac
if [ ${TYPE} = test ]; then
    echo "Working on test patch update site for ${VERSION}"
    TPTYPE="${VERSION} Test"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    REL=`ls $HOME/ws_31x/working/package | sort | tail -1`
    if [ "$REL" != "" ]; then
      echo "Checking new Updates from $REL"
      DIR="$HOME/ws_31x/working/package/$REL/updates"
      if [ -d "$DIR/features" ]; then
        echo "Copying new plugins and features from $DIR"
        rm -rf features
        rm -rf plugins
        cp -R $DIR/features .
        cp -R $DIR/plugins .
      fi
    fi
    # CHECK VERSION CORRECTNESS for MICRO UPDATES only
    # Minor/major version updates are not allowed.
    # Update of "qualifier" requires also updating "micro"
    echo "VERIFYING VERSION CORRECTNESS: Features"
    ls features/*.jar | sed -e 's,^.*features/,,' | sort > f1.$$.txt
    ls ../updates/3.1/features/*.jar | sed -e 's,^.*features/,,' | sort > f2.$$.txt
    diff f2.$$.txt f1.$$.txt | grep '^[>]' \
       | sed -e 's,[>] \(.*_[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\)\..*,\1,' > f_new.txt
    for f in `cat f_new.txt`; do
      fold=`grep "${f}\." f2.$$.txt`
      if [ "${fold}" != "" ]; then
        echo "PROBLEM: QUALIFIER update without MICRO: ${f}"
      fi
      fbase=`echo $f | sed -e 's,\(.*_[0-9][0-9]*\.[0-9][0-9]*\)\..*,\1,'`
      fold=`grep "${fbase}\." f2.$$.txt`
      if [ "${fold}" = "" ]; then
        echo "PROBLEM: MAJOR or MINOR update : ${f}"
      fi
    done
    echo "VERIFYING VERSION CORRECTNESS: Plugins"
    ls plugins/*.jar | sed -e 's,^.*plugins/,,' | sort > p1.$$.txt
    ls ../updates/3.1/plugins/*.jar | sed -e 's,^.*plugins/,,' | sort > p2.$$.txt
    diff p2.$$.txt p1.$$.txt | grep '^[>]' \
       | sed -e 's,[>] \(.*_[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\)\..*,\1,' > p_new.txt
    for p in `cat p_new.txt`; do
      pold=`grep "${p}\." p2.$$.txt`
      if [ "${pold}" != "" ]; then
        echo "PROBLEM: QUALIFIER update without MICRO: ${p}"
      fi
      pbase=`echo $p | sed -e 's,\(.*_[0-9][0-9]*\.[0-9][0-9]*\)\..*,\1,'`
      pold=`grep "${pbase}\." p2.$$.txt`
      if [ "${pold}" = "" ]; then
        echo "PROBLEM: MAJOR or MINOR update : ${p}"
      fi
    done
    #rm f_new.txt p_new.txt
    mv -f f1.$$.txt fversions.txt
    mv -f p1.$$.txt pversions.txt
    mv -f f2.$$.txt f30versions.txt
    mv -f p2.$$.txt p30versions.txt
    ## rm f1.$$.txt f2.$$.txt p1.$$.txt p2.$$.txt    
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	index.html > index.html.new
    mv -f index.html.new index.html
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
    	-e '/<!-- BEGIN_3_0 -->/,/<!-- END_3_1_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
    echo "Conditioning the site... $SITE"
    #java -Dorg.eclipse.update.jarprocessor.pack200=$mydir \
    #java -jar $HOME/ws_31x/eclipse/startup.jar \
    java \
        -jar ${basebuilder}/plugins/org.eclipse.equinox.launcher.jar \
        -application org.eclipse.update.core.siteOptimizer \
        -jarProcessor -outputDir $SITE \
        -processAll -repack $SITE
    #java -Dorg.eclipse.update.jarprocessor.pack200=$mydir \
    #	$HOME/ws_31x/jarprocessor/jarprocessor.jar \
	#	-outputDir $SITE -processAll -repack $SITE
elif [ ${TYPE} = testSigned ]; then
    echo "Working on signed patch update site for ${VERSION}"
    TPTYPE="${VERSION} Signed Test Patch"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    echo "Signing jars from test patch update site (expecting conditioned jars)..."
    STAGING=/home/data/httpd/download-staging.priv/dsdp/tm
    stamp=`date +'%Y%m%d-%H%M'`
    if [ -d ${STAGING} -a -d ${SITE}/../testPatchUpdates ]; then
      #get jars from testPatchUpdates, sign them and put them here
      mkdir ${SITE}/features.${stamp}
      mkdir -p ${STAGING}/updates.${stamp}/features
      chmod -R g+w ${STAGING}/updates.${stamp}
      cp -R ${SITE}/../testPatchUpdates/features/*.jar ${STAGING}/updates.${stamp}/features
      cd ${STAGING}/updates.${stamp}/features
      for x in `ls *.jar`; do
        result=`jarsigner -verify ${x} | head -1`
        if [ "$result" != "jar verified." ]; then
          # do not sign Orbit bundles again since they are signed already 
          echo "signing feature: ${x}"
          sign ${x} nomail >/dev/null
        fi
      done
      TRIES=10
      while [ $TRIES -gt 0 ]; do
        sleep 30
        echo "TRIES to go: ${TRIES}"
        for x in `ls *.jar | grep -v '^temp[_.]'`; do
          result=`jarsigner -verify ${x} | head -1`
          if [ "$result" = "jar verified." ]; then
            echo "${result}: ${x}"
            cp ${x} ${SITE}/features.${stamp}
            rm ${x}
          else
            echo "-pending- ${x} : ${result}" | head -1
            sleep 30
          fi
        done
        FILES=`ls 2>/dev/null`
        if [ "$FILES" = "" ]; then
          TRIES=0
          ok=1
        else
          echo "--> FILES is $FILES"
          TRIES=`expr $TRIES - 1`
          ok=0
        fi
      done
      if [ "$ok" = "1" ]; then
        rmdir ${STAGING}/updates.${stamp}/features
        mkdir ${SITE}/plugins.${stamp}
        mkdir -p ${STAGING}/updates.${stamp}/plugins
        chmod -R g+w ${STAGING}/updates.${stamp}
        cp ${SITE}/../testPatchUpdates/plugins/*.jar ${STAGING}/updates.${stamp}/plugins
        cd ${STAGING}/updates.${stamp}/plugins
        for x in `ls *.jar`; do
          result=`jarsigner -verify ${x} | head -1`
          if [ "$result" != "jar verified." ]; then
            # do not sign Orbit bundles again since they are signed already 
            echo "signing plugin: ${x}"
            sign ${x} nomail >/dev/null
          fi
        done
        TRIES=10
        while [ $TRIES -gt 0 ]; do
          sleep 30
          echo "TRIES to go: ${TRIES}"
          for x in `ls *.jar | grep -v '^temp[_.]'`; do
            result=`jarsigner -verify ${x} | head -1`
            if [ "$result" = "jar verified." ]; then
              echo "${result}: ${x}"
              cp ${x} ${SITE}/plugins.${stamp}
              rm ${x}
            else
              echo "-pending- ${x} : ${result}" | head -1
              sleep 30
            fi
          done
          FILES=`ls 2>/dev/null`
          if [ "$FILES" = "" ]; then
            TRIES=0
            ok=1
          else
            echo "--> FILES is $FILES"
            TRIES=`expr $TRIES - 1`
            ok=0
          fi
        done
      fi
      if [ "$ok" = "1" ]; then
        cd ${SITE}
        rmdir ${STAGING}/updates.${stamp}/plugins
        rmdir ${STAGING}/updates.${stamp}
        #mv features features.old.${stamp}
        #mv plugins plugins.old.${stamp}
        rm fversions.txt pversions.txt f30versions.txt p30versions.txt f_new.txt p_new.txt 2>/dev/null
        rm -rf features plugins
        mv features.${stamp} features
        mv plugins.${stamp} plugins
      else
        echo "Something went wrong during staging and signing."
        echo "Keeping existing update site intact."
        exit 1
      fi
    else
      echo "staging or testPatchUpdates not found:"
      echo "please fix your pathes"
      exit 1
    fi
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	index.html > index.html.new
    mv -f index.html.new index.html
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
    	-e '/<!-- BEGIN_3_0 -->/,/<!-- END_3_1_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
elif [ ${TYPE} = milestone ]; then
    echo "Working on milestone update site for ${VERSION}"
    TPTYPE="${VERSION} Milestone"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    echo "Expect that you copied your features and plugins yourself"
    stamp=`date +'%Y%m%d-%H%M'`
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '\,</h1>,a\
This site contains Target Management Milestones (I-, S- and M- builds) which are \
being contributed to the Galileo coordinated release train (Eclipse 3.5.x).' \
    	index.html > index.html.new
    mv -f index.html.new index.html
    ## keep 3.0.x features in site.xml
    ##	-e '/<!-- BEGIN_2_0_1 -->/,/<!-- END_2_0_4 -->/d' \
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
    	-e '/<!-- BEGIN_3_0 -->/,/<!-- END_3_1_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
elif [ ${TYPE} = interim ]; then
    echo "Working on interim update site for ${VERSION}"
    TPTYPE="${VERSION} Interim"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    echo "Expect that you copied your features and plugins yourself"
    stamp=`date +'%Y%m%d-%H%M'`
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '\,</h1>,a\
This site contains Target Management Interim Maintenance builds (I-, S-, and M-builds) in order \
to test them before going live.' \
    	index.html > index.html.new
    mv -f index.html.new index.html
    ## keep 2.0.x features in site.xml
    ##	-e '/<!-- BEGIN_2_0_1 -->/,/<!-- END_2_0_4 -->/d' \
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
    	-e '/<!-- BEGIN_3_0 -->/,/<!-- END_3_1_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
elif [ `basename $SITE` = 3.0 ]; then
    echo "Working on 3.0 update site"
    TPTYPE="3.0"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    TYPE=official
    echo "Expect that you copied your features and plugins yourself"
    stamp=`date +'%Y%m%d-%H%M'`
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '\,</h1>,a\
This site contains Target Management 3.0 Releases and Updates (R- builds) which are \
being contributed to the Ganymede coordinated release train (Eclipse 3.4).' \
    	index.html > index.html.new
    mv -f index.html.new index.html
    ## dont keep 2.0.x features in site.xml -- this site has 3.0 and 3.1 content
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
    	-e '/<!-- BEGIN_3_2 -->/,/<!-- END_3_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
elif [ `basename $SITE` = 3.1 ]; then
    echo "Working on 3.1 update site"
    TPTYPE="3.1"
    TPVERSION="${TPVERSION} ${TPTYPE}"
    TYPE=official
    echo "Expect that you copied your features and plugins yourself"
    stamp=`date +'%Y%m%d-%H%M'`
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
    	-e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '\,</h1>,a\
This site contains Target Management 3.1 Releases and Updates (R- builds) which are \
being contributed to the Galileo coordinated release train (Eclipse 3.5.x).' \
    	index.html > index.html.new
    mv -f index.html.new index.html
    ## dont keep 2.0.x and 3.0.x features in site.xml
    ## this site has 3.1.x content
    sed -e "s,/dsdp/tm/updates/2.0,/dsdp/tm/updates/${SITEDIR},g" \
        -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	-e '/<!-- BEGIN_2_0 -->/,/<!-- END_2_0_4 -->/d' \
        -e '/<!-- BEGIN_3_0 -->/,/<!-- END_3_0_3 -->/d' \
        -e '/<!-- BEGIN_3_1_2plus -->/,/<!-- END_3_1_2plus -->/d' \
        -e '/<!-- BEGIN_3_2 -->/,/<!-- END_3_2 -->/d' \
        site.xml > site.xml.new
    mv -f site.xml.new site.xml
    sed -e "s,Project 2.0 Update,Project ${TPTYPE} Update,g" \
    	web/site.xsl > web/site.xsl.new
    mv -f web/site.xsl.new web/site.xsl
else
    echo "Working on official update site"
    TYPE=official
    echo "Expect that you copied your features and plugins yourself"
    stamp=`date +'%Y%m%d-%H%M'`
    rm index.html site.xml web/site.xsl
    cvs -q update -dPR
    sed -e '/<!-- BEGIN_2_0_5 -->/,/<!-- END_2_0_5 -->/d' \
        site.xml > site.xml.new1
    sed -e '/<!-- BEGIN_3_0_4 -->/,/<!-- END_3_0_4 -->/d' \
        site.xml.new1 > site.xml.new2
    sed -e '/<!-- BEGIN_3_2 -->/,/<!-- END_3_2 -->/d' \
        site.xml.new2 > site.xml.new
    mv -f site.xml.new site.xml
    rm site.xml.new1 site.xml.new2
fi
FEATURES=`grep 'features/[^ ]*\.qualifier\.jar' site.xml | sed -e 's,^[^"]*"features/\([^0-9]*[0-9][0-9.]*\).*$,\1,g'`
for feature in $FEATURES ; do
  #list newest ones first
  TAG=`ls -t features/${feature}*.jar | head -1 | sed -e 's,[^0-9]*[0-9][0-9]*\.[0-9]*\.[0-9]*\.\([^.]*\).jar,\1,'`
  if [ "$TAG" != "" ]; then
    echo "$feature : $TAG"
    sed -e "/$feature/s/qualifier/$TAG/g" site.xml > site.xml.new
    mv -f site.xml.new site.xml
  fi
done
#Create Europa version of site.xml
if [ -f site-europa.xml ]; then
  rm -rf site-europa.xml
fi
sed -e '/!EUROPA_ONLY!/d' site.xml > site-europa.xml

#Get rid of Europa comments completely in order to avoid SAX exception 
#in comment when the feature qualifier extends to --
awk 'BEGIN {doit=1}
  /-- !EUROPA_ONLY!/ {doit=0}
  { if(doit==1) print; }
  /!EUROPA_ONLY! --/ {doit=1}' site.xml > site.xml.tmp
mv -f site.xml.tmp site.xml

# optimize the site
# see http://wiki.eclipse.org/Platform-releng-faq
case ${TYPE} in test*)
  echo "Packing the site... $SITE"
  # Workaround for downgrading effort of pack200 to avoid VM bug
  # See https://bugs.eclipse.org/bugs/show_bug.cgi?id=154069
  #java -Dorg.eclipse.update.jarprocessor.pack200=$mydir \
  #java -jar $HOME/ws_31x/eclipse/startup.jar \
  java -jar ${basebuilder}/plugins/org.eclipse.equinox.launcher.jar \
    -application org.eclipse.update.core.siteOptimizer \
    -jarProcessor -outputDir $SITE \
    -processAll -pack $SITE
  #java -Dorg.eclipse.update.jarprocessor.pack200=$mydir \
  #    $HOME/ws_31x/jarprocessor/jarprocessor.jar \
  #    -outputDir $SITE -processAll -pack $SITE
  ;;
esac

#Create the digest
echo "Creating digest..."
#java -jar $HOME/ws_31x/eclipse/startup.jar \
java -jar ${basebuilder}/plugins/org.eclipse.equinox.launcher.jar \
    -application org.eclipse.update.core.siteOptimizer \
    -digestBuilder -digestOutputDir=$SITE \
    -siteXML=$SITE/site-europa.xml

##if false ; then
#Create P2 metadata
echo "Creating P2 metadata..."
#Always create from scratch
cd ${SITE}
for x in content.xml content.jar content.jar.pack.gz artifacts.xml artifacts.jar artifacts.jar.pack.gz ; do
  if [ -f $x ]; then rm -f $x; fi
done
java -jar ${basebuilder}/plugins/org.eclipse.equinox.launcher.jar \
    -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator \
    -updateSite ${SITE}/ \
    -site file:${SITE}/site.xml \
    -metadataRepository file:${SITE}/ \
    -metadataRepositoryName "${TPVERSION} Update Site" \
    -artifactRepository file:${SITE}/ \
    -artifactRepositoryName "${TPVERSION} Artifacts" \
    -compress \
    -reusePack200Files \
    -noDefaultIUs \
    -vmargs -Xmx256M
##fi

cd $SITE
chgrp -R dsdp-tmadmin .
chmod -R g+w .
chmod -R a+r .
cd $curdir
