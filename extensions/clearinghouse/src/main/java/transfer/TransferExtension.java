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
package transfer;

import de.fraunhofer.iais.eis.Artifact;
import okhttp3.OkHttpClient;
import org.eclipse.edc.protocol.ids.api.configuration.IdsApiConfiguration;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.edc.protocol.ids.jsonld.JsonLd;
import org.eclipse.edc.protocol.ids.service.ConnectorServiceSettings;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import sender.message.dispatcher.IdsMultipartExtendedRemoteMessageDispatcher;
import transfer.clearinghouse.IdsClearingHouseService;
import transfer.clearinghouse.IdsClearingHouseServiceImpl;
import transfer.serializer.MultiContextJsonLdSerializer;
import transfer.transfer.ClearingHouseDefinitionProvider;
import transfer.transfer.SynchronizerImpl;
import transfer.transfer.TransferProcess;
import transfer.transfer.TransferProcessImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class TransferExtension implements ServiceExtension {

    public static final String CATALOG_TRANSFER_EXTENSION = "ClearingHouseExtension";
    private static final String TYPE_MANAGER_SERIALIZER_KEY = "ids-clearinghouse";

    private static final Map<String, String> CONTEXT_MAP = Map.of(
            "cat", "http://w3id.org/mds/data-categories#",
            "ids", "https://w3id.org/idsa/core/",
            "idsc", "https://w3id.org/idsa/code/");
    @Setting
    public static final String CLEARINGHOUSE_LOG_URL_SETTING = "edc.clearinghouse.log.url";

    @Setting
    private static final String EDC_CATALOG_URL = "edc.catalog.url";

    @Setting
    private static final String UPDATE_INTERVAL_IN_SECONDS = "update.interval.in.seconds";

    @Setting
    private static final String EDC_CONNECTOR_NAME = "edc.connector.name";

    @Setting
    private static final String EDC_IDS_ENDPOINT = "edc.ids.endpoint";

    @Inject
    private IdsApiConfiguration idsApiConfiguration;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private IdentityService identityService;

    @Inject
    private IdsTransformerRegistry transformerRegistry;

    @Inject
    private Hostname hostname;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private DynamicAttributeTokenService dynamicAttributeTokenService;

    @Inject
    private ClearingHouseDefinitionProvider provider;

    private IdsClearingHouseService idsClearingHouseService;
    private TransferProcess catalogTransferProcess;

    private URL clearingHouseLogUrl;
    private Monitor monitor;

    @Override
    public String name() {
        return CATALOG_TRANSFER_EXTENSION;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        clearingHouseLogUrl = readUrlFromSettings(context, CLEARINGHOUSE_LOG_URL_SETTING);
        monitor = context.getMonitor();

        var updateInterval = context.getSetting(UPDATE_INTERVAL_IN_SECONDS, "5");

        registerSerializerClearingHouseMessages(context);
        registerClearingHouseMessageSenders(context);

        initializeIdsClearingHouseService(
                context,
                dispatcherRegistry,
                hostname);

        initializeCatalogTransferProcess(
                idsClearingHouseService,
                monitor,
                updateInterval);
    }

    private void initializeCatalogTransferProcess(
            IdsClearingHouseService idsClearinghouseService,
            Monitor monitor,
            String updateInterval) {

        var synchronizer = new SynchronizerImpl(
                idsClearinghouseService,
                monitor,
                provider);
        catalogTransferProcess = new TransferProcessImpl(
                synchronizer,
                monitor,
                updateInterval);
    }

    private void initializeIdsClearingHouseService(
            ServiceExtensionContext context,
            RemoteMessageDispatcherRegistry dispatcherRegistry,
            Hostname hostname) {
        var connectorServiceSettings = new ConnectorServiceSettings(context, context.getMonitor());
        idsClearingHouseService = new IdsClearingHouseServiceImpl(
                dispatcherRegistry,
                connectorServiceSettings,
                hostname);
    }

    private URL readUrlFromSettings(ServiceExtensionContext context, String settingsPath) {
        try {
            var urlString = context.getSetting(settingsPath, null);
            if (urlString == null && !EDC_CATALOG_URL.equals(settingsPath)) {
                throw new EdcException(String.format("Could not initialize " +
                        "CatalogTransferExtension: " +
                        "No url specified using setting %s", settingsPath));
            } else if (urlString == null) {
                return null;
            }

            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new EdcException(String.format("Could not parse setting %s to Url",
                    settingsPath), e);
        }
    }

    private void registerSerializerClearingHouseMessages(ServiceExtensionContext context) {
        var typeManager = context.getTypeManager();
        typeManager.registerContext(TYPE_MANAGER_SERIALIZER_KEY, JsonLd.getObjectMapper());
        registerCommonTypes(typeManager);
    }

    private void registerCommonTypes(TypeManager typeManager) {
        typeManager.registerSerializer(TYPE_MANAGER_SERIALIZER_KEY, Artifact.class,
                new MultiContextJsonLdSerializer<>(Artifact.class, CONTEXT_MAP));
    }

    private void registerClearingHouseMessageSenders(ServiceExtensionContext context) {
        var httpClient = context.getService(OkHttpClient.class);
        var monitor = context.getMonitor();
        var typeManager = context.getTypeManager();
        var objectMapper = typeManager.getMapper(TYPE_MANAGER_SERIALIZER_KEY);
        var connectorName = context.getSetting(EDC_CONNECTOR_NAME, "EDC");
        var endpoint = context.getSetting(EDC_IDS_ENDPOINT, "http://endpoint");

//        var registerConnectorSender = new RegisterConnectorRequestSender(objectMapper, connectorName, endpoint);

        var idsMultipartSender = new IdsMultipartSender(monitor, httpClient, dynamicAttributeTokenService, objectMapper);
        var dispatcher = new IdsMultipartExtendedRemoteMessageDispatcher(idsMultipartSender);
//        dispatcher.register(registerConnectorSender);
        dispatcherRegistry.register(dispatcher);
    }

    @Override
    public void start() {
        try {
            catalogTransferProcess.startTransferProcess(clearingHouseLogUrl);
        } catch (Exception e) {
            monitor.severe(String.format("%s failed during startup: ", CATALOG_TRANSFER_EXTENSION), e);
        }
    }

    @Override
    public void prepare() {
        ServiceExtension.super.prepare();
    }
}
