package com.az.servicebus.lib.services;

import com.az.servicebus.lib.config.ServiceBusConfig;
import com.az.servicebus.lib.enums.Mode;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.ISubscriptionClient;
import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceBusManager
{
    private final static Object MUTEX = new Object();
    private static ServiceBusManager INSTANCE;


    private ServiceBusOperation serviceBusOperation;

    private ITopicClient topicClient;
    private ISubscriptionClient subscriptionClient;

    private ServiceBusManager(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        initialize(config);
    }

    public static ServiceBusManager getInstance(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        if (INSTANCE == null)
        {
            synchronized (MUTEX)
            {
                if (INSTANCE == null)
                {
                    INSTANCE = new ServiceBusManager(config);
                }
            }
        }
        return INSTANCE;
    }

    private void initialize(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        if (config.getMode() == Mode.PUBLISHER)
        {
            serviceBusOperation = new DS(config);
        } else
        {
            serviceBusOperation = new QS(config);
        }
    }

    private void initializePublisher(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        System.out.println("=======================================================");
        System.out.println("Connecting to topic " + config.getTopic());
        System.out.println("=======================================================");
        ITopicClient topicClient = new TopicClient(connectionStringBuilder(config, config.getTopic()));
        String message = "";
        try
        {
            while (true)
            {
                System.out.println("Type your message or type \"exit\" to terminate the app: ");
                message = System.console().readLine();
                System.out.println(Arrays.toString(message.getBytes()));
                if (message.equalsIgnoreCase("exit"))
                {
                    break;
                }
                if (message != null && !message.equalsIgnoreCase(""))
                {
                    Message sbMessage = new Message(message);
                    topicClient.send(sbMessage);
                }
            }
        } catch (Exception ex)
        {
            System.err.println(ex.getMessage());
        } finally
        {
            System.out.println("=======================================================");
            System.out.println("Closing topic connection");
            System.out.println("=======================================================");
            topicClient.close();
            topicClient.closeAsync();
        }
    }

    private void initializeConsumer(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        String path = String.join("/", config.getTopic(), "subscriptions", config.getSubscription());
        System.out.println("=======================================================");
        System.out.println("Connecting to subscription " + path);
        System.out.println("=======================================================");
        ISubscriptionClient subscriptionClient = new SubscriptionClient(connectionStringBuilder(config, path), ReceiveMode.PEEKLOCK);
        ExecutorService executorService = Executors.newCachedThreadPool();
        subscriptionClient.registerMessageHandler(new IMessageHandler()
        {
            @Override
            public CompletableFuture<Void> onMessageAsync(IMessage message)
            {
                System.out.println("MessageId: " + message.getMessageId());
                System.out.println("Content-Type: " + message.getContentType());
                System.out.println("Message: " + new String(message.getBody()));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void notifyException(Throwable exception, ExceptionPhase phase)
            {
                System.err.println(exception.getMessage());
                System.err.println(phase.name());
            }
        }, executorService);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("=======================================================");
            System.out.println("Stopping executor service");
            executorService.shutdownNow();
            System.out.println("Closing subscription to " + path);
            System.out.println("=======================================================");
            try
            {
                subscriptionClient.close();
            } catch (ServiceBusException e)
            {
                e.printStackTrace();
            }
        }));
    }

    private ConnectionStringBuilder connectionStringBuilder(ServiceBusConfig config, String path)
    {

        return new ConnectionStringBuilder(config.getNamespace(), path, config.getAccessKeyName(), config.getAccessKey());
    }

    public void run()
    {
        try
        {
            serviceBusOperation.perform();
        } catch (Exception ex)
        {
            System.err.println(ex.getMessage());
        }
    }
}
