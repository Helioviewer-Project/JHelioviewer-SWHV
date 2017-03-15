#!/bin/bash

# halt on errors
set -e

rm -rf build | true
mkdir build
javac -d ./build -g -cp './src/:./lib/*'  $(find ./src -name "*.java")

# create run script
#echo 'java -classpath '\''./:../resources/:../lib/*'\'' org.helioviewer.jhv.JHelioviewer' > ./build/run.sh
#chmod a+x ./build/run.sh

# create deployment
# hint: add use of JarClassLoader to support loading of jar-files inside of jar

rm -rf deploy | true
mkdir deploy
cp -r lib deploy/
echo -n "Class-Path:" > deploy/Manifest.txt
for i in $(\ls deploy/lib)
do
    echo "  lib/"$i >> deploy/Manifest.txt 
done
echo "  resources" >> deploy/Manifest.txt 
echo "version: "$(cat VERSION) >> deploy/Manifest.txt 
echo "revision: 1" >> deploy/Manifest.txt 

jar cemf org.helioviewer.jhv.JHelioviewer deploy/Manifest.txt deploy/JHelioviewer.jar -C build . -C resources .
rm deploy/Manifest.txt

