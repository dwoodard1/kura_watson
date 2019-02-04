/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/

package org.eclipse.kura.cloudconnection.watson.mqtt;


public enum CloudPayloadJsonFields {
    SENTON("sentOn"),
    POSITION("position"),
    METRICS("metrics"),
    BODY("body");

    public enum CloudPayloadJsonPositionFields {
        LATITUDE("latitude"),
        LONGITUDE("longitude"),
        ALTITUDE("altitude"),
        HEADING("heading"),
        PRECISION("precision"),
        SATELLITES("satellites"),
        SPEED("speed"),
        TIMESTAMP("timestamp"),
        STATUS("status");

        private String value;

        private CloudPayloadJsonPositionFields(final String value) {
            this.value = value;
        }

        /**
         * Returns the string representation of the constant
         *
         * @return the string value
         */
        public String value() {
            return this.value;
        }
    }

    private String value;

    private CloudPayloadJsonFields(final String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the constant
     *
     * @return the string value
     */
    public String value() {
        return this.value;
    }
}
