#!/bin/bash
adb install -r bin/AstroStacker.apk
adb shell am start -n com.example.astrostacker/.MainActivity
