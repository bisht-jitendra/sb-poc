package com.az.servicebus.lib.utils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpUtils
{
    private static HttpUtils INSTANCE;
    private final HttpClient httpClient;

    private HttpUtils()
    {
        httpClient = HttpClient.newBuilder().build();
    }

    public static HttpUtils getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new HttpUtils();
        }
        return INSTANCE;
    }

    public String execute(HttpRequest request) throws IOException, InterruptedException
    {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }


}
