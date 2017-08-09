:: @ECHO off  
if exist .\build\outputs\apk\*.apk (
	del F:\AndroidStudio\AppSign\*.apk
)else(
		echo "No targit apk!"
		pause>nul
		pause
		::exit
)
echo "copy apks..."
COPY .\build\outputs\apk\*.apk F:\AndroidStudio\AppSign\
git add .
git commit -m "签名"
git push origin HEAD:refs/for/master
echo "upload finished"
pause>nul
