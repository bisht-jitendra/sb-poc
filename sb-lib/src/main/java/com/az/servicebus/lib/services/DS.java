package com.az.servicebus.lib.services;

import com.az.servicebus.lib.config.ServiceBusConfig;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.ISubscriptionClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.rules.CorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DS extends AbstractServiceBusConfig
{
    private static final Logger LOG = LoggerFactory.getLogger(DS.class);

    public DS(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        super(config);
    }

    @Override
    protected void initializeClients() throws ServiceBusException, InterruptedException
    {
        setTopicClient(new TopicClient(buildTopicConnectionString(getConfig().getTopic())));
    }

    @Override
    public void perform() throws ServiceBusException, InterruptedException
    {
        Message message = new Message("Message from Domain Service");
        registerAckHandler(message);
        getTopicClient().send(message);
    }

    private void registerAckHandler(IMessage message) throws ServiceBusException, InterruptedException
    {
        getConfig().setSubscription(message.getMessageId());
        CorrelationFilter filter = new CorrelationFilter();
        filter.setCorrelationId(message.getMessageId());
        ISubscriptionClient subscriptionClient = buildSubscription(getConfig().getAckTopic(), ReceiveMode.PEEKLOCK, List.of(filter));
        subscriptionClient.registerMessageHandler(new AckMessageHandler(this, getConfig().getAckTopic()), getExecutorService());
    }

    static class AckMessageHandler implements IMessageHandler
    {
        private DS parent;
        private String topicName;

        private AckMessageHandler(DS parent, String topicName)
        {
            this.parent = parent;
            this.topicName = topicName;
        }

        @Override
        public CompletableFuture<Void> onMessageAsync(IMessage message)
        {
            System.out.println("================  ACK MESSAGE ========================");
            System.out.println("          MessageId: " + message.getMessageId());
            System.out.println("          MessageText: " + new String(message.getBody()));
            System.out.println("          Correlation: " + message.getCorrelationId());
            System.out.println("======================================================");
            try
            {
                Thread.sleep(10000);
                parent.deleteSubscription(topicName);
            } catch (Exception ex)
            {
                LOG.error(ex.getMessage(), ex);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void notifyException(Throwable exception, ExceptionPhase phase)
        {
            LOG.error(phase.name());
            LOG.error(exception.getMessage(), exception);
        }
    }
}
