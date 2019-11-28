#!/bin/bash

mkdir "$TEMP/JHelioviewer.app/Contents/Helpers"
cp -r "$RESOURCES/notifier/terminal-notifier.app" "$TEMP/JHelioviewer.app/Contents/Helpers"
# Sign app, comment next line if you don't know the password
#codesign -f -s "SWHV" --keychain "$RESOURCES/swhv.keychain" "$TEMP/JHelioviewer.app"
codesign -f --deep --strict -s "Developer ID Application" "$TEMP/JHelioviewer.app"

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
hdiutil create -srcfolder "$TEMP" -volname JHelioviewer -format UDZO -fs HFS+ -o "$BUILD/$NAME.dmg"

rm -rf "$TEMP"
