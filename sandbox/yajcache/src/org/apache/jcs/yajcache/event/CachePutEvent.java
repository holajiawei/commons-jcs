/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jcs.yajcache.event;

import org.apache.jcs.yajcache.annotate.*;
import org.apache.jcs.yajcache.core.ICache;
/**
 *
 * @author Hanson Char
 */
@CopyRightApache
public class CachePutEvent<V> extends CacheChangeEvent {
    private final @NonNullable String key;
    private final @NonNullable V value;

    public CachePutEvent(@NonNullable ICache<V> cache, 
            @NonNullable String key, @NonNullable V value)
    {
        super(cache);
        this.key = key;
        this.value = value;
    }
    public String getKey() {
        return key;
    }
    public V getValue() {
        return value;
    }
}