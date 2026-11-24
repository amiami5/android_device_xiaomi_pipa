#!/bin/bash
#if ! mountpoint -q "$HOME/axion/out"; then
#    sudo mount --bind /mnt/aosp_out "$HOME/axion/out"
#fi
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
# Apply repo patches (patches/<project path>/*.patch via git apply)
# The patches are applied to the working tree only, so a repo sync can reset
# them; they are put back the next time envsetup runs.
# ──────────────────────────────────────────────────────────────
apply_repo_patches() {
    local project=$1
    local repo_dir="$ROOT_DIR/$project"
    local patch_dir="$DEVICE_PATH/patches/$project"
    local tmp_index tree patch

    [ -d "$patch_dir" ] || return 0
    [ -e "$repo_dir/.git" ] || {
        warn "$project not found, skipping patches."
        return 0
    }

    tmp_index="$(mktemp)"
    rm -f "$tmp_index"
    if ! GIT_INDEX_FILE="$tmp_index" git -C "$repo_dir" read-tree HEAD; then
        rm -f "$tmp_index"
        return 0
    fi

    for patch in "$patch_dir"/*.patch; do
        [ -f "$patch" ] || continue
        if ! GIT_INDEX_FILE="$tmp_index" git -C "$repo_dir" apply --cached "$patch" 2>/dev/null; then
            if git -C "$repo_dir" apply --reverse --check "$patch" >/dev/null 2>&1; then
                continue
            fi
            error "Failed to apply $(basename "$patch") to $project; check if upstream changed."
            rm -f "$tmp_index"
            return 0
        fi
    done

    tree="$(GIT_INDEX_FILE="$tmp_index" git -C "$repo_dir" write-tree)"
    rm -f "$tmp_index"

    if git -C "$repo_dir" diff --quiet "$tree" --; then
        warn "Patches for $project already applied; skipping."
        return 0
    fi

    if ! git -C "$repo_dir" diff --quiet HEAD --; then
        warn "$project has local changes, not applying patches."
        return 0
    fi

    for patch in "$patch_dir"/*.patch; do
        [ -f "$patch" ] || continue
        if ! git -C "$repo_dir" apply "$patch"; then
            error "Failed to apply $(basename "$patch") to $project."
            return 1
        fi
    done
    success "Applied patches to $project."
}

# ──────────────────────────────────────────────────────────────
# Setup llvm-clang (../toolchains/llvm-clang)
# ──────────────────────────────────────────────────────────────
_pipa_resolve_tool() {
    local cmd="$1"
    local fallback="$2"
    if command -v "${cmd}" >/dev/null 2>&1; then
        command -v "${cmd}"
        return 0
    fi
    if [ -n "${fallback}" ] && [ -x "${fallback}" ]; then
        echo "${fallback}"
        return 0
    fi
    return 1
}

setup_llvm_clang() {
    local cache_dir="${PIPA_TOOLCHAIN_DIR:-$(dirname "$ROOT_DIR")/toolchains}"
    local src_dir="${cache_dir}/llvm-project"
    local build_dir="${src_dir}/build"
    local install_dir="${cache_dir}/llvm-clang"
    local llvm_rev="f42c87ba2c70219b1f4ad1f031a5b6a1ea6603fb"

    if [ -x "${install_dir}/bin/clang" ] && [ -x "${install_dir}/bin/ld.lld" ]; then
        return 0
    fi

    if [ "${PIPA_SKIP_LLVM_BUILD:-0}" = "1" ]; then
        warn "Skipping llvm-clang build (PIPA_SKIP_LLVM_BUILD=1)."
        return 0
    fi

    local cmake_bin ninja_bin python3_bin
    if ! command -v git >/dev/null 2>&1; then
        error "Missing required tool 'git' to build llvm-clang."
        return 1
    fi
    cmake_bin="$(_pipa_resolve_tool cmake "${ROOT_DIR}/prebuilts/cmake/linux-x86/bin/cmake")" || {
        error "Missing required tool 'cmake' to build llvm-clang."
        return 1
    }
    ninja_bin="$(_pipa_resolve_tool ninja "${ROOT_DIR}/prebuilts/build-tools/linux-x86/bin/ninja")" || {
        error "Missing required tool 'ninja' to build llvm-clang."
        return 1
    }
    python3_bin="$(_pipa_resolve_tool python3 "${ROOT_DIR}/prebuilts/build-tools/path/linux-x86/python3")" || {
        error "Missing required tool 'python3' to build llvm-clang."
        return 1
    }

    if [ -z "${CC:-}" ] && ! command -v cc >/dev/null 2>&1 && ! command -v gcc >/dev/null 2>&1 && ! command -v clang >/dev/null 2>&1; then
        error "Missing host C compiler (install build-essential or clang) to build llvm-clang."
        return 1
    fi
    if [ -z "${CXX:-}" ] && ! command -v c++ >/dev/null 2>&1 && ! command -v g++ >/dev/null 2>&1 && ! command -v clang++ >/dev/null 2>&1; then
        error "Missing host C++ compiler (install build-essential or clang) to build llvm-clang."
        return 1
    fi

    mkdir -p "${cache_dir}" || return 1

    if [ ! -d "${src_dir}/.git" ]; then
        info "Fetching llvm-project (${llvm_rev}) to ${src_dir}..."
        mkdir -p "${src_dir}" || return 1
        git -C "${src_dir}" init -q || return 1
        git -C "${src_dir}" remote add origin https://github.com/llvm/llvm-project.git || return 1
        if ! git -C "${src_dir}" fetch --depth 1 origin "${llvm_rev}"; then
            error "Failed to fetch llvm-project."
            rm -rf "${src_dir}"
            return 1
        fi
        git -C "${src_dir}" checkout -q FETCH_HEAD || return 1
    fi

    info "Configuring llvm-clang in ${build_dir}..."
    "${cmake_bin}" -S "${src_dir}/llvm" -B "${build_dir}" -G Ninja \
        -DCMAKE_MAKE_PROGRAM="${ninja_bin}" \
        -DPython3_EXECUTABLE="${python3_bin}" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_INSTALL_PREFIX="${install_dir}" \
        -DLLVM_ENABLE_PROJECTS="clang;lld" \
        -DLLVM_TARGETS_TO_BUILD="AArch64;ARM;X86" \
        -DLLVM_ENABLE_ASSERTIONS=OFF || {
        error "Failed to configure llvm-clang."
        return 1
    }

    info "Building llvm-clang (this may take a while on first run)..."
    "${cmake_bin}" --build "${build_dir}" -j"$(nproc)" || {
        error "Failed to build llvm-clang."
        return 1
    }

    info "Installing llvm-clang to ${install_dir}..."
    "${cmake_bin}" --install "${build_dir}" || {
        error "Failed to install llvm-clang."
        return 1
    }
    success "llvm-clang installed to ${install_dir}."
}

# ──────────────────────────────────────────────────────────────
# Run Patch Setup
# ──────────────────────────────────────────────────────────────
DEVICE_PATH="${ROOT_DIR}/device/xiaomi/pipa"
mkdir -p "$DEVICE_PATH/patches"

setup_llvm_clang
apply_tablet_patch
apply_repo_patches "hardware/google/pixel"
apply_repo_patches "packages/apps/AxionParts"
apply_repo_patches "axion_sdk"

echo "-------------------------------------"
echo "           Setup complete!           "
echo "-------------------------------------"
