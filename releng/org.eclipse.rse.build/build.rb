#!/usr/bin/ruby
#*******************************************************************************
# Copyright (c) 2006, 2009 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
# David Dykstal (IBM) - initial API and implementation
# Martin Oberhuber (Wind River) - ongoing maintenance
#*******************************************************************************

# Build script for Remote System Explorer
# Author: Dave Dykstal, Kushal Munir
# Prerequisites:
# written in ruby
# java and cvs have to be in the path

require "ftools"

def ask(question, default)
	message = "#{question} (default is #{default}): " 
	STDERR.print message
	answer = readline().strip
	answer = answer.empty? ? default : answer
	return answer
end

# "eclipse" is the location of the basic PDE and plugins to compile against
# This should include the org.eclipse.pde.build project
eclipse	= "../eclipse" 

# "basebuilder" is the location of the Eclipse Releng basebuilder
# This can be set to #{eclipse}
basebuilder = "../org.eclipse.releng.basebuilder"

# "builder" is the location of the custom build scripts customTargets.xml and build.properties
# (i.e. the contents of org.eclipse.rse.build)
builder	= "."

# "working" is where the build is actually done, does not need to exist
working = "../working"

# make these absolute paths
eclipse = File.expand_path(eclipse)
basebuilder = File.expand_path(basebuilder)
builder = File.expand_path(builder)
working = File.expand_path(working)

# Find the base build scripts: genericTargets.xml and build.xml
candidates = Dir["#{basebuilder}/plugins/org.eclipse.pde.build*"]
if (candidates.size == 0) then 
	raise("PDE Build was not found.")
end
if (candidates.size > 1) then
	raise("Too many versions of PDE Build were found.")
end
pdeBuild = candidates[0]

buildDirectory = "#{working}/build"
packageDirectory = "#{working}/package"
publishDirectory = "#{working}/publish"

tag = ask("Enter tag to fetch from CVS", "HEAD")
buildType = ask("Enter build type (P=Personal, N=Nightly, I=Integration, S=Stable, J/M=Maintenance, K/L=Legacy)", "P")
mydstamp = Time.now.strftime("%Y%m%d")
mytstamp = Time.now.strftime("%H%M")
buildId = ask("Enter the build id", buildType + mydstamp + "-" + mydstamp)

command = "java -cp #{basebuilder}/plugins/org.eclipse.equinox.launcher.jar org.eclipse.core.launcher.Main "
command += "-application org.eclipse.ant.core.antRunner "
command += "-buildfile #{pdeBuild}/scripts/build.xml "
command += "-DbuildDirectory=#{buildDirectory} "
command += "-DpackageDirectory=#{packageDirectory} "
command += "-DpublishDirectory=#{publishDirectory} "
command += "-Dbuilder=#{builder} "
command += "-DbaseLocation=#{eclipse} "
command += "-DbuildType=#{buildType} "
command += "-DbuildId=#{buildId} "
command += "-DmapVersionTag=#{tag} "
command += "-Dmydstamp=#{mydstamp} "
command += "-Dmytstamp=#{mytstamp} "
if ("#{buildType}" == "N") then
	command += "-DforceContextQualifier=#{buildId} "
	command += "-DfetchTag=HEAD "
end

puts(command)

system(command)