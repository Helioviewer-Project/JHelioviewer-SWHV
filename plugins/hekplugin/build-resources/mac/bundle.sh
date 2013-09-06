#!/bin/bash


# Setting up the dir to bundle
ln -s /Applications "$TEMP/Applications"
mkdir "$TEMP/.background"
cp "$RESOURCES/JHV_Installer.png" "$TEMP/.background/background.png"
cp "$RESOURCES/DSStore" "$TEMP/.DS_Store"
# Don't copy the rest to keep the image clean
# Should be included with the app bundle
#cp $RESOURCES/*.webloc "$TEMP/"
#cp "$README" "$TEMP/"
#cp "$COPYING" "$TEMP/"
#cp "$VERSION" "$TEMP/"

# Delete old image
if [ -e "$BUILD/$NAME.dmg" ]
	then rm "$BUILD/$NAME.dmg" 
fi

# Build the disk image
hdiutil create -srcfolder "$TEMP" -volname JHelioviewer -format UDBZ -o "$BUILD/$NAME.dmg"

rm -rf "$TEMP"
