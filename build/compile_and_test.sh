#!/bin/bash

dot="$(cd "$(dirname "$0")"; pwd)"

junit_path=~/java-extras/junit/junit_4-12.jar
hamcrest_path=~/java-extras/hamcrest/hamcrest-core_1-3.jar
export CLASSPATH=$dot/../bin:$dot/../test/bin:$junit_path:$hamcrest_path


src_compile_result="$(javac -d $dot/../bin $dot/../src/*.java 2>&1)"
test_compile_result="$(javac -d $dot/../test/bin $dot/../test/src/*.java 2>&1)"

if [[ -z "$src_compile_result" ]] && [[ -z "$test_compile_result" ]] ; then
	class_files="$(ls "$dot"/../test/bin | sed -En "s/(.+)\.class/\1/p")"
	java org.junit.runner.JUnitCore "$class_files" 
else
	printf "%s" "$src_compile_result"
	printf "%s" "$test_compile_result"
fi
