/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.util;

/**
 * Interface for objects that have a identifying key of the given type.
 */

public interface Keyed<K> {
    K getKey();
}
