#!/usr/bin/env bash

# exit when any command fails
set -e

LIB_DIR="lib"

JAR_PREFIX="ant-salesforce"
OLD_JAR=$(basename $LIB_DIR/$JAR_PREFIX*.jar)

MAJOR_VERSION_NUMBER=$(echo $OLD_JAR | sed "s/$JAR_PREFIX-\([0-9]\+\).0.jar/\1/g")
OLD_API_VERSION="$MAJOR_VERSION_NUMBER.0"

NEW_API_VERSION="$(( MAJOR_VERSION_NUMBER + 1 )).0"
NEW_JAR="$JAR_PREFIX-$NEW_API_VERSION.jar"

read -p "Upgrade Salesforce API version from $OLD_API_VERSION to $NEW_API_VERSION? " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Skipped."
    exit 1 
fi


## Download new ant lib jar and replace old one
ANT_ZIP_URL="https://gs0.salesforce.com/dwnld/SfdcAnt/salesforce_ant_$NEW_API_VERSION.zip"
ANT_ZIP="ant.zip"
JAR="$JAR_PREFIX.jar"

curl $ANT_ZIP_URL --output $ANT_ZIP
unzip -j $ANT_ZIP $JAR -d $LIB_DIR
mv $LIB_DIR/$JAR $LIB_DIR/$NEW_JAR
rm $ANT_ZIP


## Update API version in default properties and groovy script
DEFAULT_PROPERTIES_FILE="ant-includes/default.properties"
GROOVY_FILE="force-meta-backup.groovy"

sed -i "s/\(sf.antlib.version = \)$OLD_API_VERSION/\1$NEW_API_VERSION/g" $DEFAULT_PROPERTIES_FILE
sed -i "s/\(version='\)$OLD_API_VERSION/\1$NEW_API_VERSION/g" $GROOVY_FILE

## Update 
git rm $LIB_DIR/$OLD_JAR
git add $LIB_DIR/$NEW_JAR $DEFAULT_PROPERTIES_FILE $GROOVY_FILE


echo ""
echo "********************************************************************************"
echo ""
echo "Salesforce Ant lib upgraded from $OLD_API_VERSION to $NEW_API_VERSION"
echo ""
echo "API version number updated in following files:"
echo "  - $DEFAULT_PROPERTIES_FILE"
echo "  - $GROOVY_FILE"
echo ""
echo "Changes have been staged in git for commit"
echo ""
echo "Manually apply any required changes to $GROOVY_FILE to support "
echo "new Metadata types introduced in this release (see release notes)"
echo ""
echo "Test >> Commit >> Done"
echo ""
