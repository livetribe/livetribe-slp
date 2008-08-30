/*
 * Copyright 2005-2008 the original author or authors
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
import java.util.concurrent.TimeUnit;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.SLPError;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceType;
import org.testng.annotations.Test;

/**
 * @version $Rev$ $Date$
 */
public class MessageTest
{
    @Test
    public void testSrvRqstSerializeDeserialize() throws Exception
    {
        SrvRqst original = new SrvRqst();
        original.setServiceType(new ServiceType("a:b"));
        Scopes scopes = Scopes.from("scope1", "scope2");
        original.setScopes(scopes);
        Set<String> previousResponders = new HashSet<String>();
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

        assert deserialized.getServiceType() != null;
        assert original.getServiceType().equals(deserialized.getServiceType());
        assert deserialized.getScopes() != null;
        assert original.getScopes().equals(deserialized.getScopes());
        Set deserializedResponders = deserialized.getPreviousResponders();
        assert deserializedResponders != null;
        assert previousResponders.equals(deserializedResponders);
        assert deserialized.isMulticast();
        assert deserialized.isOverflow();
        assert deserialized.isFresh();
        assert deserialized.getFilter() != null;
        assert original.getFilter().equals(deserialized.getFilter());
        assert original.getLanguage().equals(deserialized.getLanguage());
        assert original.getXID() == deserialized.getXID();
        assert deserialized.getSecurityParameterIndex() != null;
        assert original.getSecurityParameterIndex().equals(deserialized.getSecurityParameterIndex());
    }

