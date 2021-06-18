package com.stoecklin.nifi.twilio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.stream.io.StreamUtils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.exception.TwilioException;

@Tags({ "sms", "whatsapp", "twilio", "notification" })
@CapabilityDescription("Sends messages to the twilio service. Supports SMS and WhatsApp")
@SeeAlso({})
@EventDriven
@ReadsAttributes({
		@ReadsAttribute(attribute = "message.body", description = "Message body to send") })
public class PutSMSTwilio extends AbstractProcessor {

	protected static final List<String> ALLOWED_DELIVERY_TYPES = new ArrayList<>(
			Arrays.asList("sms", "whatsapp")
	);

	protected static final String NUMBER_REGEXP = "^\\+[0-9]*$";

	public PutSMSTwilio() {
	}

	public static final PropertyDescriptor ACCOUNT_ID = new PropertyDescriptor.Builder()
			.name("Account Id")
			.description("Twilio Account Id")
			.required(true)
			.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor AUTH_TOKEN = new PropertyDescriptor.Builder()
			.name("Auth token")
			.description("Twilio Auth token")
			.required(true)
			.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor FROM_NUMBER = new PropertyDescriptor.Builder()
			.name("From")
			.description("Twilio sending number")
			.required(true)
			.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.addValidator(TwilioValidator.ATTRIBUTE_FROM_NUMBER_VALIDATOR).build();
			
	public static final PropertyDescriptor TO_NUMBER = new PropertyDescriptor.Builder()
			.name("To")
			.description("Twilio recipient number(s)")
			.required(true)
			.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.addValidator(TwilioValidator.ATTRIBUTE_FROM_NUMBER_VALIDATOR).build();			

	public static final PropertyDescriptor DELIVERY_TYPE = new PropertyDescriptor.Builder()
			.name("DeliveryType")
			.description("Delivery type. Currently supported whatsapp and sms.")
			.required(true)
			.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
			.addValidator(TwilioValidator.ATTRIBUTE_DELIVERY_TYPE_VALIDATOR).build();

	// IMPORTANT
	// Add new property before this line.
	// After you've added a property,
	// put it also in the properties list below

	public static final Relationship REL_SUCCESS = new Relationship.Builder()
			.name("success").description("Message sent successfully").build();
	public static final Relationship REL_FAILURE = new Relationship.Builder()
			.name("failure").description("Message failed to send").build();

	public static final List<PropertyDescriptor> properties = Collections
			.unmodifiableList(Arrays.asList(ACCOUNT_ID, AUTH_TOKEN, FROM_NUMBER, TO_NUMBER, DELIVERY_TYPE));

	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return properties;
	}

	@Override
	public Set<Relationship> getRelationships() {
		Set<Relationship> relationships = new HashSet();
		relationships.add(REL_SUCCESS);
		relationships.add(REL_FAILURE);
		return relationships;
	}

	@Override
	public void onTrigger(final ProcessContext context,
			final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			return;
		}

		String messageText = "";

		final byte[] byteBuffer = new byte[(int) flowFile.getSize()];
		session.read(flowFile, new InputStreamCallback() {
			@Override
			public void process(InputStream in) throws IOException {
				StreamUtils.fillBuffer(in, byteBuffer, false);
			}
			});

		messageText = new String(byteBuffer, 0, byteBuffer.length, Charset.forName("UTF-8"));

		final String accountSid = context.getProperty(ACCOUNT_ID).evaluateAttributeExpressions(flowFile).getValue();
		final String authToken = context.getProperty(AUTH_TOKEN).evaluateAttributeExpressions(flowFile).getValue();
		final String from = context.getProperty(FROM_NUMBER).evaluateAttributeExpressions(flowFile).getValue();
		final String to = context.getProperty(TO_NUMBER).evaluateAttributeExpressions(flowFile).getValue();		
		final String deliveryType = context.getProperty(DELIVERY_TYPE).evaluateAttributeExpressions(flowFile).getValue();

		if (!ALLOWED_DELIVERY_TYPES.contains(deliveryType)) {
			getLogger().error(String.format("TwilioProcessor: %s is not an allowed value for Delivery Type", deliveryType));
			session.transfer(flowFile, REL_FAILURE);
			return;
		}

		if (messageText.isEmpty()) {
			getLogger().error("Attribute \"message.body\" cannot be empty");
			session.transfer(flowFile, REL_FAILURE);
			return;
		}

		Twilio.init(accountSid, authToken);

		String[] numbers = to.split(",");
		String notValidNumbers = "";
		int cntNotSend = 0;
		for (int i = 0; i < numbers.length; i++) {
		  try {

		  	String phoneNumberTo = numbers[i];
		  	if ( !phoneNumberTo.matches(NUMBER_REGEXP) ) {
				notValidNumbers += phoneNumberTo + ",";
				cntNotSend++;
		  		continue;
			}

		  	String phoneNumberFrom = from;
		  	if ("whatsapp".equals(deliveryType)) {
				phoneNumberTo = "whatsapp:" + phoneNumberTo;
				phoneNumberFrom = "whatsapp:"+from;
			}

			Message.creator(
				new PhoneNumber(phoneNumberTo),
				new PhoneNumber(phoneNumberFrom),
					messageText).create();

		  } catch (TwilioException e) {
		  	getLogger().error("TwilioProcessor: Message wasn't sent. TwilioException: " + e.toString());
			session.transfer(flowFile, REL_FAILURE);
			Twilio.destroy();
			return;
		  }
		}

		if ( cntNotSend == numbers.length ) {
			getLogger().error("TwilioProcessor: Message wasn't sent. All numbers are not valid.");
			session.transfer(flowFile, REL_FAILURE);
		}
		else {
			if ( cntNotSend == 0 ) {
				getLogger().info("TwilioProcessor: Message was sent successfully to all numbers.");
			}
			else {
				getLogger().warn("TwilioProcessor: Message was sent, but some numbers were not valid: " + notValidNumbers);
			}
			session.transfer(flowFile, REL_SUCCESS);
		}

		Twilio.destroy();
	}
}
