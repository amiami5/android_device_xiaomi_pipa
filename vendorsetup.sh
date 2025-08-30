#!/bin/bash

# ──────────────────────────────────────────────────────────────
# 🎨 Terminal Colors
# ──────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

info()    { echo -e "${BLUE}${BOLD}→${NC} ${BLUE}$1${NC}"; }
success() { echo -e "${GREEN}${BOLD}✔${NC} ${GREEN}$1${NC}"; }
warn()    { echo -e "${YELLOW}${BOLD}!${NC} ${YELLOW}$1${NC}"; }
error()   { echo -e "${RED}${BOLD}✖${NC} ${RED}$1${NC}"; }
divider() { echo -e "${BOLD}──────────────────────────────────────────────${NC}"; }

# ──────────────────────────────────────────────────────────────
# Prompt for Axion Patch
# ──────────────────────────────────────────────────────────────
read -p "Do you want to apply the axion patch? (yes/no): " APPLY_AXION
if [ "$APPLY_AXION" == "yes" ]; then
    info "Applying axion patch..."
    cd device/xiaomi/pipa || exit
    git am patches/axion.patch || {
        error "Failed to apply axion patch."
        exit 1
    }
    cd - || exit
    success "Axion patch applied successfully."
    KERNEL_BRANCH="16" # Use 16 branch for kernel if axion patch is applied
else
    KERNEL_BRANCH="16.ksun" # Default to 16.ksun branch
fi

# ──────────────────────────────────────────────────────────────
# clone_if_missing + clean_clone
# ──────────────────────────────────────────────────────────────
clone_if_missing() {
    local repo_url=$1 branch=$2 target_dir=$3
    [ -z "$repo_url" ] || [ -z "$branch" ] || [ -z "$target_dir" ] && {
        error "Usage: clone_if_missing <repo_url> <branch> <target_dir>"
        return 1
    }

    if [ ! -d "$target_dir" ]; then
        info "Cloning $target_dir..."
        git clone "$repo_url" -b "$branch" "$target_dir" -q \
            && success "Done cloning $target_dir." || {
            error "Failed to clone $repo_url."
            return 1
        }
    else
        warn "$target_dir already exists, skipping."
    fi
    return 0
}

clean_clone() {
    local repo_url=$1 branch=$2 target_dir=$3
    [ -z "$repo_url" ] || [ -z "$branch" ] || [ -z "$target_dir" ] && {
        error "Usage: clean_clone <repo_url> <branch> <target_dir>"
        return 1
    }

    info "Fresh cloning $target_dir from $branch..."
    [ -d "$target_dir" ] && rm -rf "$target_dir" && success "Removed $target_dir."
    git clone "$repo_url" -b "$branch" "$target_dir" -q && \
        success "Cloned $target_dir." || {
        error "Clone failed."
        return 1
    }
    return 0
}

# ──────────────────────────────────────────────────────────────
# Kernel Repo (using dynamic branch selection)
# ──────────────────────────────────────────────────────────────
divider
info "Cloning kernel into kernel/xiaomi/sm8250..."
clean_clone "https://github.com/sheoranpranshu/android_kernel_xiaomi_sm8250" "$KERNEL_BRANCH" "kernel/xiaomi/sm8250"
divider

# ──────────────────────────────────────────────────────────────
# Other Repos
# ──────────────────────────────────────────────────────────────
info "Setting up other repositories..."
clone_if_missing "https://github.com/sheoranpranshu/android_device_xiaomi_sm8250-common" "16" "device/xiaomi/sm8250-common"
clone_if_missing "https://github.com/sheoranpranshu/proprietary_vendor_xiaomi_sm8250-common" "16" "vendor/xiaomi/sm8250-common"
clone_if_missing "https://github.com/sheoranpranshu/proprietary_vendor_xiaomi_pipa" "16" "vendor/xiaomi/pipa"
clone_if_missing "https://github.com/PocoF3Releases/vendor_qcom_wfd.git" "bka" "vendor/qcom/wfd"
clone_if_missing "https://github.com/PocoF3Releases/device_qcom_wfd.git" "bka" "device/qcom/wfd"
clean_clone "https://github.com/gensis01/hardware_xiaomi.git"  "aosp-16" "hardware/xiaomi"
clean_clone "https://github.com/PocoF3Releases/packages_resources_devicesettings.git" "aosp-16" "packages/resources/devicesettings"
divider

