/*
 * ErrorListener
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jason Lai on 31 Jan 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.connectsdk.service.command;

import java.util.ArrayList;
import java.util.List;


public class NotSupportedServiceSubscription<T> implements ServiceSubscription<T> {
    private List<T> listeners = new ArrayList<T>();

    @Override
    public void unsubscribe() {
    }

    @Override
    public T addListener(T listener) {
        listeners.add(listener);

        return listener;
    }

    @Override
    public List<T> getListeners() {
        return listeners;
    }

    @Override
    public void removeListener(T listener) {
        listeners.remove(listener);
    }
}
