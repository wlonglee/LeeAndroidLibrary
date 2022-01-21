echo "$$package_begin$$"
sleep 1
cd ..
gradlew build
gradle assembleRelease

echo -e "$$package success$$"

notify-send build.sh "package down!"