# ──────────────────────────────────────────────────────────────
# Apply Recovery Patch (non-fatal warning only)
# ──────────────────────────────────────────────────────────────
apply_recovery_patch() {
    local root_dir
    root_dir=$(pwd)
    local target_dir="bootable/recovery"
    local patch_file="$root_dir/device/xiaomi/pipa/patches/atomic-recovery.diff"
    local temp_patch="/tmp/atomic-recovery.patch"

    info "Attempting to apply recovery patch..."

    if [ ! -f "$patch_file" ]; then
        warn "Patch file not found, skipping: $patch_file"
        return
    fi

    if ! cd "$target_dir"; then
        warn "Could not enter $target_dir, skipping patch."
        return
    fi
    
    # Clean DOS line endings from the patch file
    tr -d '\r' < "$patch_file" > "$temp_patch"

    # Check if the patch is already applied by looking for its commit
    local patch_fingerprint
    patch_fingerprint=$(sha1sum "$temp_patch" | awk '{print $1}')
    if git log -1 --pretty=%B | grep -q "$patch_fingerprint"; then
        warn "Recovery patch seems to be already applied. Skipping."
        rm -f "$temp_patch"
        cd "$root_dir"
        return
    fi

    # Attempt to apply the patch. If it fails, warn the user and continue.
    if git apply --check --ignore-whitespace "$temp_patch" >/dev/null 2>&1; then
        git apply --ignore-whitespace "$temp_patch"
        git add .
        git commit -m "Apply recovery patch: $patch_fingerprint" -q
        success "Recovery patch applied successfully."
    else
        warn "White recovery patch is skipped, may cause problems in recovery."
    fi

    # Cleanup and return to the original directory
    rm -f "$temp_patch"
    cd "$root_dir"
}

# ──────────────────────────────────────────────────────────────
# Apply Tablet FW Patch (similar to apply_recovery_patch)
# ──────────────────────────────────────────────────────────────
apply_tablet_patch() {
    local root_dir
    root_dir=$(pwd)
    local target_dir="frameworks/base"
    local patch_file="$root_dir/device/xiaomi/pipa/patches/tablet-fwb.patch"
    local temp_patch="/tmp/tablet-fwb.patch"

    info "Attempting to apply tablet-fwb.patch..."

    if [ ! -f "$patch_file" ]; then
        warn "Patch file not found, skipping: $patch_file"
        return
    fi

    if ! cd "$target_dir"; then
        warn "Could not enter $target_dir, skipping patch."
        return
    fi

    # Clean DOS line endings from the patch file
    tr -d '\r' < "$patch_file" > "$temp_patch"

    # Check if the patch is already applied by looking for its commit
    local patch_fingerprint
    patch_fingerprint=$(sha1sum "$temp_patch" | awk '{print $1}')
    if git log -1 --pretty=%B | grep -q "$patch_fingerprint"; then
        warn "Tablet patch seems to be already applied. Skipping."
        rm -f "$temp_patch"
        cd "$root_dir"
        return
    fi

    # Attempt to apply the patch. If it fails, warn the user and continue.
    if git apply --check --ignore-whitespace "$temp_patch" >/dev/null 2>&1; then
        git apply --ignore-whitespace "$temp_patch"
        git add .
        git commit -m "Apply tablet patch: $patch_fingerprint" -q
        success "Tablet patch applied successfully."
    else
        warn "Tablet patch is skipped, may cause issues."
    fi

    # Cleanup and return to the original directory
    rm -f "$temp_patch"
    cd "$root_dir"
}

