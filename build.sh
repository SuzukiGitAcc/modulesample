#!/bin/sh
ant clean

cd ./libs
mkdir ./armeabi
mkdir ./armeabi-v7a
cd ../

cp -f ./ndklib/armeabi/liblinphonearmv5.so      ./libs/armeabi
cp -f ./ndklib/armeabi-v7a/libavcodec.so        ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/libavcodecnoneon.so  ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/libavcore.so         ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/libavutil.so         ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/liblincrypto.so      ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/liblinphone.so       ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/liblinphonenoneon.so ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/liblinssl.so         ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/libsrtp.so           ./libs/armeabi-v7a
cp -f ./ndklib/armeabi-v7a/libswscale.so        ./libs/armeabi-v7a


#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi/liblinphonearmv5.so      ./libs/armeabi
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libavcodec.so        ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libavcodecnoneon.so  ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libavcore.so         ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libavutil.so         ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/liblincrypto.so      ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/liblinphone.so       ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/liblinphonenoneon.so ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/liblinssl.so         ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libsrtp.so           ./libs/armeabi-v7a
#cp -f /Users/develop/Documents/workspace/linphone-android/libs/armeabi-v7a/libswscale.so        ./libs/armeabi-v7a

ant dist
