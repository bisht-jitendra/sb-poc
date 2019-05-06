package com.az.servicebus.lib.services;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;

public interface ServiceBusOperation
{

    void perform() throws ServiceBusException, InterruptedException;

    void cleanup();
}
