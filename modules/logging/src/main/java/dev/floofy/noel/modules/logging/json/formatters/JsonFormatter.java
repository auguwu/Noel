/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel.modules.logging.json.formatters;

import java.util.Map;

/**
 * Represents an interface for formatting a Map object into a JSON string.
 */
public interface JsonFormatter {
    /**
     * Formats the given {@link Map} to return a JSON string.
     * @param data The data that is given by {@link org.noelware.charted.modules.logging.json.JsonLayout}
     * @return JSON string
     * @throws Exception If anything occurred while transforming.
     */
    String doFormat(Map<String, Object> data) throws Exception;
}
