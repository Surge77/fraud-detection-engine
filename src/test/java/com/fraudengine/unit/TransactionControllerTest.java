package com.fraudengine.unit;

import com.fraudengine.api.TransactionController;
import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.application.TransactionIngestionService;
import com.fraudengine.domain.exception.TransactionNotFoundException;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionIngestionService ingestionService;
    @MockBean
    private TransactionEvaluationService evaluationService;

    private static final String VALID_BODY = """
            {"transactionId":"tx-1","accountId":"acc_001","amount":100.00,"currency":"USD",
             "merchantId":"merch_999","merchantName":"Clean","location":"United States",
             "timestamp":"2026-05-30T12:00:00Z"}""";

    @Test
    void valid_transaction_is_accepted_with_202() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.transactionId").value("tx-1"));
    }

    @Test
    void negative_amount_is_rejected_with_422() throws Exception {
        String body = VALID_BODY.replace("100.00", "-5");
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void blank_merchant_is_rejected_with_422() throws Exception {
        String body = VALID_BODY.replace("\"merch_999\"", "\"\"");
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void existing_decision_is_returned_with_200() throws Exception {
        when(evaluationService.findDecision("tx-1")).thenReturn(
                new FraudDecision("tx-1", Decision.BLOCK, 90, List.of("BLACKLISTED_MERCHANT"), Instant.now()));

        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.riskScore").value(90));
    }

    @Test
    void unknown_transaction_returns_404_problem_detail() throws Exception {
        when(evaluationService.findDecision(eq("missing")))
                .thenThrow(new TransactionNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/transactions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Transaction not found"));
    }
}
