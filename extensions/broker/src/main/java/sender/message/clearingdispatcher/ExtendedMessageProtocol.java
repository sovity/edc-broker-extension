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

import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;

public final class ExtendedMessageProtocol {

    private static final String EXTENDED_SUFFIX = "-extended";
    public static final String IDS_EXTENDED_PROTOCOL = String.format("%s%s", MessageProtocol.IDS_MULTIPART, EXTENDED_SUFFIX);

    private ExtendedMessageProtocol() {
    }
}
