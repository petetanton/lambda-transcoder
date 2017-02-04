#!/usr/bin/env bash
echo "Determining which version to deploy"
export VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
echo "We will deploy $VERSION"

echo "Cleaning project"
rm new-last-modified.txt
rm -rf deploy/
rm lambda-transcoder-transcoder-$VERSION.zip

echo "Checking Lambda support for binaries"
curl "http://docs.aws.amazon.com/lambda/latest/dg/current-supported-versions.html" -I --silent | grep "Last-Modified" > new-last-modified.txt
cmp --silent lambda-last-modified.txt new-last-modified.txt || echo "please check that ffmpeg binary is still compatible with lambda [http://docs.aws.amazon.com/lambda/latest/dg/current-supported-versions.html]"

echo "Verifying depandencies"
if [ ! -f sources/ffmpeg ]; then
    rm -rf sources/
    mkdir sources
    aws s3 cp s3://tanton-binary/ffmpeg sources/ffmpeg --profile pete-work
    echo "ffmpeg downloaded"
fi


echo "Preparing dependenices"
mkdir deploy
mkdir deploy/jars
cp sources/ffmpeg deploy/ffmpeg
cp sources/ffmpeg src/main/resources/ffmpeg


echo "Building"
mvn clean package


echo "Zipping all sources together"
cp target/lambda-transcoder-$VERSION-jar-with-dependencies.jar deploy/jars/lambda-transcoder-$VERSION-jar-with-dependencies.jar
zip -r lambda-transcoder-$VERSION.zip deploy

echo "Uploading to S3"
aws s3 cp target/lambda-transcoder-$VERSION-jar-with-dependencies.jar s3://sr-lambda/ --profile pete-work

echo "Finished"