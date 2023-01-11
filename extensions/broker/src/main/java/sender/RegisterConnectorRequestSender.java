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
package sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.ConnectorEndpointBuilder;
import de.fraunhofer.iais.eis.ConnectorUpdateMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import sender.message.RegisterConnectorMessage;

import java.net.URI;
import java.util.List;

import static org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.edc.protocol.ids.jsonld.JsonLd.getObjectMapper;

public class RegisterConnectorRequestSender implements MultipartSenderDelegate<RegisterConnectorMessage,
        String> {

    private ObjectMapper objectMapper;
    private String connectorName;
    private String endpoint;

    public RegisterConnectorRequestSender(ObjectMapper objectMapper,
                                          String connectorName,
                                          String endpoint) {
        this.objectMapper = objectMapper;
        this.connectorName = connectorName;
        this.endpoint = endpoint;
    }

    @Override
    public Message buildMessageHeader(RegisterConnectorMessage registerConnectorMessage,
                                      DynamicAttributeToken token) throws Exception {
        return new ConnectorUpdateMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(registerConnectorMessage.connectorBaseUrl())
                ._senderAgent_(registerConnectorMessage.connectorBaseUrl())
                ._affectedConnector_(registerConnectorMessage.connectorBaseUrl())
                .build();
    }

    @Override
    public String buildMessagePayload(RegisterConnectorMessage registerConnectorMessage) throws Exception {
        var connectorEndpoint = new ConnectorEndpointBuilder(new URI("http://endpointid"))
                ._accessURL_(new URI(endpoint))
                .build();
        var resourceCatalog = new ResourceCatalogBuilder()
                .build();
        var baseConnector = new BaseConnectorBuilder(registerConnectorMessage.connectorBaseUrl())
                ._title_(new TypedLiteral(connectorName))
                ._curator_(registerConnectorMessage.curator())
                ._hasDefaultEndpoint_(connectorEndpoint)
                ._resourceCatalog_(resourceCatalog) // has to be set, otherwise broker will crash
                ._maintainer_(registerConnectorMessage.maintainer())
                ._outboundModelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
                .build();
        return objectMapper.writeValueAsString(baseConnector);
    }

    @Override
    public MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return parseMultipartStringResponse(parts, getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(MessageProcessedNotificationMessageImpl.class);
    }

    @Override
    public Class<RegisterConnectorMessage> getMessageType() {
        return RegisterConnectorMessage.class;
    }
}
