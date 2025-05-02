echo "recovery用固有コード"
cd bootable/recovery && git fetch https://github.com/amiami5/android_bootable_recovery.git crdroid-pipa && git cherry-pick 4c6d8c7d396d95f39eeea937ce92b4b847ab33c6
cd ../../