    @Test
    public void testSrvRplySerializedDeserialize() throws Exception
    {
        SrvRply original = new SrvRply();
        original.setSLPError(SLPError.BUSY_NOW);

        byte[] serialized = original.serialize();
        SrvRply deserialized = (SrvRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();

        original = new SrvRply();
        original.setSLPError(SLPError.NOT_IMPLEMENTED);
        URLEntry entry1 = new URLEntry();
        entry1.setURL("url1=");
        entry1.setLifetime(123);
        original.addURLEntry(entry1);
        URLEntry entry2 = new URLEntry();
        entry2.setURL("url2");
        entry2.setLifetime(321);
        original.addURLEntry(entry2);

        serialized = original.serialize();
        deserialized = (SrvRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();
        assert original.getURLEntries().equals(deserialized.getURLEntries());
    }

    @Test
    public void testDAAdvertSerializeDeserialize() throws Exception
    {
        DAAdvert original = new DAAdvert();
        original.setSLPError(SLPError.BUSY_NOW);

        byte[] serialized = original.serialize();
        DAAdvert deserialized = (DAAdvert)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();

        original = new DAAdvert();
        original.setSLPError(SLPError.INTERNAL_SYSTEM_ERROR);
        Long bootTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        original.setBootTime(bootTime.intValue());
        original.setURL("service:directory-agent://test");
        original.setScopes(Scopes.from("scope1", "scope2"));
        Attributes attributes = Attributes.from("(attr1=foo),attr2");
        original.setAttributes(attributes);
        original.setSecurityParamIndexes(new String[]{"spi1", "spi2"});
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        serialized = original.serialize();
        deserialized = (DAAdvert)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();
        assert original.getBootTime() == deserialized.getBootTime();
        assert original.getURL().equals(deserialized.getURL());
        assert original.getScopes().equals(deserialized.getScopes());
        assert original.getAttributes().equals(deserialized.getAttributes());
        assert Arrays.equals(original.getSecurityParameterIndexes(), deserialized.getSecurityParameterIndexes());
        // TODO: test auth blocks
//        assert Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks());
    }

    @Test
    public void testSrvAckSerializeDeserialize() throws Exception
    {
        SrvAck original = new SrvAck();
        original.setSLPError(SLPError.TYPE_ERROR);
        byte[] serialized = original.serialize();
        SrvAck deserialized = (SrvAck)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();
    }

    @Test
    public void testSrvRegSerializeDeserialize() throws Exception
    {
        SrvReg original = new SrvReg();
        URLEntry urlEntry = new URLEntry();
        urlEntry.setURL("url1");
        urlEntry.setLifetime(123);
        original.setURLEntry(urlEntry);
        original.setServiceType(new ServiceType("a:b"));
        original.setScopes(Scopes.from("scope1", "scope2"));
        Attributes attributes = Attributes.from("(attr1=foo),attr2");
        original.setAttributes(attributes);
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        SrvReg deserialized = (SrvReg)Message.deserialize(serialized);

        assert original.getURLEntry().equals(deserialized.getURLEntry());
        assert original.getServiceType().equals(deserialized.getServiceType());
        assert original.getScopes().equals(deserialized.getScopes());
        assert original.getAttributes().equals(deserialized.getAttributes());
        // TODO: test auth blocks
//        assert Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks());
    }

    @Test
    public void testSrvDeRegSerializeDeserialize() throws Exception
    {
        SrvDeReg original = new SrvDeReg();
        original.setScopes(Scopes.from("scope1", "scope2"));
        URLEntry urlEntry = new URLEntry();
        urlEntry.setURL("url1");
        urlEntry.setLifetime(123);
        original.setURLEntry(urlEntry);
        Attributes tags = Attributes.from("tag1,tag2");
        original.setTags(tags);

        byte[] serialized = original.serialize();
        SrvDeReg deserialized = (SrvDeReg)Message.deserialize(serialized);

        assert original.getScopes().equals(deserialized.getScopes());
        assert original.getURLEntry().equals(deserialized.getURLEntry());
        assert original.getTags().equals(deserialized.getTags());
    }

    @Test
    public void testSAAdvertSerializeDeserialize() throws Exception
    {
        SAAdvert original = new SAAdvert();
        original.setScopes(Scopes.from("scope1", "scope2"));
        Attributes attributes = Attributes.from("(attr1=foo),attr2");
        original.setAttributes(attributes);
        original.setURL("url1");
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        byte[] serialized = original.serialize();
        SAAdvert deserialized = (SAAdvert)Message.deserialize(serialized);

        assert original.getScopes().equals(deserialized.getScopes());
        assert original.getAttributes().equals(deserialized.getAttributes());
        assert original.getURL().equals(deserialized.getURL());
        // TODO: test auth blocks
//        assert Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks());
    }

    @Test
    public void testAttrRqstSerializeDeserialize()
    {
        AttrRqst original = new AttrRqst();
        original.setURL("service:jmx:rmi:///jndi/jmxrmi");
        original.setTags(Attributes.fromTags("tag1,foo*"));
        Scopes scopes = Scopes.from("scope1", "scope2");
        original.setScopes(scopes);
        Set<String> previousResponders = new HashSet<String>();
        previousResponders.add("1.2.3.4");
        previousResponders.add("4.3.2.1");
        original.setPreviousResponders(previousResponders);
        original.setMulticast(true);
        original.setOverflow(true);
        original.setFresh(true);
        original.setLanguage("en");
        original.setXID(5);
        original.setSecurityParameterIndex("spi1");

        byte[] serialized = original.serialize();
        AttrRqst deserialized = (AttrRqst)Message.deserialize(serialized);

        assert deserialized.getURL() != null;
        assert original.getURL().equals(deserialized.getURL());
        assert deserialized.getTags() != null;
        assert original.getTags().equals(deserialized.getTags());
        assert deserialized.getScopes() != null;
        assert original.getScopes().equals(deserialized.getScopes());
        Set deserializedResponders = deserialized.getPreviousResponders();
        assert deserializedResponders != null;
        assert previousResponders.equals(deserializedResponders);
        assert deserialized.isMulticast();
        assert deserialized.isOverflow();
        assert deserialized.isFresh();
        assert original.getLanguage().equals(deserialized.getLanguage());
        assert original.getXID() == deserialized.getXID();
        assert deserialized.getSecurityParameterIndex() != null;
        assert original.getSecurityParameterIndex().equals(deserialized.getSecurityParameterIndex());
    }

    @Test
    public void testAttrRplySerializeDeserialize()
    {
        AttrRply original = new AttrRply();
        original.setSLPError(SLPError.INTERNAL_ERROR);

        byte[] serialized = original.serialize();
        AttrRply deserialized = (AttrRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();

        original = new AttrRply();
        original.setSLPError(SLPError.TYPE_ERROR);
        original.setAttributes(Attributes.from("(a=1,2),foo,(b=1),(separator=\\2c),(d=string),(condition=true),(cofee\\5Fbytes=\\FF\\CA\\FE)"));
        // TODO: test auth blocks
//        original.setAuthenticationBlocks();

        serialized = original.serialize();
        deserialized = (AttrRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();
        assert original.getAttributes().equals(deserialized.getAttributes());
        // TODO: test auth blocks
//        assert Arrays.equals(original.getAuthenticationBlocks(), deserialized.getAuthenticationBlocks());
    }

    @Test
    public void testSrvTypeRqstSerializeDeserialize()
    {
        SrvTypeRqst original = new SrvTypeRqst();
        original.setNamingAuthority("foo");
        original.setScopes(Scopes.from("scope1", "scope2"));

        byte[] serialized = original.serialize();
        SrvTypeRqst deserialized = (SrvTypeRqst)Message.deserialize(serialized);

        assert deserialized.getNamingAuthority().equals(original.getNamingAuthority());
        assert deserialized.getScopes().equals(original.getScopes());

        original = new SrvTypeRqst();
        original.setNamingAuthority("*");
        original.setScopes(Scopes.from("scope1", "scope2"));

        serialized = original.serialize();
        deserialized = (SrvTypeRqst)Message.deserialize(serialized);

        assert deserialized.getNamingAuthority().equals(original.getNamingAuthority());
        assert deserialized.getScopes().equals(original.getScopes());
    }

    @Test
    public void testSrvTypeRplySerializeDeserialize()
    {
        SrvTypeRply original = new SrvTypeRply();
        original.setSLPError(SLPError.INTERNAL_ERROR);

        byte[] serialized = original.serialize();
        SrvTypeRply deserialized = (SrvTypeRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();

        original = new SrvTypeRply();
        original.setSLPError(SLPError.TYPE_ERROR);
        original.addServiceType(new ServiceType("service:foo:bar"));
        original.addServiceType(new ServiceType("service:http"));

        serialized = original.serialize();
        deserialized = (SrvTypeRply)Message.deserialize(serialized);

        assert original.getSLPError() == deserialized.getSLPError();
        assert original.getServiceTypes().equals(deserialized.getServiceTypes());
    }
}
