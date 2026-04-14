/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KernelExecutor {

    private static final ExecutorService sExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "pipa-kernel-writer"));

    private KernelExecutor() {}

    public static void submit(Runnable task) {
        sExecutor.execute(task);
    }
}
