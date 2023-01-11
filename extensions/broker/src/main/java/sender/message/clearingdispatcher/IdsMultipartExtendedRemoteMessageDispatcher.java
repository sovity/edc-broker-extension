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
package sender.message.clearingdispatcher;

import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.IdsMultipartRemoteMessageDispatcher;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;

public class IdsMultipartExtendedRemoteMessageDispatcher extends IdsMultipartRemoteMessageDispatcher {

    public IdsMultipartExtendedRemoteMessageDispatcher(IdsMultipartSender idsMultipartSender) {
        super(idsMultipartSender);
    }

    @Override
    public String protocol() {
        return ExtendedMessageProtocol.IDS_EXTENDED_PROTOCOL;
    }
}
