/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.tests;

import java.io.File;
import java.util.List;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Test;

import org.glowroot.Containers;
import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.TempDirs;
import org.glowroot.container.config.StorageConfig;
import org.glowroot.container.local.LocalContainer;
import org.glowroot.container.trace.Span;
import org.glowroot.container.trace.Trace;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class UpgradeTest {

    @Test
    public void shouldReadTraces() throws Exception {
        // given
        File dataDir = TempDirs.createTempDir("glowroot-test-datadir");
        Resources.asByteSource(Resources.getResource("for-upgrade-test/config.json"))
                .copyTo(Files.asByteSink(new File(dataDir, "config.json")));
        Resources.asByteSource(Resources.getResource("for-upgrade-test/glowroot.h2.db"))
                .copyTo(Files.asByteSink(new File(dataDir, "glowroot.h2.db")));
        Resources.asByteSource(Resources.getResource("for-upgrade-test/glowroot.capped.db"))
                .copyTo(Files.asByteSink(new File(dataDir, "glowroot.capped.db")));
        Container container = Containers.create(dataDir, true);
        // when
        Trace trace = container.getTraceService().getLastTrace();
        List<Span> spans = container.getTraceService().getSpans(trace.getId());
        // then
        try {
            assertThat(trace.getHeadline()).isEqualTo("Level One");
            assertThat(trace.getTransactionName()).isEqualTo("basic test");
            assertThat(spans).hasSize(4);
            Span span1 = spans.get(0);
            assertThat(span1.getMessage().getText()).isEqualTo("Level One");
            Span span2 = spans.get(1);
            assertThat(span2.getMessage().getText()).isEqualTo("Level Two");
            Span span3 = spans.get(2);
            assertThat(span3.getMessage().getText()).isEqualTo("Level Three");
            Span span4 = spans.get(3);
            assertThat(span4.getMessage().getText()).isEqualTo("Level Four: axy, bxy");
        } finally {
            // cleanup
            container.checkAndReset();
            container.close();
            TempDirs.deleteRecursively(dataDir);
        }
    }

    // create initial database for upgrade test
    public static void main(String... args) throws Exception {
        File dataDir = TempDirs.createTempDir("glowroot-test-datadir");
        Container container = LocalContainer.createWithFileDb(dataDir);
        StorageConfig storageConfig = container.getConfigService().getStorageConfig();
        // disable trace snapshot expiration so the test data won't expire
        storageConfig.setTraceExpirationHours(Integer.MAX_VALUE);
        container.getConfigService().updateStorageConfig(storageConfig);
        container.executeAppUnderTest(ShouldGenerateTraceWithNestedSpans.class);
        container.close();
        Files.copy(new File(dataDir, "config.json"),
                new File("src/test/resources/for-upgrade-test/config.json"));
        Files.copy(new File(dataDir, "glowroot.h2.db"),
                new File("src/test/resources/for-upgrade-test/glowroot.h2.db"));
        Files.copy(new File(dataDir, "glowroot.capped.db"),
                new File("src/test/resources/for-upgrade-test/glowroot.capped.db"));
        TempDirs.deleteRecursively(dataDir);
    }

    // upgrade existing database for upgrade test
    public static void mainx(String... args) throws Exception {
        File dataDir = TempDirs.createTempDir("glowroot-test-datadir");
        Resources.asByteSource(Resources.getResource("for-upgrade-test/config.json"))
                .copyTo(Files.asByteSink(new File(dataDir, "config.json")));
        Resources.asByteSource(Resources.getResource("for-upgrade-test/glowroot.h2.db"))
                .copyTo(Files.asByteSink(new File(dataDir, "glowroot.h2.db")));
        Resources.asByteSource(Resources.getResource("for-upgrade-test/glowroot.capped.db"))
                .copyTo(Files.asByteSink(new File(dataDir, "glowroot.capped.db")));
        Container container = LocalContainer.createWithFileDb(dataDir);
        container.close();
        Files.copy(new File(dataDir, "config.json"),
                new File("src/test/resources/for-upgrade-test/config.json"));
        Files.copy(new File(dataDir, "glowroot.h2.db"),
                new File("src/test/resources/for-upgrade-test/glowroot.h2.db"));
        Files.copy(new File(dataDir, "glowroot.capped.db"),
                new File("src/test/resources/for-upgrade-test/glowroot.capped.db"));
        TempDirs.deleteRecursively(dataDir);
    }

    public static class ShouldGenerateTraceWithNestedSpans implements AppUnderTest {
        @Override
        public void executeApp() {
            new LevelOne().call("a", "b");
        }
    }
}