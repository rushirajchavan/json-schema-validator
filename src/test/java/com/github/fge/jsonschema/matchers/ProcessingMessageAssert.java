/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonschema.matchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.util.JsonUtils;
import org.fest.assertions.GenericAssert;

import java.util.Collection;
import java.util.Map;

import static org.fest.assertions.Assertions.*;
import static org.testng.Assert.*;

public final class ProcessingMessageAssert
    extends GenericAssert<ProcessingMessageAssert, ProcessingMessage>
{
    private final JsonNode msg;

    public static ProcessingMessageAssert assertMessage(
        final ProcessingMessage message)
    {
        return new ProcessingMessageAssert(message);
    }

    private ProcessingMessageAssert(final ProcessingMessage actual)
    {
        super(ProcessingMessageAssert.class, actual);
        msg = actual.asJson();
    }

    /*
     * Simple asserts
     */
    public ProcessingMessageAssert hasJsonField(final String name,
        final JsonNode value)
    {
        assertThat(msg.has(name)).isTrue();
        // We have to use assertEquals, otherwise it takes the node as a
        // Collection
        assertTrue(msg.get(name).equals(value), "values differ: expected " +
            value + " but got " + msg.get(name));
        return this;
    }

    public ProcessingMessageAssert hasSerializedField(final String name,
        final Object value)
    {
        return hasJsonField(name, JsonUtils.toJson(value));
    }

    // FIXME: for some reason, I have to declare an Integer here, int won't work
    public ProcessingMessageAssert hasField(final String name,
        final Integer value)
    {
        return hasJsonField(name, JacksonUtils.nodeFactory().numberNode(value));
    }

    public ProcessingMessageAssert hasField(final String name,
        final Object value)
    {
        return hasJsonField(name, JsonUtils.toString(value));
    }

    public <T> ProcessingMessageAssert hasField(final String name,
        final Collection<T> value)
    {
        assertThat(msg.has(name)).isTrue();
        final JsonNode node = msg.get(name);
        assertThat(node.isArray()).isTrue();
        final ArrayNode input = JacksonUtils.nodeFactory().arrayNode();
        for (final T element: value)
            input.add(element.toString());
        assertEquals(node, input);
        return this;
    }

    public ProcessingMessageAssert hasTextField(final String name)
    {
        assertTrue(msg.path(name).isTextual());
        return this;
    }

    public ProcessingMessageAssert hasNullField(final String name)
    {
        assertThat(msg.has(name)).isTrue();
        assertEquals(msg.get(name), JacksonUtils.nodeFactory().nullNode());
        return this;
    }

    /*
     * Simple dedicated matchers
     */
    public ProcessingMessageAssert hasLevel(final LogLevel level)
    {
        assertThat(level).isEqualTo(actual.getLogLevel());
        return hasField("level", level);
    }

    public ProcessingMessageAssert hasMessage(final String expected)
    {
        final String message = msg.get("message").textValue();
        assertThat(message).isEqualTo(expected);
        return this;
    }

    /*
     * More complicated matchers
     */
    public <T> ProcessingMessageAssert isSyntaxError(final String keyword,
        final String msg, final SchemaTree tree)
    {
        // FIXME: .hasLevel() is not always set
        return hasField("keyword", keyword).hasMessage(msg)
            .hasSerializedField("schema", tree).hasField("domain", "syntax");
    }

    /*
     * More complicated matchers
     */
    public ProcessingMessageAssert isValidationError(final String keyword,
        final String msg)
    {
        return hasField("keyword", keyword).hasMessage(msg)
            .hasField("domain", "validation");
    }

    public ProcessingMessageAssert isFormatMessage(final String fmt,
        final String msg)
    {
        return hasField("keyword", "format").hasField("attribute", fmt)
            .hasMessage(msg).hasField("domain", "validation");
    }

    public ProcessingMessageAssert hasContents(final ObjectNode node)
    {
        /*
         * No need to check if the map is empty
         */
        if (node.size() == 0)
            return this;

        /*
         * Grab the two nodes as maps
         */
        final Map<String, JsonNode> expectedMap = JacksonUtils.asMap(msg);
        final Map<String, JsonNode> actualMap = JacksonUtils.asMap(node);

        /*
         * Check that this message's map contains all keys of the wanted data
         */
        assertTrue(expectedMap.keySet().containsAll(actualMap.keySet()));

        /*
         * OK? Let's check contents with Map.equals().
         */
        expectedMap.keySet().retainAll(actualMap.keySet());
        assertEquals(actualMap, expectedMap, "different map contents");
        return this;
    }
}
