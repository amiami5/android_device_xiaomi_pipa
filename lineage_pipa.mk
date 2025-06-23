#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit some common lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_tablet_wifionly.mk)

# Inherit from pipa device
$(call inherit-product, device/xiaomi/pipa/device.mk)

# Inherit keys
$(call inherit-product, vendor/lineage-priv/keys/keys.mk)

PRODUCT_NAME := lineage_pipa
PRODUCT_DEVICE := pipa
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Pad 6

# Axion specific
AXION_CAMERA_REAR_INFO := 13
AXION_CAMERA_FRONT_INFO := 8
AXION_CPU_SMALL_CORES := 0,1,2,3
AXION_CPU_BIG_CORES := 4,5,6,7
AXION_MAINTAINER := amisuke
AXION_PROCESSOR := Qualcomm_Snapdragon_870
TARGET_PREBUILT_BCR := false
PRODUCT_NO_CAMERA := false
TARGET_INCLUDE_MATLOG := true
BYPASS_CHARGE_SUPPORTED := true

PRODUCT_CHARACTERISTICS := tablet
TARGET_SUPPORTS_QUICK_TAP := false

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Xiaomi/pipa_global/pipa:14/UKQ1.240624.001/OS2.0.5.0.UMZMIXM:user/release-keys

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi