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
#$(call inherit-product, vendor/lineage-priv/keys/keys.mk)

# Charging
BYPASS_CHARGE_SUPPORTED := true
BYPASS_CHARGE_TOGGLE_PATH := /sys/class/power_supply/battery/input_suspend

# CPU / performance
PERF_GOV_SUPPORTED := true
PERF_DEFAULT_GOV := schedutil
PERF_ANIM_OVERRIDE := false

# Debug
PRODUCT_SYSTEM_PROPERTIES += \
    persist.sys.ax_debug_enabled=1

# Device  Info
AXION_CAMERA_REAR_INFO := 13
AXION_CAMERA_FRONT_INFO := 8
AXION_PROCESSOR := Qualcomm_Snapdragon_870

# Display
TARGET_SUPPORTED_REFRESH_RATES := 30,48,50,60,90,120,144

# GPU
GPU_FREQS_PATH := /sys/class/kgsl/kgsl-3d0/freq_table_mhz
GPU_MIN_FREQ_PATH := /sys/class/kgsl/kgsl-3d0/min_clock_mhz

# High Brightness Mode (HBM)
HBM_SUPPORTED := false

# Maintainer
AXION_MAINTAINER := amisuke

# Power / memory
TARGET_IS_LOW_RAM ?= false
TARGET_NEEDS_DOZE_FIX := false

# UI / features
TARGET_ENABLE_BLUR := true
TARGET_INCLUDE_VIPERFX := false
TARGET_INCLUDES_LOS_PREBUILTS := false
TARGET_SUPPORTS_QUICK_TAP := false
TORCH_STR_SUPPORTED := false

PRODUCT_NAME := lineage_pipa
PRODUCT_DEVICE := pipa
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Pad 6

PRODUCT_CHARACTERISTICS := tablet

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Xiaomi/pipa_global/pipa:13/RKQ1.211001.001/OS2.0.16.0.UMZMIXM:user/release-keys
