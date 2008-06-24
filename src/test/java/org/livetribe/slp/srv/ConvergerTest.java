/*
 * Copyright 2007-2008 the original author or authors
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
package org.livetribe.slp.srv;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.msg.Rply;
import org.livetribe.slp.srv.msg.SrvRqst;
import org.livetribe.slp.srv.msg.SrvRply;
import org.livetribe.slp.srv.msg.IdentifierExtension;
import org.livetribe.slp.srv.net.UDPConnector;
import org.testng.annotations.Test;

/**
 * @version $Revision$ $Date$
 */
public class ConvergerTest
{
    @Test
    public void testConvergenceNoReplies() throws Exception
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.isEmpty();
    }

    @Test
    public void testConvergenceOneReply() throws Exception
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        firstReplies.add(reply1);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 1;
        assert result.get(0).getResponder().equals(reply1.getResponder());
    }

    @Test
    public void testConvergenceTwoReplies() throws Exception
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        firstReplies.add(reply1);
        SrvRply reply2 = new SrvRply();
        reply2.setResponder("2");
        firstReplies.add(reply2);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 2;
        assert result.get(0).getResponder().equals(reply1.getResponder());
        assert result.get(1).getResponder().equals(reply2.getResponder());
    }

    @Test
    public void testConvergenceTimeoutOneReply() throws Exception
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        replies.add(Collections.<Rply>emptyList());
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        firstReplies.add(reply1);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 1;
        assert result.get(0).getResponder().equals(reply1.getResponder());
    }

    @Test
    public void testConvergenceOneReplyTimeoutOneReply() throws Exception
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        firstReplies.add(reply1);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        List<Rply> secondReplies = new ArrayList<Rply>();
        SrvRply reply2 = new SrvRply();
        reply2.setResponder("2");
        secondReplies.add(reply2);
        replies.add(secondReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 2;
        assert result.get(0).getResponder().equals(reply1.getResponder());
        assert result.get(1).getResponder().equals(reply2.getResponder());
    }

    @Test
    public void testConvergenceOneReplyOneReplySameResponder()
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        firstReplies.add(reply1);
        replies.add(firstReplies);
        List<Rply> secondReplies = new ArrayList<Rply>();
        SrvRply reply2 = new SrvRply();
        reply2.setResponder(reply1.getResponder());
        secondReplies.add(reply2);
        replies.add(secondReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 1; // Convergence must discard same responder
        assert result.get(0).getResponder().equals(reply1.getResponder());
    }

    @Test
    public void testConvergenceOneReplyId()
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        reply1.addExtension(new IdentifierExtension(reply1.getResponder(), "id1"));
        firstReplies.add(reply1);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 1;
        assert result.get(0).getResponder().equals(reply1.getResponder());
    }

    @Test
    public void testConvergenceTwoRepliesIdSameResponder()
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        reply1.addExtension(new IdentifierExtension(reply1.getResponder(), "id1"));
        firstReplies.add(reply1);
        SrvRply reply2 = new SrvRply();
        reply2.setResponder(reply1.getResponder());
        reply2.addExtension(new IdentifierExtension(reply1.getResponder(), "id2"));
        firstReplies.add(reply2);
        replies.add(firstReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 2;
        assert result.get(0).getResponder().equals(reply1.getResponder());
        assert result.get(1).getResponder().equals(reply1.getResponder());
    }

    @Test
    public void testConvergenceReplyIdReplyIdSameResponderSameId()
    {
        List<List<Rply>> replies = new ArrayList<List<Rply>>();
        List<Rply> firstReplies = new ArrayList<Rply>();
        SrvRply reply1 = new SrvRply();
        reply1.setResponder("1");
        IdentifierExtension idExt = new IdentifierExtension(reply1.getResponder(), "id1");
        reply1.addExtension(idExt);
        firstReplies.add(reply1);
        replies.add(firstReplies);
        List<Rply> secondReplies = new ArrayList<Rply>();
        SrvRply reply2 = new SrvRply();
        reply2.setResponder(reply1.getResponder());
        reply2.addExtension(idExt);
        secondReplies.add(reply2);
        replies.add(secondReplies);
        replies.add(Collections.<Rply>emptyList());
        replies.add(Collections.<Rply>emptyList());

        Converger<Rply> converger = new TestConverger(replies, null, null);
        List<Rply> result = converger.converge(new SrvRqst());
        assert result.size() == 1;
        assert result.get(0).getResponder().equals(reply1.getResponder());
    }

    private static class TestConverger extends Converger<Rply>
    {
        private final List<List<Rply>> replies;
        private int replyCount;

        private TestConverger(List<List<Rply>> replies, UDPConnector udpConnector, Settings settings)
        {
            super(udpConnector, settings);
            this.replies = replies;
        }

        @Override
        protected DatagramSocket convergenceBegin()
        {
            return null;
        }

        @Override
        protected List<Rply> manycastSendAndReceive(DatagramSocket datagramSocket, byte[] rqstBytes, int timeout)
        {
            return replies.get(replyCount++);
        }

        @Override
        protected void convergenceEnd(DatagramSocket datagramSocket)
        {
        }

        protected Rply convert(byte[] rplyBytes, InetSocketAddress address)
        {
            return null;
        }
    }
}
