package com.az.servicebus.lib.config;

import com.az.servicebus.lib.enums.Mode;
import org.kohsuke.args4j.Option;

public class ServiceBusConfig
{

    @Option(name = "-n", aliases = "--namespace", required = true, usage = "Service Bus namespace")
    private String namespace;
    @Option(name = "-t", aliases = "--topic", required = true, usage = "Service Bus topic name")
    private String topic;
    @Option(name = "-a", aliases = "--ack-topic", required = true, usage = "Service Bus acknowledgment topic name")
    private String ackTopic;
    @Option(name = "-k", aliases = "--access-key", required = true, usage = "Service Bus Access key")
    private String accessKey;
    @Option(name = "-N", aliases = "--access-key-name", required = true, usage = "Service Bus Access key Name")
    private String accessKeyName;
    @Option(name = "-s", aliases = "--subscription", usage = "Subscription name")
    private String subscription;
    @Option(name = "-m", aliases = "--mode", usage = "Possible values PUBLISHER or CONSUMER. Default is PUBLISHER")
    private Mode mode = Mode.PUBLISHER;

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        this.accessKey = accessKey;
    }

    public String getAccessKeyName()
    {
        return accessKeyName;
    }

    public void setAccessKeyName(String accessKeyName)
    {
        this.accessKeyName = accessKeyName;
    }

    public String getSubscription()
    {
        return subscription;
    }

    public void setSubscription(String subscription)
    {
        this.subscription = subscription;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public String getAckTopic()
    {
        return ackTopic;
    }

    public void setAckTopic(String ackTopic)
    {
        this.ackTopic = ackTopic;
    }

    @Override
    public String toString()
    {
        return "ServiceBusConfig{" +
            "namespace='" + namespace + '\'' +
            ", topic='" + topic + '\'' +
            ", accessKey='" + accessKey + '\'' +
            ", accessKeyName='" + accessKeyName + '\'' +
            ", subscription='" + subscription + '\'' +
            ", mode=" + mode +
            '}';
    }
}
