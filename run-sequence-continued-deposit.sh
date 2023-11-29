#!/usr/bin/env bash
#
# Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if (( $# < 5 )); then
 echo "Runs the test program SequenceContinuedDeposit."
 echo "Usage: ./run-continued-deposit.sh [--suspend] <COL-IRI> <user> <password> <chunksize> <bag>..."
 echo "Where:"
 echo "--suspend = suspend execution at the start so as to allow a debugger to attach at port $DEBUG_PORT."
 echo "<COL-IRI> = the collection IRI to post to"
 echo "<user> = Data Station user account OR the string 'API_KEY'"
 echo "<password> = password for <user> OR the user's API key if the string 'API_KEY' was passed as user name"
 echo "<chunksize> = size in bytes of each chunk"
 echo "<bag>... = one or more bag directories or zip files to send in sequence"
 exit
fi

DEBUG_PORT=8000
SUSPEND=n
if [[ "$1" == "--suspend" ]]; then
 SUSPEND=y
 shift 1
fi

MAIN_CLASS="nl.knaw.dans.sword2examples.SequenceContinuedDeposit"
COL_IRI=$1
USER=$2
PASSWORD=$3
CHUNKSIZE=$4
BAGS=${*:5}

JARFILE=$(ls -1 target/*SNAPSHOT.jar)

CP_SEP=":"
# If windows use ";" as path separator
if [[ "$OSTYPE" == "msys" ]]; then
  CP_SEP=";"
fi

mvn dependency:copy-dependencies
java -agentlib:jdwp=transport=dt_socket,server=y,address=$DEBUG_PORT,suspend=$SUSPEND -cp "target/dependency/*$CP_SEP$JARFILE" $MAIN_CLASS $COL_IRI $USER $PASSWORD $CHUNKSIZE $BAGS
