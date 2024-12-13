echo "recovery用固有コード"
cd bootable/recovery && git fetch https://github.com/amiami5/android_bootable_recovery.git 15 && git cherry-pick feaff1fb244afbb87792338c6a113f5650237157
cd ../../

echo "updaterを自分に向ける"
cd packages/apps/Updater && git fetch https://github.com/amiami5/android_packages_apps_Updater.git 15.0 && git cherry-pick 18c5680d874d11d8b5950ef903c712beccf20df5
cd ../../../
