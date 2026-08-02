package com.example.dcs.credential.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka")
public class KafkaCustomProperties {

    private String transactionTopic;
    private String userTopic;

    public String getTransactionTopic() {
        return transactionTopic;
    }

    public void setTransactionTopic(String transactionTopic) {
        this.transactionTopic = transactionTopic;
    }

    public String getUserTopic() {
        return userTopic;
    }

    public void setUserTopic(String userTopic) {
        this.userTopic = userTopic;
    }
}
