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

package dev.floofy.noel.modules.logging.json;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import dev.floofy.noel.NoelInfo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.event.KeyValuePair;

public class ClassicJsonLayout extends JsonLayout<ILoggingEvent> {
    private final ThrowableHandlingConverter throwableProxyConverter = new ThrowableProxyConverter();

    public Map<String, Object> toJsonMap(ILoggingEvent event) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final DateFormat formatter = new SimpleDateFormat(getTimestampFormat());
        formatter.setTimeZone(TimeZone.getTimeZone(getTimezone()));

        data.put("@timestamp", formatter.format(new Date(event.getTimeStamp())));
        data.put("thread", event.getThreadName());
        data.put("message", event.getFormattedMessage());

        // Key-value pairs
        final List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs != null && !pairs.isEmpty()) {
            final HashMap<String, Object> argsMap = new HashMap<>();
            for (KeyValuePair pair : pairs) {
                argsMap.put(pair.key, pair.value);
            }

            data.put("args", argsMap);
        }

        final HashMap<String, Object> logArgs = new HashMap<>();
        logArgs.put("context", event.getLoggerContextVO().getName());
        logArgs.put("level", event.getLevel().toString().toLowerCase());
        logArgs.put("name", event.getLoggerName());

        // Log context
        data.put("log", logArgs);

        // metadata
        final HashMap<String, Object> metadataMap = new HashMap<>();
        final NoelInfo info = NoelInfo.getInstance();

        metadataMap.put("version", info.getVersion());
        metadataMap.put("commit_hash", info.getCommitHash());
        metadataMap.put("build_date", info.getBuildDate());

        data.put("metadata", metadataMap);

        // mdc properties
        final Map<String, String> mdc = event.getMDCPropertyMap();
        data.putAll(mdc);

        // exception data
        final IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            final String exception = throwableProxyConverter.convert(event);
            if (exception != null && !exception.isEmpty()) {
                data.put("exception", exception);
            }
        }

        return data;
    }

    @Override
    public void start() {
        throwableProxyConverter.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        throwableProxyConverter.stop();
    }
}
