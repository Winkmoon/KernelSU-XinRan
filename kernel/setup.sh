#!/bin/sh
set -eu

GKI_ROOT=$(pwd)

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag>]"
    echo "  --cleanup:              Cleans up previous modifications made by the script."
    echo "  <commit-or-tag>:        Sets up or updates the KernelSU-XinRan to specified tag or commit."
    echo "  -h, --help:             Displays this usage information."
    echo "  (no args):              Sets up or updates the KernelSU-XinRan environment to the latest tagged version."
}

initialize_variables() {
    if test -d "$GKI_ROOT/common/drivers"; then
         DRIVER_DIR="$GKI_ROOT/common/drivers"
    elif test -d "$GKI_ROOT/drivers"; then
         DRIVER_DIR="$GKI_ROOT/drivers"
    else
         echo '[ERROR] "drivers/" directory not found.'
         exit 127
    fi

    DRIVER_MAKEFILE=$DRIVER_DIR/Makefile
    DRIVER_KCONFIG=$DRIVER_DIR/Kconfig
}

# Reverts modifications made by this script
perform_cleanup() {
    echo "[+] Cleaning up..."
    [ -L "$DRIVER_DIR/KernelSU-XinRan" ] && rm "$DRIVER_DIR/KernelSU-XinRan" && echo "[-] Symlink removed."
    grep -q "KernelSU-XinRan" "$DRIVER_MAKEFILE" && sed -i '/KernelSU-XinRan/d' "$DRIVER_MAKEFILE" && echo "[-] Makefile reverted."
    grep -q "drivers/KernelSU-XinRan/Kconfig" "$DRIVER_KCONFIG" && sed -i '/drivers\/KernelSU-XinRan\/Kconfig/d' "$DRIVER_KCONFIG" && echo "[-] Kconfig reverted."
    if [ -d "$GKI_ROOT/KernelSU-XinRan" ]; then
        rm -rf "$GKI_ROOT/KernelSU-XinRan" && echo "[-] KernelSU-XinRan directory deleted."
    fi
}

# Sets up or update KernelSU-XinRan environment
setup_KernelSU-XinRan() {
    echo "[+] Setting up KernelSU-XinRan..."
    #苏安奈你个大笨蛋！！link不知道改一下！
    #然来帮帮腻～
    test -d "$GKI_ROOT/KernelSU-XinRan" || git clone https://github.com/Winkmoon/KernelSU-XinRan && echo "[+] Repository cloned."
    cd "$GKI_ROOT/KernelSU-XinRan"
    git stash && echo "[-] Stashed current changes."
    if [ "$(git status | grep -Po 'v\d+(\.\d+)*' | head -n1)" ]; then
        git checkout main && echo "[-] Switched to main branch."
    fi
    git pull && echo "[+] Repository updated."
    if [ -z "${1-}" ]; then
        git checkout "$(git describe --abbrev=0 --tags)" && echo "[-] Checked out latest tag."
    else
        git checkout "$1" && echo "[-] Checked out $1." || echo "[-] Checkout default branch"
    fi
    cd "$DRIVER_DIR"
    ln -sf "$(realpath --relative-to="$DRIVER_DIR" "$GKI_ROOT/KernelSU-XinRan/kernel")" "KernelSU-XinRan" && echo "[+] Symlink created."

    # Add entries in Makefile and Kconfig if not already existing
    grep -q "KernelSU-XinRan" "$DRIVER_MAKEFILE" || printf "\nobj-\$(CONFIG_KSU) += KernelSU-XinRan/\n" >> "$DRIVER_MAKEFILE" && echo "[+] Modified Makefile."
    grep -q "source \"drivers/KernelSU-XinRan/Kconfig\"" "$DRIVER_KCONFIG" || sed -i "/endmenu/i\source \"drivers/KernelSU-XinRan/Kconfig\"" "$DRIVER_KCONFIG" && echo "[+] Modified Kconfig."
    echo '[+] Done.'
}

# Process command-line arguments
if [ "$#" -eq 0 ]; then
    initialize_variables
    setup_KernelSU-XinRan
elif [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    display_usage
elif [ "$1" = "--cleanup" ]; then
    initialize_variables
    perform_cleanup
else
    initialize_variables
    setup_KernelSU-XinRan "$@"
fi
