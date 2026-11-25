#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from sm8250-common
include device/xiaomi/sm8250-common/BoardConfigCommon.mk

DEVICE_PATH := device/xiaomi/pipa

# Board
TARGET_BOARD_INFO_FILE := $(DEVICE_PATH)/board-info.txt

# Camera (Miui/Leica Camera)
ifneq ($(wildcard device/xiaomi/camera/miuicamera.mk),)
TARGET_CAMERA_PACKAGE_NAME := com.android.camera

DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    $(DEVICE_PATH)/framework_compatibility_matrix.xml

BOARD_VENDOR_SEPOLICY_DIRS += \
    device/xiaomi/camera/sepolicy/camera/vendor \
    device/xiaomi/camera/sepolicy/misys/vendor

SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += \
    device/xiaomi/camera/sepolicy/camera/private \
    device/xiaomi/camera/sepolicy/misys/private

SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += \
    device/xiaomi/camera/sepolicy/misys/public
endif

# Display
TARGET_SCREEN_DENSITY := 400

# Kernel
TARGET_KERNEL_CONFIG += vendor/xiaomi/pipa.config

# Properties
TARGET_SYSTEM_PROP += $(DEVICE_PATH)/system.prop
TARGET_VENDOR_PROP += $(DEVICE_PATH)/vendor.prop

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor

# Wi-Fi
SOONG_CONFIG_XIAOMI_KONA_WIFI_SYMLINK_VERSION := v2

# Inherit from the proprietary version
include vendor/xiaomi/pipa/BoardConfigVendor.mk
