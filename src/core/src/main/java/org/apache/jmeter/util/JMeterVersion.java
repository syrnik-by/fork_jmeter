/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utility class to define the JMeter Version string.
 * Note: @...@-like macroses in this file should be replaced by a Gradle task.
 */
public final class JMeterVersion {
    private JMeterVersion() {
    }

    static String getVERSION() {
        final InputStream mfStream = JMeterVersion.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
        Manifest mf = new Manifest();
        try {
            mf.read(mfStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Attributes atts = mf.getMainAttributes();
        return Optional.ofNullable(atts.getValue(Attributes.Name.IMPLEMENTATION_VERSION)).orElse("Runtime Dev").concat(" Promsvyazbank PJSC Edition");
    }

    public static String getCopyRight() {
        return "Copyright (c) 2024-2026 Promsvyazbank PJSC ";
    }
}
