package com.swifteats.paymentmock;

import com.swifteats.paymentmock.dto.PaymentRequest;
import com.swifteats.paymentmock.service.PaymentMockService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentMockServiceTest {

    @Test
    public void testSuccessPath() {
        PaymentMockService svc = new PaymentMockService(0.0, 1);
        PaymentRequest r = new PaymentRequest();
        r.setOrderId(123L);
        r.setAmountCents(1000L);
        r.setCurrency("USD");
        r.setPaymentMethod("card");

        var resp = svc.process(r);
        assertThat(resp).isNotNull();
        assertThat(resp.getOrderId()).isEqualTo(123L);
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        assertThat(resp.getTransactionId()).isNotNull();
    }
}
