package sender.message;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import sender.message.clearingdispatcher.ExtendedMessageProtocol;

import java.net.URI;
import java.net.URL;

public record LogMessage(URL clearingHouseLogUrl,
                         URI connectorBaseUrl,
                         ContractAgreement contractAgreement) implements RemoteMessage {
    @Override
    public String getProtocol() {
        return ExtendedMessageProtocol.IDS_EXTENDED_PROTOCOL;
    }

    @Override
    public String getConnectorAddress() {
        return clearingHouseLogUrl.toString();
    }
}
