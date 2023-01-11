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
package broker;

import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.net.URL;
import java.util.List;

public interface IdsBrokerService {

    void registerConnectorAtBroker(URL brokerBaseUrl);

    void unregisterConnectorAtBroker(URL brokerBaseUrl);

    void registerResourceAtBroker(URL brokerBaseUrl, String resourceId, Asset asset);

    void unregisterResourceAtBroker(URL brokerBaseUrl, String resourceId);

    List<String> findResourceIdsByQuery(URL brokerBaseUrl, String fullTextQueryString);

}
