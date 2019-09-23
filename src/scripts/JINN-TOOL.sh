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

# JINN-TOOL (JINN-C)
# Tool for automatically instrumment, execute, and produced trained 
# applications for different hardware configurations, based on the
# input.

# Inporting helper functions and variables
BASEDIR=src
source ${BASEDIR}/scripts/JINN-FUNCTIONS.sh
initializeTool
#
# Input java/class/jar file
INPUT_APP=$1
# File containing input list
INPUT_FILE=$2

echo "- JINN-C Adaptive Compiler -"
#
# 1 Check input arguments
checkArguments "$INPUT_APP" "$INPUT_FILE"
#
# 2 Get input file extension (Righ now only using .class)
INPUT_EXTENSION="${INPUT_APP##*.}"
INPUT_APPNAME=$( basename "${INPUT_APP%.*}" )
#
# 3 Run JINN-JAVA - to instrumment input application
echo -e "\nRunning Stage 1 - Instrummenting input app with profiling information"
${JAVA} -cp ${SOOTLIB}:${JINNLIB} jinn.JinnDriver -cp ${SOOTCP}:${JAVALIB} $INPUT_APPNAME -output-dir $SOOT_INSTR_OUT
mv $SIGNATURES_MAP $RUNTIME_LOG_DIR
checkFails $?
#
# 4 Run Intrummented application with input list for all configurations
echo -e "\nRunning Stage 2 - Executing instrummented app with target configurations and inputs"
runApp "$INPUT_APPNAME" "$INPUT_FILE"
checkFails $?
#
# 5 Run Runtime Parser python script to define best configuration for each input
echo -e "\nRunning Stage 3 - Parsing runtime logs"
parseLogs $INPUT_APPNAME
checkFails $?
#
# 6 Run Logistic Regression over the log file generated
echo -e "\nRunning Stage 4 - Performing Logistic Regression Analysis"
runRegressor $INPUT_APPNAME
checkFails $?
#
# 7 Genering specilized Predictor
echo -e "\nRunning Stage 5 - Specializing Input-guided Predictor"
specilizePredictor
checkFails $?
#
# 8 Instrummenting original appplication, adding call to predictor
echo -e "\nRunning Stage 6 - Instrummenting original application, adding call to predictor"
${JAVA} -cp ${SOOTLIB}:${JINNLIB} jinn.AppSpecialization -cp ${SOOTCP}:${JAVALIB}:${PREDITOR_DIR} $INPUT_APPNAME -output-dir $SOOT_SPEC_OUT
mv -v ${PREDITOR_DIR}/jinn $SOOT_SPEC_OUT
checkFails $?
