# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\AndroidSDK\SDK\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}
-libraryjars libs/android-xml-json.jar
-libraryjars libs/BaiduLBS_Android.jar
-libraryjars libs/libface_recognition.jar
-libraryjars libs/libmp3lame.jar
-libraryjars libs/putaoservice.jar


# 过滤R文件的混淆：
-keep class **.R$* {*;}


# 保护指定的类文件和类的成员
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
  }

#不混淆第三方包中的指定内容
-keep class android-support-v4.**{*;}
-keep public class * extends android.support.v4.**
-keep class android.view.**{*;}
