/*
 * $Id: IdempotentSecureHashMessageProcessorTestCase.java 17050 2010-04-20 02:52:45Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import com.mockobjects.dynamic.Mock;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;


public class IdempotentSecureHashMessageProcessorTestCase extends AbstractMuleTestCase
{
    public IdempotentSecureHashMessageProcessorTestCase()
    {
        setStartContext(true);
    }

    public void testIdempotentReceiver() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?synchronous=false");
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());


        IdempotentSecureHashMessageProcessor ir = new IdempotentSecureHashMessageProcessor();


        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());

        // This one will process the event on the target endpoint
        event = ir.process(event);
        assertNotNull(event);

         // This will not process, because the message is a duplicate
        okMessage = new DefaultMuleMessage("OK", muleContext);
        event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());
        event = ir.process(event);
        assertNull(event);

        // This will process, because the message  is not a duplicate
        okMessage = new DefaultMuleMessage("Not OK", muleContext);
        event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());
        event = ir.process(event);
        assertNotNull(event);
    }

}