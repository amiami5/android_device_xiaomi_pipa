echo "recovery用固有コード"
cd bootable/recovery && git fetch https://github.com/amiami5/android_bootable_recovery.git 15 && git cherry-pick feaff1fb244afbb87792338c6a113f5650237157
cd ../../