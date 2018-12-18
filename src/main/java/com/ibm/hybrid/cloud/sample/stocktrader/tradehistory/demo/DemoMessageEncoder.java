package com.ibm.hybrid.cloud.sample.stocktrader.tradehistory.demo;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;


public class DemoMessageEncoder implements Encoder.Text<DemoConsumedMessage> {

    @Override
    public void init(EndpointConfig config) {}

    @Override
    public void destroy() {}

    @Override
    public String encode(DemoConsumedMessage message) throws EncodeException {
        return message.encode();
    }

}