/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service.impl;

/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.service.SignatureService;
import org.apache.commons.codec.digest.HmacUtils;


public class HMacSha256SignatureService implements SignatureService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final String key;


    public HMacSha256SignatureService(String key) {
        this.key = key;
    }

    @Override
    public void sign(SubscriptionMessage message) {
        var signature = new HmacUtils(HMAC_ALGO, key).hmacHex(message.getPayload());
        message.setSignature("sha256=" + signature);
    }


}
