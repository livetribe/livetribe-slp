/*
 * Copyright 2005 the original author or authors
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
package org.livetribe.slp.spi.msg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.SLPSPITestCase;

/**
 * @version $Rev$ $Date$
 */
public class MessageTest extends SLPSPITestCase
{
    public void testSrvRqstSerializeDeserialize() throws Exception
    {
        SrvRqst original = new SrvRqst();
        original.setServiceType(new ServiceType("a:b"));
        String[] scopes = new String[]{"scope1", "scope2"};
        original.setScopes(scopes);
        Set previousResponders = new HashSet();
        previousResponders.add("1.2.3.4");
        previousResponders.add("4.3.2.1");
        original.setPreviousResponders(previousResponders);
        original.setMulticast(true);
        original.setOverflow(true);
        original.setFresh(true);
        original.setFilter("filter1");
        original.setLanguage("US");
        original.setXID(5);
        original.setSecurityParameterIndex("spi1");

        byte[] serialized = original.serialize();
        SrvRqst deserialized = (SrvRqst)Message.deserialize(serialized);

        assertNotNull(deserialized.getServiceType());
        assertEquals(original.getServiceType(), deserialized.getServiceType());
        assertNotNull(deserialized.getScopes());
        assertTrue(Arrays.equals(original.getScopes(), deserialized.getScopes()));
        Set deserializedResponders = deserialized.getPreviousResponders();
        assertNotNull(deserializedResponders);
        assertEquals(previousResponders, deserializedResponders);
        assertTrue(deserialized.isMulticast());
        assertTrue(deserialized.isOverflow());
        assertTrue(deserialized.isFresh());
        assertNotNull(deserialized.getFilter());
        assertEquals(original.getFilter(), deserialized.getFilter());
        assertEquals(original.getLanguage(), deserialized.getLanguage());
        assertEquals(original.getXID(), deserialized.getXID());
        assertNotNull(deserialized.getSecurityParameterIndex());
        assertEquals(original.getSecurityParameterIndex(), deserialized.getSecurityParameterIndex());
    }

    public void testSrvRplySerializedDeserialize() throws Exception
    {
        SrvRply original = new SrvRply();
        original.setErrorCode(1);

        byte[] serialized = original.serialize();
        SrvRply deserialized = (SrvRply)Message.deserialize(serialized);

        assertEquals(original.getErrorCode(), deserialized.getErrorCode());

        original = new SrvRply();
        original.setErrorCode(0);
        URLEntry entry1 = new URLEntry();
        entry1.setURL("url1");
        entry1.setLifetime(123);
        URLEntry entry2 = new URLEntry();
        entry2.setURL("url2");
        entry2.setLifetime(321);
        URLEntry[] urlEntries = new URLEntry[]{entry1, entry2};
        original.setURLEntries(urlEntries);

        serialized = original.serialize();
        deserialized = (SrvRply)Message.deserialize(serialized);

        assertEquals(original.getErrorCode(), deserialized.getErrorCode());
        assertTrue(Arrays.equals(original.getURLEntries(), deserialized.getURLEntries()));
    }

    public void testDAAdvertSerializeDeserialize() throws Exception
    {
        DAAdvert original = new DAAdvert();
        original.setErrorCode(1);
        original.setBootTime(System.currentTimeMillis());
        original.setURL("service:directory-agent://test");
        original.setScopes(new String[]{"scope1", "scope2"});
        Attributes attributes = new Attributes("(attr1=foo),attr2");
        original.setAttributes(attributes);
        original.setSecurityParamIndexes(new String[]{"spi1", "spi2"});
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        DAAdvert deserialized = (DAAdvert)Message.deserialize(serialized);

        assertEquals(original.getErrorCode(), deserialized.getErrorCode());
        assertEquals(original.getBootTime(), deserialized.getBootTime());
        assertEquals(original.getURL(), deserialized.getURL());
        assertTrue(Arrays.equals(original.getScopes(), deserialized.getScopes()));
        assertEquals(original.getAttributes(), deserialized.getAttributes());
        assertTrue(Arrays.equals(original.getSecurityParameterIndexes(), deserialized.getSecurityParameterIndexes()));
        // TODO: test auth blocks
//        assertTrue(Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks()));
    }

    public void testSrvAckSerializeDeserialize() throws Exception
    {
        SrvAck original = new SrvAck();
        original.setErrorCode(1);
        byte[] serialized = original.serialize();
        SrvAck deserialized = (SrvAck)Message.deserialize(serialized);

        assertEquals(original.getErrorCode(), deserialized.getErrorCode());
    }

    public void testSrvRegSerializeDeserialize() throws Exception
    {
        SrvReg original = new SrvReg();
        URLEntry urlEntry = new URLEntry();
        urlEntry.setURL("url1");
        urlEntry.setLifetime(123);
        original.setURLEntry(urlEntry);
        original.setServiceType(new ServiceType("a:b"));
        original.setScopes(new String[]{"scope1", "scope2"});
        Attributes attributes = new Attributes("(attr1=foo),attr2");
        original.setAttributes(attributes);
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        SrvReg deserialized = (SrvReg)Message.deserialize(serialized);

        assertEquals(original.getURLEntry(), deserialized.getURLEntry());
        assertEquals(original.getServiceType(), deserialized.getServiceType());
        assertTrue(Arrays.equals(original.getScopes(), deserialized.getScopes()));
        assertEquals(original.getAttributes(), deserialized.getAttributes());
        // TODO: test auth blocks
//        assertTrue(Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks()));
    }

    public void testSrvDeRegSerializeDeserialize() throws Exception
    {
        SrvDeReg original = new SrvDeReg();
        original.setScopes(new String[]{"scope1", "scope2"});
        URLEntry urlEntry = new URLEntry();
        urlEntry.setURL("url1");
        urlEntry.setLifetime(123);
        original.setURLEntry(urlEntry);
        Attributes tags = new Attributes("(tag1=foo),tag2");
        original.setTags(tags);

        byte[] serialized = original.serialize();
        SrvDeReg deserialized = (SrvDeReg)Message.deserialize(serialized);

        assertTrue(Arrays.equals(original.getScopes(), deserialized.getScopes()));
        assertEquals(original.getURLEntry(), deserialized.getURLEntry());
        assertEquals(original.getTags(), deserialized.getTags());
    }

    public void testSAAdvertSerializeDeserialize() throws Exception
    {
        SAAdvert original = new SAAdvert();
        original.setScopes(new String[]{"scope1", "scope2"});
        Attributes attributes = new Attributes("(attr1=foo),attr2");
        original.setAttributes(attributes);
        original.setURL("url1");
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        SAAdvert deserialized = (SAAdvert)Message.deserialize(serialized);

        assertTrue(Arrays.equals(original.getScopes(), deserialized.getScopes()));
        assertEquals(original.getAttributes(), deserialized.getAttributes());
        assertEquals(original.getURL(), deserialized.getURL());
        // TODO: test auth blocks
//        assertTrue(Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks()));
    }
}
