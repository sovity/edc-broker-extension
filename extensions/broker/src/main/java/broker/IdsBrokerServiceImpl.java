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

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.protocol.ids.service.ConnectorServiceSettings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.spi.event.contractdefinition.ContractDefinitionDeleted;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import sender.message.QueryMessage;
import sender.message.RegisterConnectorMessage;
import sender.message.RegisterResourceMessage;
import sender.message.UnregisterResourceMessage;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class IdsBrokerServiceImpl implements IdsBrokerService, EventSubscriber {

    private static final String CONTEXT_BROKER_REGISTRATION = "BrokerRegistration";

    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final ConnectorServiceSettings connectorServiceSettings;
    private final Hostname hostname;

    private final ContractDefinitionStore contractDefinitionStore;

    private final URL brokerBaseUrl;

    private final AssetIndex assetIndex;

    private final Monitor monitor;

    public IdsBrokerServiceImpl(
            RemoteMessageDispatcherRegistry dispatcherRegistry,
            ConnectorServiceSettings connectorServiceSettings,
            Hostname hostname,
            ContractDefinitionStore contractDefinitionStore,
            URL brokerBaseUrl,
            AssetIndex assetIndex,
            Monitor monitor) {
        this.dispatcherRegistry = dispatcherRegistry;
        this.connectorServiceSettings = connectorServiceSettings;
        this.hostname = hostname;
        this.contractDefinitionStore = contractDefinitionStore;
        this.brokerBaseUrl = brokerBaseUrl;
        this.assetIndex = assetIndex;
        this.monitor = monitor;
    }

    @Override
    public void registerConnectorAtBroker(URL brokerBaseUrl) {
        try {
            var brokerInfrastructureUrl = new URL(String.format("%s/infrastructure",
                    brokerBaseUrl));
            var connectorBaseUrl = new URI(String.format("http://%s/", hostname.get()));
            var registerConnectorMessage = new RegisterConnectorMessage(brokerInfrastructureUrl,
                    connectorBaseUrl,
                    connectorServiceSettings.getCurator(),
                    connectorServiceSettings.getMaintainer());
            dispatcherRegistry.send(Object.class, registerConnectorMessage,
                    () -> CONTEXT_BROKER_REGISTRATION);
        } catch (MalformedURLException e) {
            throw new EdcException("Could not build brokerInfrastructureUrl", e);
        } catch (URISyntaxException e) {
            throw new EdcException("Could not create connectorBaseUrl. Hostname can be set using:" +
                    " edc.hostname", e);
        }
    }

    @Override
    public void unregisterConnectorAtBroker(URL brokerBaseUrl) {

    }

    @Override
    public void registerResourceAtBroker(URL brokerBaseUrl, String resourceId, Asset asset) {
        try {
            var resourceUriId = new URI(String.format("https://%1$s/catalog/%2$s", hostname.get(), resourceId));
            var brokerInfrastructureUrl = new URL(String.format("%s/infrastructure",
                    brokerBaseUrl));
            var connectorBaseUrl = new URI(String.format("http://%s/", hostname.get()));

            var registerResourceMessage = new RegisterResourceMessage(
                    brokerInfrastructureUrl,
                    connectorBaseUrl,
                    resourceUriId,
                    asset);
            dispatcherRegistry.send(Object.class, registerResourceMessage,
                    () -> CONTEXT_BROKER_REGISTRATION);
        } catch (MalformedURLException e) {
            throw new EdcException("Could not build brokerInfrastructureUrl", e);
        } catch (URISyntaxException e) {
            throw new EdcException("Could not create connectorBaseUrl. Hostname can be set using:" +
                    " edc.hostname", e);
        }
    }

    @Override
    public void unregisterResourceAtBroker(URL brokerBaseUrl, String resourceId) {
        try {
            var resourceUriId = new URI(resourceId);
            var brokerInfrastructureUrl = new URL(String.format("%s/infrastructure",
                    brokerBaseUrl));
            var connectorBaseUrl = new URI(String.format("http://%s/", hostname.get()));
            var unregisterResourceMessage = new UnregisterResourceMessage(
                    brokerInfrastructureUrl,
                    connectorBaseUrl,
                    resourceUriId);
            dispatcherRegistry.send(Object.class, unregisterResourceMessage,
                    () -> CONTEXT_BROKER_REGISTRATION);
        } catch (MalformedURLException e) {
            throw new EdcException("Could not build brokerInfrastructureUrl", e);
        } catch (URISyntaxException e) {
            throw new EdcException("Could not create connectorBaseUrl. Hostname can be set using:" +
                    " edc.hostname", e);
        }
    }

    @Override
    public List<String> findResourceIdsByQuery(URL brokerBaseUrl, String fullTextQueryString) {
        try {
            var brokerInfrastructureUrl = new URL(String.format("%s/infrastructure",
                    brokerBaseUrl));
            var connectorBaseUrl = new URI(String.format("http://%s/", hostname.get()));
            var queryMessage = new QueryMessage(
                    brokerInfrastructureUrl,
                    connectorBaseUrl,
                    fullTextQueryString);
            var resultMessageCompletableFuture = dispatcherRegistry.send(
                    String.class,
                    queryMessage,
                    () -> CONTEXT_BROKER_REGISTRATION);
            return parseQueryResponse(resultMessageCompletableFuture.get());
        } catch (MalformedURLException e) {
            throw new EdcException("Could not build brokerInfrastructureUrl", e);
        } catch (URISyntaxException e) {
            throw new EdcException("Could not create connectorBaseUrl. Hostname can be set using:" +
                    " edc.hostname", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new EdcException("Failed to receive result message", e);
        }
    }

    public List<String> parseQueryResponse(String content) {
        var result = new ArrayList<String>();
        var scanner = new Scanner(content);
        var firstLine = true;
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            if (firstLine) {
                firstLine = false;
            } else {
                line = line.replace("<", "");
                line = line.replace(">", "");
                result.add(line);
            }
        }
        scanner.close();

        return result;
    }

    @Override
    public void on(Event<?> event) {
        if (event instanceof ContractDefinitionCreated contractDefinitionCreated) {
            //ContractDefinitionCreated -> Send EDC-Asset as IDS-Resource to IDS-Broker
            handleContractDefinitionCreated(contractDefinitionCreated);
        } else if (event instanceof ContractDefinitionDeleted contractDefinitionDeleted) {
            //ContractDefinitionDeleted -> Remove EDC-Asset as IDS-Resource from IDS-Broker
            handleContractDefinitionDeleted(contractDefinitionDeleted);
        }
    }

    private void handleContractDefinitionDeleted(ContractDefinitionDeleted contractDefinitionDeleted) {
        var eventPayload = contractDefinitionDeleted.getPayload();
        var contractDefinitionId = eventPayload.getContractDefinitionId();
        var deletedBrokerIds = getDeletedBrokerIds(brokerBaseUrl, contractDefinitionId);

        removeResourcesFromBroker(deletedBrokerIds, brokerBaseUrl);
    }

    private void handleContractDefinitionCreated(ContractDefinitionCreated contractDefinitionCreated) {
        var eventPayload = contractDefinitionCreated.getPayload();
        var contractDefinitionId = eventPayload.getContractDefinitionId();
        var contractDefinition = contractDefinitionStore.findById(contractDefinitionId);

        var resourceMap = new HashMap<String, Asset>();
        var assetsFromContractDefinition = getAssetsFromContractDefinition(contractDefinition);
        for (var asset : assetsFromContractDefinition) {
            var resourceId = String.format("%s-%s",
                    contractDefinition.getId(),
                    asset.getId());
            resourceMap.put(resourceId, asset);
        }

        addResourcesToBroker(resourceMap, brokerBaseUrl);
    }

    private List<Asset> getAssetsFromContractDefinition(ContractDefinition contractDefinition) {
        var querySpec = QuerySpec.Builder.newInstance()
                .filter(new ArrayList<>(contractDefinition.getSelectorExpression().getCriteria()))
                .build();
        return assetIndex.queryAssets(querySpec).toList();
    }

    private List<String> getDeletedBrokerIds(URL brokerBaseUrl, String deletedContractDefinitionIds) {
        var deletedResourceIds = new ArrayList<String>();
        var resourceIdsByQuery = findResourceIdsByQuery(brokerBaseUrl, deletedContractDefinitionIds);
        deletedResourceIds.addAll(resourceIdsByQuery);

        return deletedResourceIds;
    }

    private void removeResourcesFromBroker(
            List<String> deletedBrokerResourceIds,
            URL brokerBaseUrl) {
        for (var deletedResourceId : deletedBrokerResourceIds) {
            monitor.info(String.format("Removing resource %s from broker", deletedResourceId));
            unregisterResourceAtBroker(
                    brokerBaseUrl,
                    deletedResourceId);
        }
    }

    private void addResourcesToBroker(
            Map<String, Asset> resourceMap,
            URL brokerBaseUrl) {
        var createdResourceSet = new HashSet<>(resourceMap.keySet());

        for (var createdResourceId : createdResourceSet) {
            monitor.info(String.format("Registering resource %s at broker", createdResourceId));
            registerResourceAtBroker(
                    brokerBaseUrl,
                    createdResourceId,
                    resourceMap.get(createdResourceId));
        }
    }
}