# ──────────────────────────────────────────────────────────────
# Setup firmware: download, extract, move whole 'radio' folder
# (runs after apply_recovery_patch)
# ──────────────────────────────────────────────────────────────
setup_firmware() {
    local root_dir
    root_dir=$(pwd)
    local target_dir="${root_dir}/vendor/xiaomi/pipa"
    local firmware_url="https://github.com/SheoranPranshu/proprietary_vendor_xiaomi_pipa/releases/download/fw-radio-OS2.0.11.0.UMZCNXM-pipa/OS2.0.11.0.UMZCNXM-pipa.zip"
    local tmp_zip="/tmp/OS2.0.11.0.UMZCNXM-pipa.zip"
    local tmp_extract="/tmp/firmware_extract"

    info "Setting up firmware..."

    # Ensure target directory exists
    mkdir -p "$target_dir" || {
        error "Failed to create target directory: $target_dir"
        return 1
    }

    # Remove old radio folder if present
    if [ -d "$target_dir/radio" ]; then
        warn "Removing existing radio folder..."
        rm -rf "$target_dir/radio" || {
            error "Failed to remove existing $target_dir/radio"
            return 1
        }
    fi

    # Download firmware zip
    if command -v curl >/dev/null 2>&1; then
        info "Downloading firmware (curl)..."
        curl -L --fail -o "$tmp_zip" "$firmware_url" || {
            error "Failed to download firmware with curl."
            [ -f "$tmp_zip" ] && rm -f "$tmp_zip"
            return 1
        }
    elif command -v wget >/dev/null 2>&1; then
        info "Downloading firmware (wget)..."
        wget -q -O "$tmp_zip" "$firmware_url" || {
            error "Failed to download firmware with wget."
            [ -f "$tmp_zip" ] && rm -f "$tmp_zip"
            return 1
        }
    else
        error "Neither curl nor wget found. Cannot download firmware."
        return 1
    fi

    # Prepare temp extract dir
    rm -rf "$tmp_extract"
    mkdir -p "$tmp_extract" || {
        error "Failed to create temp extract dir: $tmp_extract"
        rm -f "$tmp_zip"
        return 1
    }

    # Extract to temp dir
    if command -v unzip >/dev/null 2>&1; then
        info "Extracting firmware into temporary location..."
        unzip -q -o "$tmp_zip" -d "$tmp_extract" || {
            error "Extraction failed with unzip."
            rm -f "$tmp_zip"
            rm -rf "$tmp_extract"
            return 1
        }
    elif command -v bsdtar >/dev/null 2>&1; then
        info "Extracting firmware with bsdtar..."
        bsdtar -xf "$tmp_zip" -C "$tmp_extract" || {
            error "Extraction failed with bsdtar."
            rm -f "$tmp_zip"
            rm -rf "$tmp_extract"
            return 1
        }
    else
        error "No extractor (unzip or bsdtar) available."
        rm -f "$tmp_zip"
        rm -rf "$tmp_extract"
        return 1
    fi

    # Find the radio directory inside the extracted tree
    local radio_dir
    radio_dir=$(find "$tmp_extract" -type d -name radio -print -quit)

    if [ -z "$radio_dir" ]; then
        error "No 'radio' directory found inside the extracted firmware."
        rm -f "$tmp_zip"
        rm -rf "$tmp_extract"
        return 1
    fi

    # Move the whole radio directory into target_dir
    info "Moving radio directory into $target_dir..."
    mv "$radio_dir" "$target_dir"/ || {
        error "Failed to move radio directory to $target_dir"
        rm -f "$tmp_zip"
        rm -rf "$tmp_extract"
        return 1
    }

    # Cleanup leftover extracted files (the moved radio dir is no longer in tmp_extract)
    rm -f "$tmp_zip"
    rm -rf "$tmp_extract"

    success "Firmware setup complete: moved 'radio' directory to $target_dir/radio"
}

# ──────────────────────────────────────────────────────────────
# Run Patch Setup
# ──────────────────────────────────────────────────────────────
ROOT_DIR=$(pwd)
DEVICE_PATH="${ROOT_DIR}/device/xiaomi/pipa"
mkdir -p "$DEVICE_PATH/patches"

# Apply patch for white screen recovery issue
apply_recovery_patch

# Apply tablet patch
apply_tablet_patch

# Download fw and place it correctly
setup_firmware

echo "-------------------------------------"
echo "           Setup complete!           "
echo "-------------------------------------"
