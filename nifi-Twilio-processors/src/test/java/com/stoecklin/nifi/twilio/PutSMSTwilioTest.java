package com.stoecklin.nifi.twilio;

import java.lang.reflect.InvocationTargetException;

import javax.net.ssl.SSLContext;

import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;


public class PutSMSTwilioTest {

    @Test
    public void testProcessorProperties() {
        TestRunner runner = TestRunners.newTestRunner(MockPutSMSTwilio.class);
        runner.setProperty(PutSMSTwilio.ACCOUNT_ID, "Test");
        runner.setProperty(PutSMSTwilio.AUTH_TOKEN, "Test");
        runner.setProperty(PutSMSTwilio.FROM_NUMBER, "+41456345345");
        runner.setProperty(PutSMSTwilio.TO_NUMBER, "+41456345345");		
        runner.setProperty(PutSMSTwilio.DELIVERY_TYPE, "whatsapp");
        runner.assertValid();

        runner.setProperty(PutSMSTwilio.FROM_NUMBER, "Test");
        runner.setProperty(PutSMSTwilio.TO_NUMBER, "Test");		
        runner.setProperty(PutSMSTwilio.DELIVERY_TYPE, "bla");
        runner.assertNotValid();

        runner.setProperty(PutSMSTwilio.ACCOUNT_ID, "");
        runner.setProperty(PutSMSTwilio.AUTH_TOKEN, "");
        runner.setProperty(PutSMSTwilio.FROM_NUMBER, "1232134");
        runner.setProperty(PutSMSTwilio.TO_NUMBER, "1232134");		
        runner.assertNotValid();

        runner.setProperty(PutSMSTwilio.FROM_NUMBER, "+41456345345+1231231");
        runner.assertNotValid();
    }

    public static class MockPutSMSTwilio extends PutSMSTwilio {
        public MockPutSMSTwilio() {}

        @Override
        public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
            // nothing to do
        }
    }
}
