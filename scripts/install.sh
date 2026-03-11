#!/bin/bash
adb install -r bin/ProAstro.apk
adb shell am start -n com.example.astrostacker/.MainActivity
