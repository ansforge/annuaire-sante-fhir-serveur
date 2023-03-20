/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.repository;

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.Date;

/**
 * Spring ahahahahRepository to access {@link SubscriptionMessage} with MongoDb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SubscriptionMessageRepository extends PagingAndSortingRepository<SubscriptionMessage, String> {

    Iterable<SubscriptionMessage> findAllByStatusInAndNextTryDateBefore(Collection<SubscriptionMessageStatus> status, Date date);

    Page<SubscriptionMessage> findAllByStatusInAndNextTryDateBefore(Collection<SubscriptionMessageStatus> status, Date date, Pageable pageable);
}
