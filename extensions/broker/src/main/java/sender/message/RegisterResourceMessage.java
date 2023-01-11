/*
 *  Copyright (c) 2022 sovity GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       sovity GmbH - initial API and implementation
 *
 */
package sender.message;

import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import sender.message.clearingdispatcher.ExtendedMessageProtocol;

import java.net.URI;
import java.net.URL;

public record RegisterResourceMessage(URL brokerUrl,
                                      // Broker seems only to accept connector ID's starting
                                      // with http or https
                                      URI connectorBaseUrl,
                                      URI affectedResourceUri,
                                      Asset asset) implements RemoteMessage {

    @Override
    public String getProtocol() {
        return ExtendedMessageProtocol.IDS_EXTENDED_PROTOCOL;
    }

    @Override
    public String getConnectorAddress() {
        return brokerUrl.toString();
    }
}
