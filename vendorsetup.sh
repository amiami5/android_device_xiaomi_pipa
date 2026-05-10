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

# Ensure script is run from repo root
ROOT_DIR=$(pwd)

# ──────────────────────────────────────────────────────────────
# clone_if_missing + clean_clone (with depth=2)
# ──────────────────────────────────────────────────────────────
clone_if_missing() {
    local repo_url=$1 branch=$2 target_dir=$3
    [ -z "$repo_url" ] || [ -z "$branch" ] || [ -z "$target_dir" ] && {
        error "Usage: clone_if_missing <repo_url> <branch> <target_dir>"
        return 1
    }

    if [ ! -d "$target_dir" ]; then
        info "Cloning $target_dir..."
        git clone --depth=2 "$repo_url" -b "$branch" "$target_dir" -q \
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
    git clone --depth=2 "$repo_url" -b "$branch" "$target_dir" -q && \
        success "Cloned $target_dir." || {
        error "Clone failed."
        return 1
    }
    return 0
}

# ──────────────────────────────────────────────────────────────
# Kernel Repo
# ──────────────────────────────────────────────────────────────
#divider
#info "Cloning kernel into kernel/xiaomi/sm8250..."
#clone_if_missing "https://github.com/SD870/kernel_xiaomi_sm8250" "16" "kernel/xiaomi/sm8250"
#divider

# ──────────────────────────────────────────────────────────────
# Other Repos
# ──────────────────────────────────────────────────────────────
#info "Setting up other repositories..."
#clone_if_missing "https://github.com/nullpointer1101/android_device_xiaomi_sm8250-common" "16" "device/xiaomi/sm8250-common"
#clone_if_missing "https://github.com/SD870/vendor_xiaomi_sm8250-common" "16" "vendor/xiaomi/sm8250-common"
#clone_if_missing "https://github.com/SD870/vendor_xiaomi_pipa" "16" "vendor/xiaomi/pipa"
#clean_clone "https://github.com/SD870/hardware_xiaomi.git"  "16" "hardware/xiaomi"
#clean_clone "https://github.com/PocoF3Releases/packages_resources_devicesettings.git" "aosp-16" "packages/resources/devicesettings"
#divider

# ──────────────────────────────────────────────────────────────
# Apply Tablet FW Patch
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

    tr -d '\r' < "$patch_file" > "$temp_patch"

    if git apply --check --ignore-whitespace "$temp_patch" >/dev/null 2>&1; then
        if git apply --ignore-whitespace "$temp_patch" >/dev/null 2>&1; then
            git add .
            git commit -m "Apply tablet patch: $(sha1sum "$temp_patch" | awk '{print $1}')" -q || true
            success "Tablet patch applied successfully."
        else
            warn "Tablet patch failed to apply cleanly; skipping."
            git reset --hard HEAD >/dev/null 2>&1 || true
            git clean -fd >/dev/null 2>&1 || true
        fi
    else
        warn "Tablet patch seems to be already applied or not applicable; skipping."
    fi

    rm -f "$temp_patch"
    cd "$root_dir"
}


# ──────────────────────────────────────────────────────────────
# Run Patch Setup
# ──────────────────────────────────────────────────────────────
DEVICE_PATH="${ROOT_DIR}/device/xiaomi/pipa"
mkdir -p "$DEVICE_PATH/patches"

apply_tablet_patch

echo "-------------------------------------"
echo "           Setup complete!           "
echo "-------------------------------------"
