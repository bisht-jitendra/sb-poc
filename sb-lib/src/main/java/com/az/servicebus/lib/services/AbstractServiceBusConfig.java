package com.az.servicebus.lib.services;

import com.az.servicebus.lib.config.ServiceBusConfig;
import com.az.servicebus.lib.utils.HttpUtils;
import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.ISubscriptionClient;
import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ClientConstants;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.microsoft.azure.servicebus.primitives.Util;
import com.microsoft.azure.servicebus.rules.Filter;
import com.microsoft.azure.servicebus.security.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractServiceBusConfig implements ServiceBusOperation
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceBusConfig.class);

    private final String subscriptionUrl = "https://%s.servicebus.windows.net/%s/Subscriptions/%s?api-version=2017-04";
    private ITopicClient topicClient;
    private ISubscriptionClient subscriptionClient;
    private ServiceBusConfig config;
    private HttpUtils httpUtils;
    private ExecutorService executorService;

    protected AbstractServiceBusConfig(ServiceBusConfig config) throws ServiceBusException, InterruptedException
    {
        this.config = config;
        this.httpUtils = HttpUtils.getInstance();
        initializeClients();
        executorService = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }

    protected abstract void initializeClients() throws ServiceBusException, InterruptedException;

    public ITopicClient getTopicClient()
    {
        return topicClient;
    }

    public void setTopicClient(ITopicClient topicClient)
    {
        this.topicClient = topicClient;
    }

    public ISubscriptionClient getSubscriptionClient()
    {
        return subscriptionClient;
    }

    public void setSubscriptionClient(ISubscriptionClient subscriptionClient)
    {
        this.subscriptionClient = subscriptionClient;
    }

    public ISubscriptionClient buildSubscription(String topic, ReceiveMode receiveMode, List<Filter> filters) throws ServiceBusException, InterruptedException
    {
        ISubscriptionClient subscriptionClient = new SubscriptionClient(buildSubscriptionConnectionString(topic), receiveMode);
        try
        {
            createSubscription(topic, subscriptionClient, filters);
        } catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
        return subscriptionClient;
    }

    protected ConnectionStringBuilder buildTopicConnectionString(String topic)
    {
        return connectionStringBuilder(config, topic);
    }

    protected ConnectionStringBuilder buildSubscriptionConnectionString(String topic)
    {
        String path = String.join("/", topic, "subscriptions", config.getSubscription());
        return connectionStringBuilder(config, path);
    }

    protected void createSubscription(String topic, ISubscriptionClient subscriptionClient, List<Filter> filters) throws ServiceBusException, InterruptedException, ExecutionException, IOException, URISyntaxException
    {
        if (subscriptionDoesNotExists(topic))
        {
            createSubscription(topic);
        }
        if (filters != null)
        {
            filters.forEach(filter -> {
                try
                {
                    subscriptionClient.addRule("Correlation", filter);
                } catch (Exception e)
                {
                    LOG.error(e.getMessage(), e);
                }
            });
        }
    }

    private void createSubscription(String topic) throws ExecutionException, InterruptedException, IOException, URISyntaxException
    {
        try(InputStream inputStream = this.getClass().getResourceAsStream("/subscription.xml"))
        {
            byte[] requestBody = inputStream.readAllBytes();
            HttpRequest putRequest = wrapRequest(HttpRequest.newBuilder(), topic)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

            httpUtils.execute(putRequest);
        }
    }

    private HttpRequest.Builder wrapRequest(HttpRequest.Builder request, String topic) throws ExecutionException, InterruptedException
    {
        String url = buildSubscriptionUrl(topic);

        ConnectionStringBuilder connectionStringBuilder = new ConnectionStringBuilder(config.getNamespace(),
            config.getNamespace() + ".servicebus.windows.net",
            config.getAccessKeyName(), config.getAccessKey());

        ClientSettings clientSettings = Util.getClientSettingsFromConnectionStringBuilder(connectionStringBuilder);

        SecurityToken token = clientSettings.getTokenProvider().getSecurityTokenAsync(url).get();

        String userAgent = String.format("%s/%s(%s)", ClientConstants.PRODUCT_NAME,
            ClientConstants.CURRENT_JAVACLIENT_VERSION, ClientConstants.PLATFORM_INFO);
        return request
            .headers("User-Agent", userAgent, "Authorization", token.getTokenValue(), "Content-Type", "application/atom+xml")
            .uri(URI.create(url));
    }

    protected void deleteSubscription(String topic) throws IOException, InterruptedException, ExecutionException
    {
//        HttpRequest deleteRequest = wrapRequest(HttpRequest.newBuilder().DELETE(), topic).build();
//        httpUtils.execute(deleteRequest);
    }

    private String buildSubscriptionUrl(String topic)
    {
        return String.format(subscriptionUrl, config.getNamespace(), topic, config.getSubscription());
    }

    private boolean subscriptionDoesNotExists(String topic) throws ServiceBusException, InterruptedException
    {
        try
        {
            new SubscriptionClient(buildSubscriptionConnectionString(topic), ReceiveMode.PEEKLOCK).getRules();
            return false;
        } catch (MessagingEntityNotFoundException ex)
        {
            return true;
        }
    }

    private ConnectionStringBuilder connectionStringBuilder(ServiceBusConfig config, String path)
    {
        return new ConnectionStringBuilder(config.getNamespace(), path, config.getAccessKeyName(), config.getAccessKey());
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public ServiceBusConfig getConfig()
    {
        return config;
    }

    @Override
    public void cleanup()
    {
        String subscriptionTopicName = null;
        try
        {
            if (getSubscriptionClient() != null)
            {
                subscriptionTopicName = getSubscriptionClient().getTopicName();
                getSubscriptionClient().close();
            }
        } catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
        try
        {
            if (getTopicClient() != null)
            {
                getTopicClient().close();
            }
        } catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }

        if (subscriptionTopicName != null)
        {

            try
            {
                deleteSubscription(subscriptionTopicName);
            } catch (Exception ex)
            {
                LOG.error(ex.getMessage(), ex);
            }
        }

        try
        {
            executorService.shutdownNow();
        } catch (Exception ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
    }

}
