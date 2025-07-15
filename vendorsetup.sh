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
# Kernel Repo (fixed to Normal Perf)
# ──────────────────────────────────────────────────────────────
divider
info "Cloning kernel into kernel/xiaomi/sm8250..."
clone_if_missing "https://github.com/glitch-wraith/android_kernel_xiaomi_sm8250" "bpf-ksu" "kernel/xiaomi/sm8250"
divider

# ──────────────────────────────────────────────────────────────
# Other Repos
# ──────────────────────────────────────────────────────────────
info "Setting up other repositories..."
clone_if_missing "https://github.com/glitch-wraith/android_device_xiaomi_sm8250-common" "16.0" "device/xiaomi/sm8250-common"
clone_if_missing "https://github.com/glitch-wraith/proprietary_vendor_xiaomi_sm8250-common" "16.0" "vendor/xiaomi/sm8250-common"
clone_if_missing "https://github.com/glitch-wraith/proprietary_vendor_xiaomi_pipa" "16.0" "vendor/xiaomi/pipa"
clone_if_missing "https://github.com/LineageOS/android_hardware_xiaomi" "lineage-23.0" "hardware/xiaomi"
clone_if_missing "https://github.com/LineageOS/android_hardware_lineage_compat" "lineage-23.0" "hardware/lineage/compat"
clone_if_missing "https://github.com/LineageOS/android_hardware_lineage_interfaces" "lineage-23.0" "hardware/lineage/interfaces"
clone_if_missing "https://github.com/LineageOS/android_hardware_lineage_livedisplay" "lineage-23.0" "hardware/lineage/livedisplay"
clone_if_missing "https://github.com/glitch-wraith/hardware_dolby.git" "sony-1.3" "hardware/dolby"
divider

# ──────────────────────────────────────────────────────────────
# Apply Recovery Patch (non-fatal warning only)
# ──────────────────────────────────────────────────────────────
apply_recovery_patch() {
    local root_dir
    root_dir=$(pwd)
    local target_dir="bootable/recovery"
    local patch_file="$root_dir/device/xiaomi/pipa/source-patches/atomic-recovery.diff"
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
# Run Patch Setup
# ──────────────────────────────────────────────────────────────
ROOT_DIR=$(pwd)
DEVICE_PATH="${ROOT_DIR}/device/xiaomi/pipa"
mkdir -p "$DEVICE_PATH/source-patches"

apply_recovery_patch

echo "-------------------------------------"
echo "           Setup complete!           "
echo "-------------------------------------"
