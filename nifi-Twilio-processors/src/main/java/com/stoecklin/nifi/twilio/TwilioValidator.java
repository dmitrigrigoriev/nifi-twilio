package com.stoecklin.nifi.twilio;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

public class TwilioValidator {
    public static final Validator ATTRIBUTE_FROM_NUMBER_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            final ValidationResult.Builder builder = new ValidationResult.Builder();
            builder.subject(subject).input(input);
            if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
                return builder.valid(true).explanation("Contains Expression Language").build();
            }

            boolean bResult = input.matches(PutSMSTwilio.NUMBER_REGEXP);
            builder.valid(bResult == true).explanation(subject + " should be in E.164-Format. E.g. +41444....");

            return builder.build();
        }
    };

    public static final Validator ATTRIBUTE_DELIVERY_TYPE_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            final ValidationResult.Builder builder = new ValidationResult.Builder();
            builder.subject(subject).input(input);
            if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
                return builder.valid(true).explanation("Contains Expression Language").build();
            }

            boolean bValid = PutSMSTwilio.ALLOWED_DELIVERY_TYPES.contains(input);
            builder.valid(bValid == true).explanation(subject + " can have only following values: " + PutSMSTwilio.ALLOWED_DELIVERY_TYPES.toString());

            return builder.build();
        }
    };
}
