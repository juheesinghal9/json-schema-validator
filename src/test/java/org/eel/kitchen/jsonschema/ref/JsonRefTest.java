/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.ref;

import com.google.common.collect.Sets;
import org.eel.kitchen.jsonschema.main.JsonSchemaException;
import org.eel.kitchen.jsonschema.report.ValidationDomain;
import org.eel.kitchen.jsonschema.report.ValidationMessage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Set;

import static org.testng.Assert.*;

public final class JsonRefTest
{
    private static final JsonRef BASE_REF;

    static {
        try {
            BASE_REF = JsonRef.fromString("http://foo.bar/baz#");
        } catch (JsonSchemaException e) {
           throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    public void NonURIStringsShouldBeIdentifiedAsInvalid()
    {
        try {
            JsonRef.fromString("+23:");
            fail("No exception thrown!");
        } catch (JsonSchemaException e) {
            final ValidationMessage msg = e.getValidationMessage();
            assertSame(msg.getDomain(), ValidationDomain.REF_RESOLVING);
            assertEquals(msg.getKeyword(), "N/A");
            assertEquals(msg.getMessage(), "invalid URI");
            assertEquals(msg.getInfo("uri").textValue(), "+23:");
        }
    }

    @Test
    public void emptyOrNoFragmentIsTheSame()
        throws JsonSchemaException
    {
        final JsonRef ref1 = JsonRef.fromString("http://foo.bar");
        final JsonRef ref2 = JsonRef.fromString("http://foo.bar#");

        assertEquals(ref1, ref2);
    }

    @Test
    public void afterURINormalizationJsonRefsShouldBeEqual()
        throws JsonSchemaException
    {
        final String s1 = "http://foo.bar/a/b";
        final String s2 = "http://foo.bar/c/../a/./b";

        final JsonRef ref1 = JsonRef.fromString(s1);
        final JsonRef ref2 = JsonRef.fromString(s2);
        assertEquals(ref1, ref2);
    }

    @Test
    public void absoluteURIWithFragmentIsNotAnAbsoluteRef()
        throws JsonSchemaException
    {
        final JsonRef ref = JsonRef.fromString("http://foo.bar/a/b#c");

        assertFalse(ref.isAbsolute());
    }

    @DataProvider
    private Iterator<Object[]> getFragmentTestData()
    {
        final Set<Object[]> set = Sets.newHashSet();

        set.add(new Object[] { "file:///a", "" });
        set.add(new Object[] { "file:///a#", "" });
        set.add(new Object[] { "file:///a#b/c", "b/c" });

        return set.iterator();
    }

    @Test(dataProvider = "getFragmentTestData")
    public void testFragmentIsCorrectlyComputed(final String refAsString,
        final String fragmentAsString)
        throws JsonSchemaException
    {
        final JsonRef ref = JsonRef.fromString(refAsString);
        final JsonFragment fragment
            = JsonFragment.fromFragment(fragmentAsString);

        assertEquals(ref.getFragment(), fragment);
        assertEquals(fragment.isEmpty(), fragmentAsString.isEmpty());
    }

    @DataProvider
    public Iterator<Object[]> getContainsData()
    {
        final Set<Object[]> set = Sets.newHashSet();

        set.add(new Object[] { "http://foo.bar/blah#", false });
        set.add(new Object[] { "http://foo.bar/baz#pwet", true });
        set.add(new Object[] { "http://foo.bar/baz?a=b", false });
        set.add(new Object[] { "#/a/b/c", true });
        set.add(new Object[] { "a/b/v", false });
        set.add(new Object[] { "baz", true });

        return set.iterator();
    }

    @Test(dataProvider = "getContainsData")
    public void testReferenceContains(final String input,
        final boolean contained)
        throws JsonSchemaException
    {
        final JsonRef tmp = JsonRef.fromString(input);
        final JsonRef resolved = BASE_REF.resolve(tmp);

        assertEquals(BASE_REF.contains(resolved), contained);
    }
}
