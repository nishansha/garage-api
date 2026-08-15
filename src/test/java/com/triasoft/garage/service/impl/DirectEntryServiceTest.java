package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.model.entry.DirectEntryRq;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies the is_direct_postable guard added to DirectEntryService.mapFields() - manual
 * journal entries must never be allowed against a control account (AR, AP, RC_DUE_RECEIVABLE,
 * etc.), because those lines carry no party/source tag and would silently desync the control
 * account balance from the sum of its subledger (see ReportService.getReceivablesSummary and
 * friends, all of which filter on party/source/system_role).
 */
@ExtendWith(MockitoExtension.class)
class DirectEntryServiceTest {

    @Mock private DirectEntryRepository directEntryRepository;
    @Mock private PaymentAccountRepository paymentAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ChartOfAccountRepository chartOfAccountRepository;
    @Mock private JournalService journalService;

    private DirectEntryService directEntryService;

    @BeforeEach
    void setUp() {
        directEntryService = new DirectEntryService(
                directEntryRepository, paymentAccountRepository, transactionRepository,
                chartOfAccountRepository, journalService);
    }

    private DirectEntryRq buildRq(Long coaId) {
        DirectEntryRq rq = new DirectEntryRq();
        rq.setCoaId(coaId);
        rq.setDirection(TransactionDirectionEnum.IN);
        rq.setAmount(new BigDecimal("1000"));
        rq.setPaymentAccountId(1L);
        return rq;
    }

    @Test
    void create_controlAccount_notDirectPostable_throws() {
        ChartOfAccount ar = new ChartOfAccount();
        ar.setId(1L);
        ar.setLabel("Accounts Receivable (A/R)");
        ar.setSystemRole("AR");
        ar.setDirectPostable(false);
        when(chartOfAccountRepository.findById(1L)).thenReturn(Optional.of(ar));

        assertThatThrownBy(() -> directEntryService.create(buildRq(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("DE_405");
    }

    @Test
    void create_directPostableAccount_succeeds() {
        ChartOfAccount capital = new ChartOfAccount();
        capital.setId(2L);
        capital.setLabel("Owner's Capital");
        capital.setDirectPostable(true);
        when(chartOfAccountRepository.findById(2L)).thenReturn(Optional.of(capital));

        PaymentAccount paymentAccount = new PaymentAccount();
        paymentAccount.setId(1L);
        paymentAccount.setName("Cash");
        when(paymentAccountRepository.findById(1L)).thenReturn(Optional.of(paymentAccount));

        lenient().when(directEntryRepository.save(any(DirectEntry.class))).thenAnswer(inv -> {
            DirectEntry e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });

        var result = directEntryService.create(buildRq(2L));

        assertThat(result.getId()).isEqualTo(99L);
    }
}
