package fr.ans.afas.hook;

import fr.ans.afas.domain.SubscriptionMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Subscription;

@Getter
@AllArgsConstructor
public class WebHookData {

    private Subscription subscription;

    private SubscriptionMessage message;
}
