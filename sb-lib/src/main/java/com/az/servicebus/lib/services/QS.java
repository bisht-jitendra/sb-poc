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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class QS extends AbstractServiceBusConfig
{

    private final static Logger LOG = LoggerFactory.getLogger(QS.class);

    protected QS(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        super(config);
        LOG.info("QS Initialization Completed.");
    }

    @Override
    public void initializeClients() throws ServiceBusException, InterruptedException
    {
        setTopicClient(new TopicClient(buildTopicConnectionString(getConfig().getAckTopic())));
        ISubscriptionClient subscriptionClient = buildSubscription(getConfig().getTopic(), ReceiveMode.PEEKLOCK, Collections.emptyList());
        setSubscriptionClient(subscriptionClient);
    }

    @Override
    public void perform()
    {
        try
        {
            getSubscriptionClient().registerMessageHandler(new MessageHandler(this), getExecutorService());
        } catch (Exception e)
        {
            LOG.error(e.getMessage(), e);
        }
    }

    static class MessageHandler implements IMessageHandler
    {

        private QS parent;

        private MessageHandler(QS parent)
        {
            this.parent = parent;
        }

        @Override
        public CompletableFuture<Void> onMessageAsync(IMessage message)
        {
            System.out.println("======================================================");
            System.out.println("          MessageId: " + message.getMessageId());
            System.out.println("          MessageText: " + new String(message.getBody()));
            System.out.println("======================================================");
            return CompletableFuture.completedFuture(acknowledgement(message));
        }

        @Override
        public void notifyException(Throwable exception, ExceptionPhase phase)
        {

        }

        private Void acknowledgement(IMessage message)
        {
            Message ackMessage = new Message("Ack message from QS " + message.getMessageId());
            ackMessage.setCorrelationId(ackMessage.getMessageId());
            try
            {
                parent.getTopicClient().send(ackMessage);
            } catch (Exception e)
            {
                LOG.error(e.getMessage(), e);
            }
            return null;
        }
    }
}
