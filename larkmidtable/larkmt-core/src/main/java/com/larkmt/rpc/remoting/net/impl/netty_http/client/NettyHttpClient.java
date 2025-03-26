package com.larkmt.rpc.remoting.net.impl.netty_http.client;

import com.larkmt.rpc.remoting.net.Client;
import com.larkmt.rpc.remoting.net.params.XxlRpcRequest;
import com.larkmt.rpc.remoting.net.common.ConnectClient;

/**
 * netty_http client
 *
 * @author xuxueli 2015-11-24 22:25:15
 */
public class NettyHttpClient extends Client {

    private Class<? extends ConnectClient> connectClientImpl = NettyHttpConnectClient.class;

    @Override
    public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
        ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
    }

}
