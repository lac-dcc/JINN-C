#!/bin/bash
# coding: utf-8
#
# Copyright (C) 2019 juniocezar.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
# MA 02110-1301  USA
#


#
# JAVA GENERAL VARS
JAVADIR="/usr/lib/jvm/java-11-openjdk-amd64/"
JAVALIB="VIRTUAL_FS_FOR_JDK"
JAVA="${JAVADIR}/bin/java"
#
# SOOT GENERAL VARS
SOOTLIB="src/netbeans-project/lib/sootclasses_j9-trunk-jar-with-dependencies.jar"
JINNLIB="build"
SOOTCP="build:$( dirname $1 )"
#
# Output directories, either used by soot or python scripts
BASEDIR=src
SOOT_INSTR_OUT="soot-out1"
SOOT_SPEC_OUT="soot-out2"
RUNTIME_LOG_DIR="runtime-logs"
PREDITOR_DIR="preditor-dir"
#
# Instrummentation helper files
SIGNATURES_MAP="methods-signaturesMap.txt"
RUNTIME_FILE="methods-runtime.txt"
LR_INPUTEX="parsed.txt"
#
# Target Configurations 0b2L, 2b0L, 2b2L, 4b0L, 0b4L, 4b4L
TARGET_CONFIGURATIONS="0x09 0x60 0x69 0xf0 0x0f 0xff"
TARGET_FREQS="1.8GHz 1.6GHz"
# Target Governors
CPU_GOVERNORS="userspace"


#
#
############################ FUNCTIONS #################################
#
#

#
# check if flag --simulate is being used
# flag used for running jinn-c on computer differenty from
# the big little arch. It will simulate some operations
# The results from simulation must NOT be used to reason about
# resources usage
for arg in "$@"; do
	if [ "$arg" == "--simulate" ]; then
		echo ">>>> Running JINN-C in simulation mode <<<<"
		function cpufreq-set {
			echo 0 > /dev/null
		}

		function sudo	 {
			echo 0 > /dev/null
		}

		function taskset {    
		    config=$1
		    shift
		    eval $@
		}
	elif [ "$arg" == "--help" ]; then
		showHelp
	fi
done

#
# Set up environment for running JINN-JAVA tool
function initializeTool () {	
	rm -rf soot-out1 soot-out2 runtime-logs preditor-dir
	mkdir -p $RUNTIME_LOG_DIR $PREDITOR_DIR
}
#
#
function showHelp () {
	echo "JINN-C Adaptive Compiler"
	echo "Syntax: Syntax: jinn-c INPUT_APP INPUT_LIST <flags>, where"
	echo "  INPUT_APP is the path to the .class/.jar file"
	echo "  INPUT_LIST is the path to the .txt input file"
	echo ""
	echo "You can use optional flags with jinn-c."
	echo "Currently, we have the following flags:"
	echo "   --simulate : Runs JINN-C in a simulate mode. This way tools like"
	echo "                taskset, cpufreq-set, and sudo are replaced by dump"
	echo "                methods, allowing one to run jinn-c in almost any computer."

}
#
#
function checkFails () {	
	if [ "$1" != "0" ]; then
		echo "  >> Failed while executing current stage of JINN-C (code $1)... aborting execution"
		exit
	fi
}

