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
package clearinghouse;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;

import java.net.URL;

public interface IdsClearingHouseService {

    void logContractAgreement(ContractAgreement contractAgreement,
                              URL clearingHouseLogUrl);

}
