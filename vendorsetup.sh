#!/bin/bash

echo "recovery用固有コード"
cd bootable/recovery && git fetch https://github.com/amiami5/android_bootable_recovery.git 15 && git cherry-pick 4c6d8c7d396d95f39eeea937ce92b4b847ab33c6
cd ../../

echo "updaterを自分に向ける"
cd packages/apps/Updater && git fetch https://github.com/amiami5/android_packages_apps_Updater.git 15.0 && git cherry-pick 7da28aae83d19bab3e39a32d27a58e3cd8c87f58
cd ../../../