#
# Check input arguments
function checkArguments () {
	# ToDo: Collapse all repeated echos into a single shared string
	INPUT_APP=$1
	INPUT_FILE=$2

	# Check if variables are set
	if [ -z "$INPUT_APP" ]; then
		echo "Error: You must pass the file containing the input application."
		echo "Syntax: jinn-c INPUT_APP INPUT_LIST."
		echo ""
		echo "For help use jinn-c --help"
		exit
	fi
	if [ -z "$INPUT_FILE" ]; then
		echo "Error: You must pass the file containing the input list."
		echo "Syntax: jinn-c INPUT_APP INPUT_LIST."
		echo ""
		echo "For help use jinn-c --help"
		exit
	fi

	# Check if files exist
	if [ ! -f "$INPUT_FILE" ]; then
		echo "Error: File containing input list not found. Aborting!"
		echo "Syntax: jinn-c INPUT_APP INPUT_LIST."
		echo ""
		echo "For help use jinn-c --help"
		exit
	fi
	if [ ! -f "$INPUT_APP" ]; then
		echo "Error: Input application [$INPUT_APP] not found. Aborting!"
		echo "Syntax: jinn-c INPUT_APP INPUT_LIST."
		echo ""
		echo "For help use jinn-c --help"
		exit
	fi
}
#
# Run the intrummented application with input list, under all hw configs
function runApp () {
	# requires install cpufrequtils and sudo
	# ToDo: If decide to run with another governors, have to add the gov
	# to the output file name in the mv operation and parse it in the python
	# script

	#
	# TODO: Redirect output and stderr to file instead of /dev/null

	echo "Running Application ($1) with $TARGET_CONFIGURATIONS"
	CLASSNAME=$1
	INPUTFILE=$2

	sudo cpufreq-set -g userspace
	sudo cpufreq-set -c 0 -f 1.5GHz

	for freq in $TARGET_FREQS ; do
		echo "    Setting big cluster frequency to $freq"
		sudo cpufreq-set -c 4 -f $freq
	    for conf in $TARGET_CONFIGURATIONS; do
	    	echo "        Setting hardware configuration to $conf"	    	
	    	while read input; do	    			    		
	      		taskset $conf $JAVA -cp $SOOT_INSTR_OUT:$JINNLIB:$SOOTCP -Xmx1000m $CLASSNAME $input 2>/dev/null >/dev/null
	    	done < $INPUTFILE
	    	mv $RUNTIME_FILE ${RUNTIME_LOG_DIR}/${freq}-${conf}.log 2>/dev/null
		done
	done
}
#
# Parse the runtime logs, deciding the best configuration for each input
function parseLogs () {
	INPUT_APPNAME=$1
	${BASEDIR}/scripts/RuntimeParser.py ${RUNTIME_LOG_DIR} ${RUNTIME_LOG_DIR}/${INPUT_APPNAME}-${LR_INPUTEX}
}
#
# Runs the logistic regression over the log of best configurations
function runRegressor () {
	INPUT_REGNAME=${RUNTIME_LOG_DIR}/"$1-${LR_INPUTEX}"	
	${BASEDIR}/scripts/LR.py $RUNTIME_LOG_DIR $INPUT_REGNAME 
}
#
# Adds coeficients to the predictor
function specilizePredictor () {	
	#ToDo: Currently it only works with one APP + one METHOD at a time
	#ToDo: check if it works with others sed, using GNU sed


	while read input; do
		#
		# checking if static config
		if [ -f ${RUNTIME_LOG_DIR}/${input}.static.txt ]; then
        	sed -e "/##CONFIG##/r ${RUNTIME_LOG_DIR}/${input}.static.txt" \
            ${BASEDIR}/scripts/StaticPredictorShell.java > $PREDITOR_DIR/Predictor.java
    	else
		   	sed -e "/##COEFS##/r ${RUNTIME_LOG_DIR}/${input}.coef.txt" \
			-e "/##CLASSES##/r ${RUNTIME_LOG_DIR}/${input}.class.txt" \
			-e "/##INTER##/r ${RUNTIME_LOG_DIR}/${input}.inter.txt" \
			-e "/##MEAN##/r ${RUNTIME_LOG_DIR}/${input}.n-mean.txt" \
            -e "/##SCALE##/r ${RUNTIME_LOG_DIR}/${input}.n-scale.txt" \
			${BASEDIR}/scripts/PredictorShell.java > $PREDITOR_DIR/Predictor.java
			
			#
			# adding inputs for normalization
			num=$( cat ${RUNTIME_LOG_DIR}/${input}.n.txt  )
	        inputs=""
	        for i in $( seq 0 $num ); do
	            inputs=$( echo "$inputs input[$i] = (input[$i] - mean[$i]) / scale[$i]; ")
	        done
	        echo "$inputs" > /tmp/tmpFileParseJinn.log
	        sed -i "/##INPUTS##/r /tmp/tmpFileParseJinn.log" $PREDITOR_DIR/Predictor.java
	    fi

		${JAVADIR}/bin/javac -d $PREDITOR_DIR $PREDITOR_DIR/Predictor.java	

	done < ${RUNTIME_LOG_DIR}/$SIGNATURES_MAP
	echo "  done"
}
