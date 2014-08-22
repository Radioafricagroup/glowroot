/*
 * Copyright 2014 the original author or authors.
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
package org.glowroot.plugin.logger;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.Containers;
import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.TraceMarker;
import org.glowroot.container.trace.Span;
import org.glowroot.container.trace.Trace;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests of the servlet plugin.
 * 
 * @author Trask Stalnaker
 * @since 0.5
 */
public class CommonsLoggingTest {

    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        container = Containers.getSharedContainer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void testLog() throws Exception {
        // given
        container.getConfigService().setPluginProperty("logger",
                "traceErrorOnErrorWithoutThrowable", true);
        // when
        container.executeAppUnderTest(ShouldLog.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        List<Span> spans = container.getTraceService().getSpans(trace.getId());
        assertThat(trace.getError()).isEqualTo("efg");
        assertThat(spans).hasSize(4);
        assertThat(spans.get(1).getMessage().getText()).isEqualTo("log warn: def");
        assertThat(spans.get(2).getMessage().getText()).isEqualTo("log error: efg");
        assertThat(spans.get(3).getMessage().getText()).isEqualTo("log fatal: fgh");
    }

    @Test
    public void testLogWithThrowable() throws Exception {
        // given
        container.getConfigService().setPluginProperty("logger",
                "traceErrorOnErrorWithoutThrowable", true);
        // when
        container.executeAppUnderTest(ShouldLogWithThrowable.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        List<Span> spans = container.getTraceService().getSpans(trace.getId());
        assertThat(trace.getError()).isEqualTo("efg_");
        assertThat(spans).hasSize(4);

        Span warnSpan = spans.get(1);
        assertThat(warnSpan.getMessage().getText()).isEqualTo("log warn: def_");
        assertThat(warnSpan.getError().getText()).isEqualTo("456");
        assertThat(warnSpan.getError().getException().getStackTrace().get(0))
                .contains("traceMarker");

        Span errorSpan = spans.get(2);
        assertThat(errorSpan.getMessage().getText()).isEqualTo("log error: efg_");
        assertThat(errorSpan.getError().getText())
                .isEqualTo("567");
        assertThat(errorSpan.getError().getException().getStackTrace().get(0))
                .contains("traceMarker");

        Span fatalSpan = spans.get(3);
        assertThat(fatalSpan.getMessage().getText()).isEqualTo("log fatal: fgh_");
        assertThat(fatalSpan.getError().getText())
                .isEqualTo("678");
        assertThat(fatalSpan.getError().getException().getStackTrace().get(0))
                .contains("traceMarker");
    }

    @Test
    public void testLogWithNullThrowable() throws Exception {
        // given
        container.getConfigService().setPluginProperty("logger",
                "traceErrorOnErrorWithoutThrowable", true);
        // when
        container.executeAppUnderTest(ShouldLogWithNullThrowable.class);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        List<Span> spans = container.getTraceService().getSpans(trace.getId());
        assertThat(trace.getError()).isEqualTo("efg_");
        assertThat(spans).hasSize(4);

        Span warnSpan = spans.get(1);
        assertThat(warnSpan.getMessage().getText()).isEqualTo("log warn: def_");
        assertThat(warnSpan.getError().getText()).isEqualTo("def_");
        Span errorSpan = spans.get(2);
        assertThat(errorSpan.getMessage().getText()).isEqualTo("log error: efg_");
        assertThat(errorSpan.getError().getText()).isEqualTo("efg_");
        Span fatalSpan = spans.get(3);
        assertThat(fatalSpan.getMessage().getText()).isEqualTo("log fatal: fgh_");
        assertThat(fatalSpan.getError().getText()).isEqualTo("fgh_");
    }

    public static class ShouldLog implements AppUnderTest, TraceMarker {
        private static final Log log = LogFactory.getLog(ShouldLog.class);
        @Override
        public void executeApp() {
            traceMarker();
        }
        @Override
        public void traceMarker() {
            log.trace("abc");
            log.debug("bcd");
            log.info("cde");
            log.warn("def");
            log.error("efg");
            log.fatal("fgh");
        }
    }

    public static class ShouldLogWithThrowable implements AppUnderTest, TraceMarker {
        private static final Log log = LogFactory.getLog(ShouldLogWithThrowable.class);
        @Override
        public void executeApp() {
            traceMarker();
        }
        @Override
        public void traceMarker() {
            log.trace("abc_", new IllegalStateException("123"));
            log.debug("bcd_", new IllegalStateException("234"));
            log.info("cde_", new IllegalStateException("345"));
            log.warn("def_", new IllegalStateException("456"));
            log.error("efg_", new IllegalStateException("567"));
            log.fatal("fgh_", new IllegalStateException("678"));
        }
    }

    public static class ShouldLogWithNullThrowable implements AppUnderTest, TraceMarker {
        private static final Log log = LogFactory.getLog(ShouldLogWithNullThrowable.class);
        @Override
        public void executeApp() {
            traceMarker();
        }
        @Override
        public void traceMarker() {
            log.trace("abc_", null);
            log.debug("bcd_", null);
            log.info("cde_", null);
            log.warn("def_", null);
            log.error("efg_", null);
            log.fatal("fgh_", null);
        }
    }
}