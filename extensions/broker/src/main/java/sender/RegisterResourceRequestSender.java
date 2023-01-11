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
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Language;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessageImpl;
import de.fraunhofer.iais.eis.ResourceBuilder;
import de.fraunhofer.iais.eis.ResourceUpdateMessageBuilder;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import sender.message.RegisterResourceMessage;

import java.util.List;

import static org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.edc.protocol.ids.jsonld.JsonLd.getObjectMapper;

public class RegisterResourceRequestSender implements MultipartSenderDelegate<RegisterResourceMessage,
        String> {

    private ObjectMapper objectMapper;

    public RegisterResourceRequestSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message buildMessageHeader(RegisterResourceMessage registerResourceMessage,
                                      DynamicAttributeToken token) throws Exception {
        return new ResourceUpdateMessageBuilder(registerResourceMessage.affectedResourceUri())
                        ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                        ._issued_(CalendarUtil.gregorianNow())
                        ._securityToken_(token)
                        ._issuerConnector_(registerResourceMessage.connectorBaseUrl())
                        ._senderAgent_(registerResourceMessage.connectorBaseUrl())
                        ._affectedResource_(registerResourceMessage.affectedResourceUri())
                        .build();
    }

    @Override
    public String buildMessagePayload(RegisterResourceMessage registerResourceMessage) throws Exception {
        var assetTitle = getAssetTitle(registerResourceMessage);
        var assetDescription = getAssetDescription(registerResourceMessage);

        var resource = new ResourceBuilder(registerResourceMessage.affectedResourceUri())
                ._title_(new TypedLiteral(assetTitle, "en"))
                ._description_(new TypedLiteral(assetDescription, "en"))
                ._language_(Language.EN)
                .build();
        return objectMapper.writeValueAsString(resource);
    }

    private static String getAssetDescription(RegisterResourceMessage registerResourceMessage) {
        var assetDescription = "";
        if (registerResourceMessage.asset().getProperties().get("asset:prop:description") != null) {
            assetDescription = registerResourceMessage.asset().getProperties().get("asset:prop:description").toString();
        }
        return assetDescription;
    }

    private static String getAssetTitle(RegisterResourceMessage registerResourceMessage) {
        var assetTitle = "";
        if (registerResourceMessage.asset().getProperties().get("asset:prop:name") != null) {
            assetTitle = registerResourceMessage.asset().getProperties().get("asset:prop:name").toString();
        }
        return assetTitle;
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
    public Class<RegisterResourceMessage> getMessageType() {
        return RegisterResourceMessage.class;
    }
}
