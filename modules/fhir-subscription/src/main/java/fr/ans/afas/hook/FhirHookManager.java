/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.hook;

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FhirHookManager<T> {


    private final SubscriptionMessageRepository subscriptionMessageRepository;

    private final FhirHookPublisher hookPublisher;

    private final int pageSize;

    private final int maxWebhookTry;

    private final int waitBeforeRetry;

    private final int nbRequestMax;

    private final FhirStoreService<T> fhirStoreService;

    public FhirHookManager(SubscriptionMessageRepository subscriptionMessageRepository, FhirStoreService<T> fhirStoreService,
                           FhirHookPublisher publisher, int maxWebhookTry, int pageSize, int waitBeforeRetry, int nbRequestMax) {
        this.subscriptionMessageRepository = subscriptionMessageRepository;
        this.fhirStoreService = fhirStoreService;
        this.hookPublisher = publisher;
        this.maxWebhookTry = maxWebhookTry;
        this.pageSize = pageSize;
        this.waitBeforeRetry = waitBeforeRetry;
        this.nbRequestMax = nbRequestMax;
    }

    /**
     * Method that processes all webhook messages to send
     */
    @Scheduled(fixedDelayString = "${fhir.hook.process.delay:20000}")
    public void process() {
        int page = 0;
        Page<SubscriptionMessage> messagePage;
        int count = 0;

        do {
            messagePage = this.subscriptionMessageRepository
                    .findAllByStatusInAndNextTryDateBefore(
                            List.of(SubscriptionMessageStatus.PENDING, SubscriptionMessageStatus.PENDING_RETRY),
                            new Date(),
                            PageRequest.of(page, this.pageSize, Sort.by(Sort.Direction.DESC, "creationDate"))
                    );

            var requests = new HashMap<WebHookData, CompletableFuture<WebHookResponse>>();

            for (var message : messagePage.getContent()) {
                IIdType id = new IdType(message.getSubscriptionId());
                var subscription = (Subscription) this.fhirStoreService.findById("Subscription", id);
                requests.put(new WebHookData(subscription, message), hookPublisher.publish(message, subscription));
            }

            handleWebHookCallbacks(requests);

            this.subscriptionMessageRepository.saveAll(requests.keySet().stream().map(WebHookData::getMessage).collect(Collectors.toList()));
            this.fhirStoreService.store(requests.keySet().stream().map(WebHookData::getSubscription).collect(Collectors.toList()), true);
            page++;
            count += messagePage.getNumberOfElements();
            // handle only the first 1000 of messages
        } while (messagePage.hasNext() && count <= this.nbRequestMax);

        while (messagePage.hasNext()) {
            messagePage = setLeftMessagesInError(page);
            page++;
        }
    }

    /**
     * Retrieve page from DB & set all status in error
     *
     * @param page the page index to get
     * @return the handle message page
     */
    private Page<SubscriptionMessage> setLeftMessagesInError(int page) {
        Page<SubscriptionMessage> messagePage;
        messagePage = this.subscriptionMessageRepository
                .findAllByStatusInAndNextTryDateBefore(
                        List.of(SubscriptionMessageStatus.PENDING, SubscriptionMessageStatus.PENDING_RETRY),
                        new Date(),
                        PageRequest.of(page, this.pageSize, Sort.by(Sort.Direction.DESC, "creationDate"))
                );

        messagePage.getContent().forEach(message -> message.setStatus(SubscriptionMessageStatus.IN_ERROR));
        subscriptionMessageRepository.saveAll(messagePage.getContent());
        return messagePage;
    }

    /**
     * Handle WebHook calls callbacks & handle try data (status, retry, logs, etc...)
     *
     * @param requests sent requests to handle
     */
    private void handleWebHookCallbacks(HashMap<WebHookData, CompletableFuture<WebHookResponse>> requests) {
        requests.forEach((data, value) -> {
            var response = value.join();

            data.getMessage().setLastUpdated(response.getDate());

            if (response.isSuccess()) {
                handleRequestSuccess(data);
            } else {
                handleRequestError(data, response);
            }
        });
    }

    private void handleRequestError(WebHookData data, WebHookResponse response) {
        data.getMessage().setLastLogs(response.getLog());
        data.getMessage().incrementNbTry();

        if (data.getMessage().getLastLogs().equals(WebHookConstants.HEADER_MALFORMED_ERROR) || data.getMessage().getNbrTry() > this.maxWebhookTry) {
            data.getSubscription().setStatus(Subscription.SubscriptionStatus.ERROR);
            data.getSubscription().setError(data.getMessage().getLastLogs());
            data.getMessage().setStatus(SubscriptionMessageStatus.IN_ERROR);
        } else {
            data.getMessage().setStatus(SubscriptionMessageStatus.PENDING_RETRY);
            data.getMessage().setNextTryDate(new Date(getNextTryDate(this.waitBeforeRetry, data.getMessage().getNbrTry())));
        }
    }

    private void handleRequestSuccess(WebHookData data) {
        data.getMessage().setStatus(SubscriptionMessageStatus.SUCCESS);
        data.getMessage().setLastLogs("");
    }

    /**
     * Calculate next try date
     *
     * @param amountToWait the configure amount of time to wait
     * @param nbTry        the number of try that have already been done
     * @return the next try date in timestamp (ms)
     */
    private long getNextTryDate(long amountToWait, long nbTry) {
        var nextDate = new Date().getTime();

        for (int i = 0; i <= nbTry; i++) {
            nextDate += i * (amountToWait * 1000);
        }

        return nextDate;
    }
}
