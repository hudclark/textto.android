/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by hudson on 3/23/18.
 */

public class SimpleCacheTests {

    @Test
    public void testAll () {
        SimpleCache<Integer, Integer> cache = new SimpleCache<>(10);

        for (int i = 1; i <= 10; i++) cache.put(i, i * 2);
        assertEquals(10, cache.getSize());
        for (int i = 1; i <= 10; i++) assertEquals(new Integer(i * 2), cache.get(i));

        cache.put(42, 58);

        assertEquals(7, cache.getSize());

        assertEquals((Integer) 58, cache.get(42));

        cache.clear();

        assertEquals(0, cache.getSize());

    }

}
