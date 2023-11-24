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

#
# Helper script to perform a deposit using the Maven project.
#
#

DEBUG_PORT=8000
SUSPEND=n
if [[ "$1" == "--suspend" ]]; then
 SUSPEND=y
 shift 1
fi

PROGRAM=$1
MAIN_CLASS="nl.knaw.dans.sword2examples.${PROGRAM}Deposit"
COL_IRI=$2
USER=$3
PASSWORD=$4

JARFILE=$(ls -1 target/*SNAPSHOT.jar)

if (( $# < 5 )); then
 echo "Runs one of the test programs to send one or more bags to the SWORD2 service."
 echo "Usage: ./run-deposit.sh [--suspend] <program> <COL-IRI> <user> <password> [<chunksize>] <bag>..."
 echo "Usage: ./run-deposit.sh [--suspend] <program> <COL-IRI> <user> <password> <bag> [<sword token>]"
 echo "Where:"
 echo "--suspend = suspend execution at the start so as to allow a debugger to attach at port "
 echo "<program> = one of Simple,Continued,SequenceSimple,SequenceContinued"
 echo "<COL-IRI> = the collection IRI to post to"
 echo "<user> = Data Station user account OR the string 'API_KEY'"
 echo "<password> = password for <user> OR the user's API key if the string 'API_KEY' was passed as user name"
 echo "<chunksize> = size in byte of each chunk (only for the Continued variants)"
 echo "<bag> = one bag directory or zip file to send or multiple (only for Sequence variants)"
 echo "<sword token> = the SWORD2 token to use if the bag is an update of an existing dataset (only for Simple variants)"
 exit
fi

if [[ "$PROGRAM" =~ ^.*Continued$ ]]; then
    CHUNKSIZE=$5
    BAGDIRS=${@:6}
    SWORD_TOKEN=""
else
    CHUNKSIZE=""
    BAGDIRS=$5
    SWORD_TOKEN=$6
fi

CP_SEP=":"
# If windows use ";" as path separator
if [[ "$OSTYPE" == "msys" ]]; then
  CP_SEP=";"
fi

mvn dependency:copy-dependencies
java -agentlib:jdwp=transport=dt_socket,server=y,address=$DEBUG_PORT,suspend=$SUSPEND -cp "target/dependency/*$CP_SEP$JARFILE" $MAIN_CLASS $COL_IRI $USER $PASSWORD $CHUNKSIZE $BAGDIRS $SWORD_TOKEN
