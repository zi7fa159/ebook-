#!/bin/bash
adb install -r project/bin/app.apk
adb shell am start -n com.example.astrostacker/.MainActivity
