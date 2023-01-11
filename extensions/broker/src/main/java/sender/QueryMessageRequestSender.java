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

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.QueryLanguage;
import de.fraunhofer.iais.eis.QueryMessageBuilder;
import de.fraunhofer.iais.eis.QueryScope;
import de.fraunhofer.iais.eis.QueryTarget;
import de.fraunhofer.iais.eis.ResultMessageImpl;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import sender.message.QueryMessage;

import java.util.List;

import static org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util.ResponseUtil.parseMultipartStringResponse;
import static org.eclipse.edc.protocol.ids.jsonld.JsonLd.getObjectMapper;

public class QueryMessageRequestSender implements MultipartSenderDelegate<QueryMessage, String> {

    private static final String SPARQL_QUERY = """
            PREFIX ids: <https://w3id.org/idsa/core/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT DISTINCT ?resultUri {
            GRAPH ?g {
                {
                  ?internalUri a ids:BaseConnector .
                  # binds are directly to cover "x a A, B ." cases.
                  BIND(ids:BaseConnector AS ?type)
                  BIND(?internalUri AS ?connector)
                }
                UNION
                {
                  ?internalUri a ids:TrustedConnector .
                  BIND(ids:TrustedConnector AS ?type)
                  BIND(?internalUri AS ?connector)
                }
                UNION
                {
                  ?internalUri a ids:Resource .
                  BIND(ids:Resource AS ?type)
                  # From Resource backwards to Connector
                  ?catalog ids:offeredResource ?internalUri .
                  ?connector ids:resourceCatalog ?catalog .
                }
                UNION
                {
                  ?internalUri a ids:Representation .
                  BIND(ids:Representation AS ?type)
                        
                 # From Representation backwards to Connector
                  ?resource ids:representation ?internalUri .
                  ?catalog ids:offeredResource ?resource .
                  ?connector ids:resourceCatalog ?catalog .
                }
                UNION
                {
                  ?internalUri a ids:Artifact .
                        
                 # From Artifact backwards to Connector
                  ?representation ids:instance ?internalUri .
                  ?resource ids:representation ?representation .
                  ?catalog ids:offeredResource ?resource .
                  ?connector ids:resourceCatalog ?catalog .
                }
                        
               ?internalUri ?predicate ?text .
                FILTER ( isLiteral(?text)) .
                        
               FILTER( REGEX(?text, "${fullTextQueryString}", "i") || REGEX(str(?uri), "${fullTextQueryString}", "i") )
                        
               # Get the Access Endpoint
                ?connector ids:hasDefaultEndpoint ?endpoint .
                ?endpoint ids:accessURL ?accessUrl .
                        
               {
                  # if a renaming happened, show the original URI.
                  ?internalUri owl:sameAs ?uri .
                } UNION {
                  # maybe this call is expensive.
                  FILTER(NOT EXISTS { ?internalUri owl:sameAs ?uri } )
                  BIND(?internalUri AS ?uri)
                }
                        
               BIND(?uri AS ?resultUri) # keep it non-breaking
                BIND(?text AS ?res) # keep it non-breaking
              }
            }
            """; // LIMIT 50 OFFSET 0
    private static final String SPARQL_QUERY_FULL_TEXT_TEMPLATE = "${fullTextQueryString}";

    @Override
    public Message buildMessageHeader(QueryMessage queryMessage,
                                      DynamicAttributeToken token) throws Exception {

        return new QueryMessageBuilder()
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._issued_(CalendarUtil.gregorianNow())
                ._securityToken_(token)
                ._issuerConnector_(queryMessage.connectorBaseUrl())
                ._senderAgent_(queryMessage.connectorBaseUrl())
                ._queryLanguage_(QueryLanguage.SPARQL)
                ._queryScope_(QueryScope.ALL)
                ._recipientScope_(QueryTarget.BROKER)
                .build();
    }

    @Override
    public String buildMessagePayload(QueryMessage queryMessage) throws Exception {
        return SPARQL_QUERY.replace(SPARQL_QUERY_FULL_TEXT_TEMPLATE, queryMessage.fullTextQueryString());
    }

    @Override
    public MultipartResponse<String> getResponseContent(IdsMultipartParts parts) throws Exception {
        return parseMultipartStringResponse(parts, getObjectMapper());
    }

    @Override
    public List<Class<? extends Message>> getAllowedResponseTypes() {
        return List.of(ResultMessageImpl.class);
    }

    @Override
    public Class<QueryMessage> getMessageType() {
        return QueryMessage.class;
    }
}
