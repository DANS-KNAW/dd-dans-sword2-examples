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
if [[ "$1" == "suspend" ]]; then
 SUSPEND=y
 shift 1
fi

BAG=$1
JARFILE=$(ls -1 target/*SNAPSHOT.jar)

if (( $# < 1 )); then
 echo "Usage: ./run-validation.sh [suspend] <bag>"
 echo "Where:"
 echo "suspend = suspend execution at the start so as to allow a debugger to attach at port "
 echo "<bag> = one bag directory or zip file to send"
 exit
fi

mvn dependency:copy-dependencies
java -agentlib:jdwp=transport=dt_socket,server=y,address=$DEBUG_PORT,suspend=$SUSPEND -cp "target/dependency/*:$JARFILE" "nl.knaw.dans.sword2examples.ValidateBag" $BAG

